package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Collection;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.WifiNetworkAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;
import rx.functions.Action1;

public class OnboardingWifiNetworkFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject HardwarePresenter hardwarePresenter;

    private WifiNetworkAdapter networkAdapter;
    private ProgressBar scanningIndicator;

    private long scanStarted = 0;
    private ListView listView;
    private Button rescanButton;

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

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        View otherNetworkView = inflater.inflate(R.layout.item_wifi_network, listView, false);
        otherNetworkView.findViewById(R.id.item_wifi_network_locked).setVisibility(View.GONE);
        otherNetworkView.findViewById(R.id.item_wifi_network_scanned).setVisibility(View.GONE);
        ((TextView) otherNetworkView.findViewById(R.id.item_wifi_network_name)).setText(R.string.wifi_other_network);
        listView.addFooterView(otherNetworkView, null, true);

        this.networkAdapter = new WifiNetworkAdapter(getActivity());
        listView.setAdapter(networkAdapter);

        this.scanningIndicator = (ProgressBar) view.findViewById(R.id.fragment_onboarding_wifi_networks_scanning);

        this.rescanButton = (Button) view.findViewById(R.id.fragment_onboarding_wifi_networks_rescan);
        rescanButton.setEnabled(false);
        rescanButton.setOnClickListener(ignored -> {
            Analytics.event(Analytics.EVENT_ONBOARDING_WIFI_SCAN, null);
            rescan();
        });

        Button helpButton = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        helpButton.setOnClickListener(v -> Toast.makeText(v.getContext().getApplicationContext(), "Hang in there...", Toast.LENGTH_SHORT).show());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rescan();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        MorpheusBle.wifi_endpoint network = (MorpheusBle.wifi_endpoint) adapterView.getItemAtPosition(position);
        getOnboardingActivity().showSignIntoWifiNetwork(network);
    }


    public void rescan() {
        scanningIndicator.setVisibility(View.VISIBLE);
        listView.setVisibility(View.INVISIBLE);
        networkAdapter.clear();

        if (hardwarePresenter.getPeripheral() == null) {
            Action1<Throwable> onError = this::deviceRepairFailed;
            bindAndSubscribe(hardwarePresenter.rediscoverPeripheral(),
                             device -> bindAndSubscribe(hardwarePresenter.connectToPeripheral(device), status -> {
                                 if (status != HelloPeripheral.ConnectStatus.CONNECTED)
                                     return;

                                 rescan();
                             }, onError),
                             onError);
            return;
        }

        this.scanStarted = System.currentTimeMillis();
        bindAndSubscribe(hardwarePresenter.scanForWifiNetworks(), this::bindScanResults, this::scanResultsUnavailable);
    }

    public void bindScanResults(@NonNull Collection<MorpheusBle.wifi_endpoint> scanResults) {
        networkAdapter.clear();
        networkAdapter.addAll(scanResults);

        scanningIndicator.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
        rescanButton.setEnabled(true);
        trackScanFinished(true);
    }

    public void scanResultsUnavailable(Throwable e) {
        scanningIndicator.setVisibility(View.GONE);
        rescanButton.setEnabled(true);

        if (hardwarePresenter.isErrorFatal(e)) {
            ErrorDialogFragment.presentFatalBluetoothError(getFragmentManager(), getActivity());
        } else {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        }

        trackScanFinished(false);
    }

    public void deviceRepairFailed(Throwable e) {
        scanningIndicator.setVisibility(View.GONE);

        if (hardwarePresenter.isErrorFatal(e)) {
            ErrorDialogFragment.presentFatalBluetoothError(getFragmentManager(), getActivity());
        } else {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        }
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
