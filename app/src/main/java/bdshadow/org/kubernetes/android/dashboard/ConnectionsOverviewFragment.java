package bdshadow.org.kubernetes.android.dashboard;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ConnectionsOverviewFragment extends Fragment {

    @Override
    public void onResume() {
        getActivity().setTitle("Choose a connection");
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connections_overview, container, false);
    }

}
