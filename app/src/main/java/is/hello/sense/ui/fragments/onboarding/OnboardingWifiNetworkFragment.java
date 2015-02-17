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

import java.util.Collection;

import is.hello.sense.R;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.ui.adapter.WifiNetworkAdapter;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import rx.functions.Action1;

public class OnboardingWifiNetworkFragment extends HardwareFragment implements AdapterView.OnItemClickListener {
    private WifiNetworkAdapter networkAdapter;

    private TextView infoLabel;
    private TextView scanningIndicatorLabel;
    private ProgressBar scanningIndicator;
    private ListView listView;
    private Button rescanButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.networkAdapter = new WifiNetworkAdapter(getActivity());
        addPresenter(hardwarePresenter);

        Analytics.trackEvent(Analytics.Onboarding.EVENT_WIFI, null);

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
        Views.setSafeOnClickListener(rescanButton, ignored -> {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_WIFI_SCAN, null);
            rescan();
        });

        OnboardingToolbar.of(this, view)
                .setWantsBackButton(false)
                .setOnHelpClickListener(ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.WIFI_SCAN));

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
            rescanButton.setEnabled(true);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        SenseCommandProtos.wifi_endpoint network = (SenseCommandProtos.wifi_endpoint) adapterView.getItemAtPosition(position);
        getOnboardingActivity().showSignIntoWifiNetwork(network);
    }


    public void rescan() {
        scanningIndicatorLabel.setVisibility(View.VISIBLE);
        scanningIndicator.setVisibility(View.VISIBLE);
        infoLabel.setVisibility(View.INVISIBLE);
        listView.setVisibility(View.INVISIBLE);
        rescanButton.setVisibility(View.INVISIBLE);
        rescanButton.setEnabled(false);
        networkAdapter.clear();

        if (!hardwarePresenter.hasPeripheral()) {
            Action1<Throwable> onError = this::peripheralRediscoveryFailed;
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(),
                             ignored -> bindAndSubscribe(hardwarePresenter.connectToPeripheral(), status -> {
                                 if (status != HelloPeripheral.ConnectStatus.CONNECTED) {
                                     return;
                                 }

                                 rescan();
                             }, onError),
                             onError);
            return;
        }

        showHardwareActivity(() -> {
            bindAndSubscribe(hardwarePresenter.scanForWifiNetworks(),
                             this::bindScanResults,
                             this::scanResultsUnavailable);
        }, this::scanResultsUnavailable);
    }

    public void bindScanResults(@NonNull Collection<SenseCommandProtos.wifi_endpoint> scanResults) {
        hideHardwareActivity(() -> {
            networkAdapter.clear();
            networkAdapter.addAll(scanResults);

            scanningIndicatorLabel.setVisibility(View.GONE);
            scanningIndicator.setVisibility(View.GONE);
            infoLabel.setVisibility(View.VISIBLE);
            listView.setVisibility(View.VISIBLE);
            rescanButton.setVisibility(View.VISIBLE);
            rescanButton.setEnabled(true);
        }, null);
    }

    public void scanResultsUnavailable(Throwable e) {
        hideHardwareActivity(() -> {
            scanningIndicatorLabel.setVisibility(View.GONE);
            scanningIndicator.setVisibility(View.GONE);
            infoLabel.setVisibility(View.VISIBLE);
            listView.setVisibility(View.VISIBLE);
            rescanButton.setVisibility(View.VISIBLE);
            rescanButton.setEnabled(true);

            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
        }, null);
    }

    public void peripheralRediscoveryFailed(Throwable e) {
        scanningIndicatorLabel.setVisibility(View.GONE);
        scanningIndicator.setVisibility(View.GONE);
        infoLabel.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
        rescanButton.setVisibility(View.VISIBLE);
        rescanButton.setEnabled(true);

        ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
    }
}
