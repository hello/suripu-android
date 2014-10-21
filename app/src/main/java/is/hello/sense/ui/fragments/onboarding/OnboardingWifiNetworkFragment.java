package is.hello.sense.ui.fragments.onboarding;

import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.WifiNetworkPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.WifiNetworkAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

public class OnboardingWifiNetworkFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject WifiNetworkPresenter networkPresenter;

    private WifiNetworkAdapter networkAdapter;
    private ListView listView;
    private Button turnOnWifiButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        networkPresenter.update();
        addPresenter(networkPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_wifi_networks, container, false);

        listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        View wifiView = inflater.inflate(R.layout.item_wifi_network, listView, false);
        wifiView.findViewById(R.id.item_wifi_network_locked).setVisibility(View.GONE);
        wifiView.findViewById(R.id.item_wifi_network_scanned).setVisibility(View.GONE);
        ((TextView) wifiView.findViewById(R.id.item_wifi_network_name)).setText(R.string.wifi_other_network);
        listView.addFooterView(wifiView, null, true);

        this.networkAdapter = new WifiNetworkAdapter(getActivity());
        listView.setAdapter(networkAdapter);

        this.turnOnWifiButton = (Button) view.findViewById(R.id.fragment_onboarding_wifi_networks_turn_on);
        turnOnWifiButton.setOnClickListener(v -> networkPresenter.showWifiSettingsFrom(getActivity()));

        Button helpButton = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        helpButton.setOnClickListener(v -> Toast.makeText(v.getContext().getApplicationContext(), "Hang in there...", Toast.LENGTH_SHORT).show());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(networkPresenter.networksInRange, this::bindScanResults, this::scanResultsUnavailable);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        ScanResult network = (ScanResult) adapterView.getItemAtPosition(position);
        ((OnboardingActivity) getActivity()).showSignIntoWifiNetwork(network);
    }


    public void bindScanResults(@NonNull List<ScanResult> scanResults) {
        networkAdapter.clear();
        if (scanResults.isEmpty() && !networkPresenter.isWifiNetworkScanningAvailable()) {
            listView.setVisibility(View.GONE);
            turnOnWifiButton.setVisibility(View.VISIBLE);
        } else {
            networkAdapter.addAll(scanResults);

            listView.setVisibility(View.VISIBLE);
            turnOnWifiButton.setVisibility(View.GONE);
        }
    }

    public void scanResultsUnavailable(Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
