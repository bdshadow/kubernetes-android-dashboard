package org.bdshadow.kubernetes.android.dashboard;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.bdshadow.kubernetes.android.dashboard.utils.EncryptionUtils;
import org.bdshadow.kubernetes.android.dashboard.utils.IOUtils;
import org.bdshadow.kubernetes.android.dashboard.widget.ConnectionCardViewWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;

import io.fabric8.kubernetes.client.ConfigBuilder;


public class ConnectionsOverviewFragment extends Fragment {

    private static final String TAG = "ConnectionsOverviewFgmt";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getActivity().setTitle("Choose a connection");
        loadConnections();
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connections_overview, container, false);
    }

    protected void loadConnections() {
        try {
            Log.i(TAG, "Loading persisted connections");
            JSONObject connectionsJson = new JSONObject(IOUtils.readFile(getActivity().openFileInput(getString(R.string.connections_file_key))));
            JSONArray connectionsJsonArray = connectionsJson.getJSONArray("connections");
            for (int i = 0; i < connectionsJsonArray.length(); i++) {
                ConnectionCardViewWrapper cardViewWrapper = new ConnectionCardViewWrapper(getActivity());
                JSONObject connectionJson = connectionsJsonArray.getJSONObject(i);
                cardViewWrapper.fillWithJsonObject(connectionJson);
                cardViewWrapper.setOnClickListener(v -> {
                    ConfigBuilder configBuilder = new ConfigBuilder()
                            .withMasterUrl(connectionJson.optString("url"));
                    try {
                        if (connectionJson.isNull("password")) {
                            configBuilder.withOauthToken(EncryptionUtils.decryptString(connectionJson.optString("token"), getActivity()));
                        } else {
                            configBuilder.withUsername(connectionJson.optString("username"));
                            configBuilder.withPassword(EncryptionUtils.decryptString(connectionJson.optString("password"), getActivity()));
                        }
                    } catch (Exception e) {}
                    MainActivity.TryConnectAsyncTask tryConnectAsyncTask = new MainActivity.TryConnectAsyncTask();
                    tryConnectAsyncTask.execute(configBuilder.build());
                    try {
                        switch (tryConnectAsyncTask.get()) {
                            case SUCCESS:
                                System.out.println("### Successfully connected");
                                break;
                            case FAILED_NETWORK:
                                System.out.println("Network connectivity problem occurred");
                                break;
                            case FAILED_AUTH:
                                System.out.println("Authentication failed");
                                break;
                        }
                    } catch(Exception e) {
                        System.out.println("### Connection failed");
                    }
                });
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "connections file was not found", e);
            return;
        } catch (JSONException e) {
            Log.e(TAG, "connections json can't be parsed", e);
            return;
        }
    }

}
