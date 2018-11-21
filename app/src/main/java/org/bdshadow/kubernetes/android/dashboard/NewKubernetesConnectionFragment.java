package org.bdshadow.kubernetes.android.dashboard;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

public class NewKubernetesConnectionFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        final View fragementView = inflater.inflate(R.layout.fragment_new_kubernetes_connection, container, false);
        RadioGroup radioGroupAuth = fragementView.findViewById(R.id.radioGroupAuthType);
        radioGroupAuth.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioOAuth) {
                    fragementView.findViewById(R.id.layoutBasicAuth).setVisibility(View.GONE);
                    fragementView.findViewById(R.id.layoutOAuth).setVisibility(View.VISIBLE);
                    ConstraintLayout.LayoutParams
                            layoutParams = (ConstraintLayout.LayoutParams)fragementView.findViewById(R.id.buttonConnect).getLayoutParams();
                    layoutParams.topToBottom = R.id.layoutOAuth;
                } else {
                    fragementView.findViewById(R.id.layoutBasicAuth).setVisibility(View.VISIBLE);
                    fragementView.findViewById(R.id.layoutOAuth).setVisibility(View.GONE);
                    ConstraintLayout.LayoutParams
                            layoutParams = (ConstraintLayout.LayoutParams)fragementView.findViewById(R.id.buttonConnect).getLayoutParams();
                    layoutParams.topToBottom = R.id.layoutBasicAuth;
                }
            }
        });
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        return fragementView;
    }

    @Override
    public void onResume() {
        AppCompatActivity activity = (AppCompatActivity)getActivity();
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.setTitle("New connection");
        super.onResume();
    }

}
