package bdshadow.org.kubernetes.android.dashboard;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

public class NewKubernetesConnectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_kubernetes_connection);

        RadioGroup radioGroupAuth = findViewById(R.id.radioGroupAuthType);
        radioGroupAuth.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioOAuth) {
                    findViewById(R.id.layoutBasicAuth).setVisibility(View.GONE);
                    findViewById(R.id.layoutOAuth).setVisibility(View.VISIBLE);
                    ConstraintLayout.LayoutParams
                            layoutParams = (ConstraintLayout.LayoutParams)findViewById(R.id.buttonConnect).getLayoutParams();
                    layoutParams.topToBottom = R.id.layoutOAuth;
                } else {
                    findViewById(R.id.layoutBasicAuth).setVisibility(View.VISIBLE);
                    findViewById(R.id.layoutOAuth).setVisibility(View.GONE);
                    ConstraintLayout.LayoutParams
                            layoutParams = (ConstraintLayout.LayoutParams)findViewById(R.id.buttonConnect).getLayoutParams();
                    layoutParams.topToBottom = R.id.layoutBasicAuth;
                }
            }
        });
    }


}
