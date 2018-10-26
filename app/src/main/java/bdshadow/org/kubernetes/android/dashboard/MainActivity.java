package bdshadow.org.kubernetes.android.dashboard;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;

import java.util.concurrent.ExecutionException;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.util.Config;

public class MainActivity extends AppCompatActivity {

    private NewKubernetesConnectionFragment newConnectionFragment;

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

    public void onNewConnectionClick(View view) throws ExecutionException, InterruptedException {
        final String url = ((android.widget.EditText)this.newConnectionFragment.getView().findViewById(R.id.editTextServerUrl)).getText().toString();
        RadioGroup radioGroupAuth = this.newConnectionFragment.getView().findViewById(R.id.radioGroupAuthType);
        if (radioGroupAuth.getCheckedRadioButtonId() == R.id.radioOAuth) {
            final String token = ((android.widget.EditText)this.newConnectionFragment.getView().findViewById(R.id.editTextToken)).getText().toString();
            new AsyncTask() {

                @Override
                protected Object doInBackground(Object[] objects) {
                    try {
                        ApiClient client = Config.fromToken(url, token);
                        Configuration.setDefaultApiClient(client);
                        CoreV1Api api = new CoreV1Api();
                        V1PodList list = api.listNamespacedPod("myproject4", null, null, null, null, null, null, null, null, null);
                        for (V1Pod item : list.getItems()) {
                            System.out.println(item.getMetadata().getName());
                        }
                    } catch (ApiException e) {
                        Log.e("MainActivity", "Error", e);
                    }
                    return new Object();
                }
            }.execute().get();
        } else {
            String username = this.newConnectionFragment.getView().findViewById(R.id.editTextUsername).toString();
            String password = this.newConnectionFragment.getView().findViewById(R.id.editTextPassword).toString();
        }

    }

}
