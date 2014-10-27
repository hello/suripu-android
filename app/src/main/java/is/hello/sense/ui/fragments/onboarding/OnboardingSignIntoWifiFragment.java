package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.hello.ble.protobuf.MorpheusBle;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;

import static com.hello.ble.BleOperationCallback.OperationFailReason;
import static is.hello.sense.util.BleObserverCallback.BluetoothError;

public class OnboardingSignIntoWifiFragment extends InjectionFragment {
    private static final String ARG_SCAN_RESULT = OnboardingSignIntoWifiFragment.class.getName() + ".ARG_SCAN_RESULT";

    private static final int ERROR_REQUEST_CODE = 0x30;

    @Inject HardwarePresenter hardwarePresenter;

    private EditText networkName;
    private EditText networkPassword;

    private ScanResult network;

    private boolean hasConnectedToNetwork = false;

    public static OnboardingSignIntoWifiFragment newInstance(@Nullable ScanResult network) {
        OnboardingSignIntoWifiFragment fragment = new OnboardingSignIntoWifiFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_SCAN_RESULT, network);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.network = getArguments().getParcelable(ARG_SCAN_RESULT);
        if (savedInstanceState != null) {
            this.hasConnectedToNetwork = savedInstanceState.getBoolean("hasConnectedToNetwork", false);
        }

        Analytics.event(Analytics.EVENT_ONBOARDING_WIFI_PASSWORD, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_sign_into_wifi, container, false);

        this.networkName = (EditText) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_network);
        this.networkPassword = (EditText) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_password);
        networkPassword.setOnEditorActionListener(new EditorActionHandler(this::sendWifiCredentials));

        if (network != null) {
            this.networkName.setText(network.SSID);
            this.networkPassword.requestFocus();
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("hasConnectedToNetwork", hasConnectedToNetwork);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ERROR_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            sendWifiCredentials();
        }
    }

    private void beginSettingWifi() {
        ((OnboardingActivity) getActivity()).beginBlockingWork(R.string.title_connecting_network);
    }

    private void finishedSettingWifi() {
        OnboardingActivity activity = (OnboardingActivity) getActivity();
        activity.finishBlockingWork();
        activity.showSetupPill();
    }

    private void sendWifiCredentials() {
        String networkName = this.networkName.getText().toString();
        String password = this.networkPassword.getText().toString();

        if (TextUtils.isEmpty(networkName) || TextUtils.isEmpty(password)) {
            return;
        }

        beginSettingWifi();

        if (hasConnectedToNetwork) {
            sendAccessToken();
            return;
        }

        bindAndSubscribe(hardwarePresenter.sendWifiCredentials(networkName, networkName, password), ignored -> {
            this.hasConnectedToNetwork = true;
            sendAccessToken();
        }, this::presentError);
    }

    private void sendAccessToken() {
        bindAndSubscribe(hardwarePresenter.linkAccount(), ignored -> finishedSettingWifi(), this::presentError);
    }


    public void presentError(Throwable e) {
        ErrorDialogFragment dialogFragment = null;
        if (e instanceof BluetoothError) {
            BluetoothError bluetoothError = (BluetoothError) e;
            if (bluetoothError.failureReason == OperationFailReason.NETWORK_COULD_NOT_CONNECT) {
                networkPassword.requestFocus();

                MorpheusBle.ErrorType errorType = MorpheusBle.ErrorType.valueOf(bluetoothError.errorCode);
                String message;
                switch (errorType) {
                    default:
                    case NETWORK_ERROR:
                    case WLAN_CONNECTION_ERROR:
                        message = getString(R.string.error_bad_wifi_credentials);
                        break;

                    case NO_ENDPOINT_IN_RANGE:
                        message = getString(R.string.error_wifi_out_of_range);
                        break;

                    case FAIL_TO_OBTAIN_IP:
                        message = getString(R.string.error_wifi_ip_failure);
                        break;
                }

                dialogFragment = ErrorDialogFragment.newInstance(message);
            } else if (bluetoothError.failureReason == OperationFailReason.GATT_ERROR) {
                // TODO: Implicit reconnect here.
            }
        }

        if (dialogFragment == null)
            dialogFragment = ErrorDialogFragment.newInstance(e);

        ((OnboardingActivity) getActivity()).finishBlockingWork();

        dialogFragment.setTargetFragment(this, ERROR_REQUEST_CODE);
        dialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
    }
}
