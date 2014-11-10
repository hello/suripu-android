package is.hello.sense.ui.fragments.onboarding;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;
import rx.Observable;

public class OnboardingPairSenseFragment extends InjectionFragment {
    public static final String ARG_IS_SECOND_USER = OnboardingPairSenseFragment.class.getName() + ".ARG_IS_SECOND_USER";

    @Inject HardwarePresenter hardwarePresenter;

    private boolean isSecondUser = false;
    private BluetoothAdapter bluetoothAdapter;

    private Button nextButton;
    private LoadingDialogFragment loadingDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.isSecondUser = (getArguments() != null && getArguments().getBoolean(ARG_IS_SECOND_USER, false));
        this.bluetoothAdapter = ((BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        Analytics.event(Analytics.EVENT_ONBOARDING_PAIR_SENSE, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_pair_sense, container, false);

        this.nextButton = (Button) view.findViewById(R.id.fragment_onboarding_step_continue);
        nextButton.setOnClickListener(this::next);
        updateNextButton();

        Button helpButton = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        helpButton.setOnClickListener(this::next);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        subscribe(hardwarePresenter.bluetoothEnabled, ignored -> updateNextButton(), Functions.LOG_ERROR);
    }


    private void updateNextButton() {
        if (bluetoothAdapter.isEnabled())
            nextButton.setText(R.string.action_continue);
        else
            nextButton.setText(R.string.action_turn_on_ble);
    }

    private void beginPairing() {
        this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(), getString(R.string.title_scanning_for_sense), true);
    }

    private void finishedPairing() {
        LoadingDialogFragment.close(getFragmentManager());

        OnboardingActivity activity = (OnboardingActivity) getActivity();
        if (isSecondUser) {
            activity.showPairPill(-1);
        } else {
            activity.showSelectWifiNetwork();
        }
    }


    public void next(View ignored) {
        if (bluetoothAdapter.isEnabled()) {
            beginPairing();

            Observable<SensePeripheral> device = hardwarePresenter.scanForDevices().map(hardwarePresenter::bestDeviceForPairing);
            bindAndSubscribe(device, this::pairWith, this::pairingFailed);
        } else {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        }
    }

    public void pairWith(@Nullable SensePeripheral device) {
        if (device != null) {
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(device), status -> {
                switch (status) {
                    case CONNECTING:
                        loadingDialogFragment.setTitle(getString(R.string.title_connecting));
                        break;

                    case BONDING:
                        loadingDialogFragment.setTitle(getString(R.string.title_pairing));
                        break;

                    case DISCOVERING_SERVICES:
                        loadingDialogFragment.setTitle(getString(R.string.title_discovering_services));
                        break;

                    case CONNECTED:
                        finishedPairing();
                        break;
                }
            }, this::pairingFailed);
        } else {
            pairingFailed(new Exception("Could not find any devices."));
        }
    }

    public void pairingFailed(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        if (hardwarePresenter.isErrorFatal(e)) {
            ErrorDialogFragment.presentFatalBluetoothError(getFragmentManager(), getActivity());
        } else {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        }
    }
}
