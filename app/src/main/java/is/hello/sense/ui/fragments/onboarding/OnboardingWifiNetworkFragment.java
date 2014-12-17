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

import org.json.JSONObject;

import java.util.Collection;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.WifiNetworkAdapter;
import is.hello.sense.ui.common.HelpUtil;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.util.Analytics;
import rx.functions.Action1;

public class OnboardingWifiNetworkFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject HardwarePresenter hardwarePresenter;

    private WifiNetworkAdapter networkAdapter;
    private long scanStarted = 0;

    private TextView infoLabel;
    private TextView scanningIndicatorLabel;
    private ProgressBar scanningIndicator;
    private ListView listView;
    private Button rescanButton;
    private Button helpButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.networkAdapter = new WifiNetworkAdapter(getActivity());
        addPresenter(hardwarePresenter);

        Analytics.event(Analytics.EVENT_ONBOARDING_SETUP_WIFI, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_wifi_networks, container, false);

        this.infoLabel = (TextView) view.findViewById(R.id.fragment_onboarding_wifi_networks_info);

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        View otherNetworkView = inflater.inflate(R.layout.item_wifi_network, listView, false);
        otherNetworkView.findViewById(R.id.item_wifi_network_locked).setVisibility(View.GONE);
        otherNetworkView.findViewById(R.id.item_wifi_network_scanned).setVisibility(View.GONE);
        ((TextView) otherNetworkView.findViewById(R.id.item_wifi_network_name)).setText(R.string.wifi_other_network);
        listView.addFooterView(otherNetworkView, null, true);

        listView.setAdapter(networkAdapter);

        this.scanningIndicatorLabel = (TextView) view.findViewById(R.id.fragment_onboarding_wifi_networks_scanning_label);
        this.scanningIndicator = (ProgressBar) view.findViewById(R.id.fragment_onboarding_wifi_networks_scanning);

        this.rescanButton = (Button) view.findViewById(R.id.fragment_onboarding_wifi_networks_rescan);
        rescanButton.setEnabled(false);
        rescanButton.setOnClickListener(ignored -> {
            Analytics.event(Analytics.EVENT_ONBOARDING_WIFI_SCAN, null);
            rescan();
        });

        this.helpButton = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        helpButton.setOnClickListener(ignored -> HelpUtil.showHelp(getActivity()));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (networkAdapter.getCount() == 0) {
            rescan();
        } else {
            scanningIndicatorLabel.setVisibility(View.GONE);
            scanningIndicator.setVisibility(View.GONE);
            infoLabel.setVisibility(View.VISIBLE);
            listView.setVisibility(View.VISIBLE);
            rescanButton.setVisibility(View.VISIBLE);
            helpButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        MorpheusBle.wifi_endpoint network = (MorpheusBle.wifi_endpoint) adapterView.getItemAtPosition(position);
        getOnboardingActivity().showSignIntoWifiNetwork(network);
    }


    public void rescan() {
        scanningIndicatorLabel.setVisibility(View.VISIBLE);
        scanningIndicator.setVisibility(View.VISIBLE);
        infoLabel.setVisibility(View.INVISIBLE);
        listView.setVisibility(View.INVISIBLE);
        rescanButton.setVisibility(View.INVISIBLE);
        rescanButton.setEnabled(false);
        helpButton.setVisibility(View.INVISIBLE);
        networkAdapter.clear();

        if (hardwarePresenter.getPeripheral() == null) {
            Action1<Throwable> onError = this::peripheralRediscoveryFailed;
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(),
                             peripheral -> bindAndSubscribe(hardwarePresenter.connectToPeripheral(peripheral), status -> {
                                 if (status != HelloPeripheral.ConnectStatus.CONNECTED) {
                                     return;
                                 }

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

        scanningIndicatorLabel.setVisibility(View.GONE);
        scanningIndicator.setVisibility(View.GONE);
        infoLabel.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
        rescanButton.setVisibility(View.VISIBLE);
        rescanButton.setEnabled(true);
        helpButton.setVisibility(View.VISIBLE);

        trackScanFinished(true);
    }

    public void scanResultsUnavailable(Throwable e) {
        scanningIndicatorLabel.setVisibility(View.GONE);
        scanningIndicator.setVisibility(View.GONE);
        infoLabel.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
        rescanButton.setVisibility(View.VISIBLE);
        rescanButton.setEnabled(true);
        helpButton.setVisibility(View.VISIBLE);

        if (hardwarePresenter.isErrorFatal(e)) {
            UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
            fragment.show(getFragmentManager(), R.id.activity_onboarding_container);
        } else {
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
        }

        trackScanFinished(false);
    }

    public void peripheralRediscoveryFailed(Throwable e) {
        scanningIndicatorLabel.setVisibility(View.GONE);
        scanningIndicator.setVisibility(View.GONE);
        infoLabel.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
        rescanButton.setVisibility(View.VISIBLE);
        helpButton.setVisibility(View.VISIBLE);
        
        if (hardwarePresenter.isErrorFatal(e)) {
            UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
            fragment.show(getFragmentManager(), R.id.activity_onboarding_container);
        } else {
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
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
