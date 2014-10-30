package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hello.ble.protobuf.MorpheusBle;

import org.json.JSONObject;

import java.util.Collection;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.WifiNetworkAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;

public class OnboardingWifiNetworkFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject HardwarePresenter hardwarePresenter;

    private WifiNetworkAdapter networkAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View otherNetworkView;

    private long scanStarted = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(hardwarePresenter);

        Analytics.event(Analytics.EVENT_ONBOARDING_SETUP_WIFI, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_wifi_networks, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        this.otherNetworkView = inflater.inflate(R.layout.item_wifi_network, listView, false);
        otherNetworkView.findViewById(R.id.item_wifi_network_locked).setVisibility(View.GONE);
        otherNetworkView.findViewById(R.id.item_wifi_network_scanned).setVisibility(View.GONE);
        ((TextView) otherNetworkView.findViewById(R.id.item_wifi_network_name)).setText(R.string.wifi_other_network);
        otherNetworkView.setVisibility(View.GONE);
        listView.addFooterView(otherNetworkView, null, true);

        this.networkAdapter = new WifiNetworkAdapter(getActivity());
        listView.setAdapter(networkAdapter);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_onboarding_wifi_networks_refresh_container);
        swipeRefreshLayout.setColorSchemeResources(R.color.sleep_light, R.color.sleep_intermediate, R.color.sleep_deep, R.color.sleep_awake);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Analytics.event(Analytics.EVENT_ONBOARDING_WIFI_SCAN, null);
            rescanRepeating();
        });

        Button helpButton = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        helpButton.setOnClickListener(v -> Toast.makeText(v.getContext().getApplicationContext(), "Hang in there...", Toast.LENGTH_SHORT).show());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rescanRepeating();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        MorpheusBle.wifi_endpoint network = (MorpheusBle.wifi_endpoint) adapterView.getItemAtPosition(position);
        getOnboardingActivity().showSignIntoWifiNetwork(network);
    }


    public void rescanRepeating() {
        swipeRefreshLayout.setRefreshing(true);
        otherNetworkView.setVisibility(View.GONE);
        networkAdapter.clear();

        this.scanStarted = System.currentTimeMillis();
        bindAndSubscribe(hardwarePresenter.scanForWifiNetworks(), this::bindScanResults, this::scanResultsUnavailable);
    }

    public void bindScanResults(@NonNull Collection<MorpheusBle.wifi_endpoint> scanResults) {
        networkAdapter.clear();
        networkAdapter.addAll(scanResults);
        otherNetworkView.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(false);
        trackScanFinished(true);
    }

    public void scanResultsUnavailable(Throwable e) {
        swipeRefreshLayout.setRefreshing(false);
        ErrorDialogFragment.presentError(getFragmentManager(), e);
        trackScanFinished(false);
    }

    private void trackScanFinished(boolean succeeded) {
        long scanFinished = System.currentTimeMillis();
        long duration = (scanFinished - scanStarted) / 1000;
        JSONObject properties = Analytics.createProperties(Analytics.PROP_FAILED, !succeeded,
                                                           Analytics.PROP_DURATION, duration);
        Analytics.event(Analytics.EVENT_ONBOARDING_WIFI_SCAN_COMPLETED, properties);
    }


    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }
}
