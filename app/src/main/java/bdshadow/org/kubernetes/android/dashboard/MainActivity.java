package bdshadow.org.kubernetes.android.dashboard;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;

import java.util.concurrent.ExecutionException;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

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
        final String url = ((android.widget.EditText) this.newConnectionFragment.getView().findViewById(R.id.editTextServerUrl)).getText().toString();
        RadioGroup radioGroupAuth = this.newConnectionFragment.getView().findViewById(R.id.radioGroupAuthType);
        if (radioGroupAuth.getCheckedRadioButtonId() == R.id.radioOAuth) {
            final String token = ((android.widget.EditText) this.newConnectionFragment.getView().findViewById(R.id.editTextToken)).getText().toString();
            try {
                new MyTask().execute(url, token).get();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } else {
            String username = this.newConnectionFragment.getView().findViewById(R.id.editTextUsername).toString();
            String password = this.newConnectionFragment.getView().findViewById(R.id.editTextPassword).toString();
        }

    }

    private static class MyTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            Config config = new ConfigBuilder().withMasterUrl(objects[0].toString()).withOauthToken(objects[1].toString()).build();
            KubernetesClient client = new DefaultKubernetesClient(config);
            for (Pod pod : client.pods().inNamespace("myproject4").list().getItems()) {
                System.out.println(pod.getMetadata().getName());
            }
            return new Object();
        }
    }

}
