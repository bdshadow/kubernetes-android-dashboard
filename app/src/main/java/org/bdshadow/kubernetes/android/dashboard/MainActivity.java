package org.bdshadow.kubernetes.android.dashboard;


import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;

import org.bdshadow.kubernetes.android.dashboard.exception.BrokenSecureStoreDataException;
import org.bdshadow.kubernetes.android.dashboard.exception.SecureStoreNotSupportedException;
import org.bdshadow.kubernetes.android.dashboard.utils.EncryptionUtils;
import org.bdshadow.kubernetes.android.dashboard.utils.IOUtils;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private NewKubernetesConnectionFragment newConnectionFragment;

    public enum ConnectionResult {
        SUCCESS, FAILED_AUTH, FAILED_NETWORK
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ConnectionsOverviewFragment connectionsOverviewFragment = new ConnectionsOverviewFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, connectionsOverviewFragment)
                .commit();
    }

    public void addKubernetesConnectionClick(View v) {
        if (newConnectionFragment == null) {
            this.newConnectionFragment = new NewKubernetesConnectionFragment();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, this.newConnectionFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getSupportFragmentManager().popBackStack();
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showTryAgainSnackBar(String message) {
        Snackbar connectionFailedSnackBar =
                Snackbar.make(findViewById(R.id.fragmentContainer), message, Snackbar.LENGTH_INDEFINITE);
        connectionFailedSnackBar.setAction("Try again", v -> onNewConnectionClick(v));
        connectionFailedSnackBar.show();
    }

    private void showOkAlert(String title, String message) {
        //TODO add possibility to clear the KeyStore and save once again from scratch
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", (dialog, which) -> {
        });
        builder.show();
    }

    public void onNewConnectionClick(View view) {
        EditText editTextServerUrl = this.newConnectionFragment.getView().findViewById(R.id.editTextServerUrl);
        final String url = editTextServerUrl.getText().toString();
        if (url.isEmpty()) {
            editTextServerUrl.setError("Please, enter url");
            return;
        }
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            editTextServerUrl.setError("is not a valid url");
            return;
        }
        AsyncTask<Config, Integer, ConnectionResult> tryConnectAsyncTask;
        Config config;
        RadioGroup radioGroupAuth = this.newConnectionFragment.getView().findViewById(R.id.radioGroupAuthType);
        if (radioGroupAuth.getCheckedRadioButtonId() == R.id.radioOAuth) {
            EditText editTextToken = this.newConnectionFragment.getView().findViewById(R.id.editTextToken);
            final String token = editTextToken.getText().toString();
            if (token.isEmpty()) {
                editTextToken.setError("Please, enter token");
                return;
            }
            config = new ConfigBuilder().withMasterUrl(url).withOauthToken(token).build();
            tryConnectAsyncTask = new TryConnectAsyncTask().execute(config);

        } else {
            EditText editTextUsername = this.newConnectionFragment.getView().findViewById(R.id.editTextUsername);
            String username = editTextUsername.getText().toString();
            EditText editTextPassword = this.newConnectionFragment.getView().findViewById(R.id.editTextPassword);
            String password = editTextPassword.getText().toString();
            if (username.isEmpty()) {
                editTextUsername.setError("Please, enter username");
                return;
            }
            if (password.isEmpty()) {
                editTextPassword.setError("Please, enter password");
                return;
            }
            config = new ConfigBuilder().withMasterUrl(url).withUsername(username).withPassword(password).build();
            tryConnectAsyncTask = new TryConnectAsyncTask().execute(config);
        }

        try {
            switch (tryConnectAsyncTask.get()) {
                case SUCCESS:
                    addConnection(config);
                    break;
                case FAILED_NETWORK:
                    showTryAgainSnackBar("Network connectivity problem occurred");
                    break;
                case FAILED_AUTH:
                    showTryAgainSnackBar("Authentication failed");
                    break;
            }
        } catch (ExecutionException | InterruptedException e) {
            //TODO do smth more sensible here
            Log.e(TAG, "Unexpected error", e);
            showTryAgainSnackBar("Unexpected error happened");
        } catch (BrokenSecureStoreDataException e) {
            //TODO add possibility to clear the KeyStore and save once again from scratch
            Log.e(TAG, "secure storage data is broken", e);
            showOkAlert("Secure store was spoiled", "Please, try to delete and install the app again");
        } catch (SecureStoreNotSupportedException e) {
            //TODO save connection without credentials
            Log.e(TAG, "secure storage is not supported", e);
            showOkAlert("Secure store is not supported", "The connection credentials can't be saved securely");
        }
    }

    /**
     * @return true, if a new connection added, false if it already existed
     */
    private void addConnection(Config config) throws SecureStoreNotSupportedException, BrokenSecureStoreDataException {
        if (exists(config)) {
            //TODO do you want to overwrite connection
            Log.i(TAG, "Connection already exists");
        } else {
            Log.i(TAG, "Adding new connection");
            JSONObject connectionsJson;
            try {
                if (new File(getFilesDir(), getString(R.string.connections_file_key)).exists()) {
                    connectionsJson = new JSONObject(IOUtils.readFile(openFileInput(getString(R.string.connections_file_key))));
                    Log.i(TAG, "Adding to already existing connections");
                } else {
                    connectionsJson = new JSONObject();
                    Log.i(TAG, "Persisting the first connection");
                }
            } catch (FileNotFoundException e) {
                connectionsJson = new JSONObject();
            } catch (JSONException e) {
                connectionsJson = new JSONObject();
            }
            try (PrintWriter printWriter = new PrintWriter(openFileOutput(getString(R.string.connections_file_key), Context.MODE_PRIVATE))) {
                JSONArray connectionsJsonArray;
                int connectionId = 0;
                if (connectionsJson.has("connections")) {
                    connectionsJsonArray = connectionsJson.getJSONArray("connections");
                } else {
                    connectionsJsonArray = new JSONArray();
                    connectionsJson.put("connections", connectionsJsonArray);
                    connectionId = connectionsJsonArray.length();
                }
                JSONObject connectionJson = new JSONObject();
                connectionJson.put("id", connectionId);
                connectionJson.put("name", config.getMasterUrl());
                connectionJson.put("url", config.getMasterUrl());
                connectionJson.put("username", config.getUsername());
                connectionJson.put("password",
                        config.getPassword() == null ? JSONObject.NULL : EncryptionUtils.encryptString(config.getPassword(), this));
                connectionJson.put("token",
                        config.getOauthToken() == null ? JSONObject.NULL : EncryptionUtils.encryptString(config.getOauthToken(), this));
                connectionsJsonArray.put(connectionJson);
                printWriter.print(connectionsJson.toString());
                printWriter.flush();
                Log.i(TAG, "Connection persisted");
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Connections file was not found", e);
            } catch (JSONException e) {
                Log.e(TAG, "Couldn't parse connections json", e);
            }
        }
    }

    private boolean exists(Config config) throws BrokenSecureStoreDataException, SecureStoreNotSupportedException {
        try {
            JSONObject connectionsJson = new JSONObject(IOUtils.readFile(openFileInput(getString(R.string.connections_file_key))));
            JSONArray connectionsJsonArray = connectionsJson.getJSONArray("connections");
            for (int i = 0; i < connectionsJsonArray.length(); i++) {
                JSONObject connectionJson = connectionsJsonArray.getJSONObject(i);
                if (!config.getMasterUrl().equals(connectionJson.getString("url"))) {
                    continue;
                }
                Log.i(TAG, "Connection with the this url already exists");
                if (config.getUsername() != null && config.getUsername().equals(connectionJson.getString("username"))) {
                    return true;
                }
                if (config.getOauthToken() != null && EncryptionUtils.encryptString(config.getOauthToken(), this).equals(connectionJson.getString("token"))) {
                    return true;
                }
            }
            return false;
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Connections file was not found", e);
            return false;
        } catch (JSONException e) {
            Log.w(TAG, "Couldn't parse connections json", e);
            return false;
        }
    }

    public static class TryConnectAsyncTask extends AsyncTask<Config, Integer, ConnectionResult> {

        @Override
        protected ConnectionResult doInBackground(Config... params) {
            try {
                Log.i(TAG, "Trying to connect");
                OkHttpClient okHttpClient = HttpClientUtils.createHttpClient(params[0]);
                RequestBody emptyRequestBody = RequestBody.create(null, new byte[0]);
                Request emptyRequest = new Request.Builder().url(params[0].getMasterUrl()).method("POST", emptyRequestBody).build();
                Response response = okHttpClient.newCall(emptyRequest).execute();
                if (response.code() == 401) {
                    Log.w(TAG, "Authentication failed");
                    return ConnectionResult.FAILED_AUTH;
                }
                Log.i(TAG, "Successfully connected");
                return ConnectionResult.SUCCESS;
            } catch (IOException e) {
                Log.e(TAG, "Couldn't connect. Network problems", e);
                return ConnectionResult.FAILED_NETWORK;
            }
        }
    }

}
