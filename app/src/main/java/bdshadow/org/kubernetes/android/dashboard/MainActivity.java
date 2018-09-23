package bdshadow.org.kubernetes.android.dashboard;


import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private NewKubernetesConnectionFragment newConnectionFragment;

    public void addKubernetesConnectionClick(View v) {
        FrameLayout frameLayoutNewConnection = new FrameLayout(this);
        frameLayoutNewConnection.setLayoutParams(new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT));
        int frameLayoutId = View.generateViewId();
        frameLayoutNewConnection.setId(frameLayoutId);
        setContentView(frameLayoutNewConnection);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        this.newConnectionFragment = new NewKubernetesConnectionFragment();
        fragmentTransaction.add(frameLayoutId, this.newConnectionFragment);
        fragmentTransaction.commit();

//        Intent newKubernetesConncetionIntent = new Intent(this, NewKubernetesConnectionFragment.class);
//        startActivity(newKubernetesConncetionIntent);
    }

    public void onNewConnectionClick(View view) {
        getSupportFragmentManager().beginTransaction().remove(this.newConnectionFragment).commit();
    }
}
