package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.hello.ble.protobuf.MorpheusBle;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import is.hello.sense.util.Logger;
import rx.functions.Action1;

import static com.hello.ble.BleOperationCallback.OperationFailReason;
import static com.hello.ble.protobuf.MorpheusBle.wifi_endpoint.sec_type;
import static is.hello.sense.util.BleObserverCallback.BluetoothError;

public class OnboardingSignIntoWifiFragment extends InjectionFragment {
    private static final String ARG_SCAN_RESULT = OnboardingSignIntoWifiFragment.class.getName() + ".ARG_SCAN_RESULT";

    private static final int ERROR_REQUEST_CODE = 0x30;
    private static final int ALREADY_LINKED_REQUEST_CODE = 0x66;

    @Inject ApiService apiService;
    @Inject PreferencesPresenter preferences;
    @Inject HardwarePresenter hardwarePresenter;

    private EditText networkName;
    private EditText networkPassword;
    private Spinner networkSecurity;

    private @Nullable MorpheusBle.wifi_endpoint network;

    private boolean hasConnectedToNetwork = false;
    private boolean hasTriedReconnect = false;

    public static OnboardingSignIntoWifiFragment newInstance(@Nullable MorpheusBle.wifi_endpoint network) {
        OnboardingSignIntoWifiFragment fragment = new OnboardingSignIntoWifiFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_SCAN_RESULT, network);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.network = (MorpheusBle.wifi_endpoint) getArguments().getSerializable(ARG_SCAN_RESULT);
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

        ViewGroup securityContainer = (ViewGroup) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_security_container);
        this.networkSecurity = (Spinner) securityContainer.findViewById(R.id.fragment_onboarding_sign_into_wifi_security);

        if (network != null) {
            this.networkName.setText(network.getSsid());
            this.networkPassword.requestFocus();
            securityContainer.setVisibility(View.GONE);
        } else {
            networkSecurity.setAdapter(new SecurityTypeAdapter(getActivity()));
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
        } else if (requestCode == ALREADY_LINKED_REQUEST_CODE && resultCode == DeviceAlreadyPairedDialogFragment.RESULT_ALREADY_LINKED) {
            finishedSettingWifi();
        }
    }

    private void beginSettingWifi() {
        ((OnboardingActivity) getActivity()).beginBlockingWork(R.string.title_connecting_network);
    }

    private void finishedSettingWifi() {
        SenseTimeZone timeZone = SenseTimeZone.fromDefault();
        apiService.updateTimeZone(timeZone)
                  .subscribe(ignored -> {
                      Logger.info(OnboardingSignIntoWifiFragment.class.getSimpleName(), "Time zone updated.");

                      preferences.edit()
                              .putString(PreferencesPresenter.PAIRED_DEVICE_TIME_ZONE, timeZone.timeZoneId)
                              .apply();
                  }, Functions.LOG_ERROR);

        OnboardingActivity activity = (OnboardingActivity) getActivity();
        activity.finishBlockingWork();
        activity.showPairPill(-1);
    }

    private void sendWifiCredentials() {
        String networkName = this.networkName.getText().toString();
        String password = this.networkPassword.getText().toString();

        if (TextUtils.isEmpty(networkName) ||
            (TextUtils.isEmpty(password) && network != null &&
             network.getSecurityType() != sec_type.SL_SCAN_SEC_TYPE_OPEN)) {
            return;
        }

        beginSettingWifi();

        if (hardwarePresenter.getDevice() == null) {
            Action1<Throwable> onError = this::deviceRepairFailed;
            bindAndSubscribe(hardwarePresenter.rediscoverDevice(),
                             device -> bindAndSubscribe(hardwarePresenter.connectToDevice(device), ignored -> sendWifiCredentials(), onError),
                             onError);
            return;
        }

        if (hasConnectedToNetwork) {
            sendAccessToken();
            return;
        }

        sec_type securityType;
        if (network != null) {
            securityType = network.getSecurityType();
        } else {
            securityType = (sec_type) networkSecurity.getSelectedItem();
        }

        bindAndSubscribe(hardwarePresenter.sendWifiCredentials(networkName, networkName, securityType, password), ignored -> {
            this.hasConnectedToNetwork = true;
            sendAccessToken();
        }, this::presentError);
    }

    private void sendAccessToken() {
        if (getActivity().getIntent().getBooleanExtra(OnboardingActivity.EXTRA_WIFI_CHANGE_ONLY, false)) {
            finishedSettingWifi();
        } else {
            bindAndSubscribe(hardwarePresenter.linkAccount(), ignored -> finishedSettingWifi(), this::presentError);
        }
    }


    private void tryDeviceReconnect() {
        this.hasTriedReconnect = true;
        bindAndSubscribe(hardwarePresenter.reconnect(), ignored -> sendWifiCredentials(), this::presentError);
    }

    public void presentError(Throwable e) {
        ErrorDialogFragment errorDialogFragment = null;
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

                    case DEVICE_ALREADY_PAIRED:
                        message = getString(R.string.error_sense_already_paired);
                        break;
                }

                errorDialogFragment = ErrorDialogFragment.newInstance(message);
            } else if (bluetoothError.failureReason == OperationFailReason.GATT_ERROR &&
                       bluetoothError.errorCode == 133 &&
                       !hasTriedReconnect) {
                tryDeviceReconnect();
                return;
            } else if (bluetoothError.failureReason == OperationFailReason.INTERNAL_ERROR &&
                       bluetoothError.errorCode == MorpheusBle.ErrorType.DEVICE_ALREADY_PAIRED_VALUE) {
                DeviceAlreadyPairedDialogFragment dialogFragment = new DeviceAlreadyPairedDialogFragment();
                dialogFragment.setTargetFragment(this, ALREADY_LINKED_REQUEST_CODE);
                dialogFragment.show(getFragmentManager(), DeviceAlreadyPairedDialogFragment.TAG);
                return;
            }
        }

        if (errorDialogFragment == null)
            errorDialogFragment = ErrorDialogFragment.newInstance(e);

        ((OnboardingActivity) getActivity()).finishBlockingWork();

        errorDialogFragment.setTargetFragment(this, ERROR_REQUEST_CODE);
        errorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    public void deviceRepairFailed(Throwable e) {
        ((OnboardingActivity) getActivity()).finishBlockingWork();
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    public static class DeviceAlreadyPairedDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
        public static final String TAG = DeviceAlreadyPairedDialogFragment.class.getSimpleName();

        public static final int RESULT_ALREADY_LINKED = 0;
        public static final int RESULT_NOT_LINKED = 1;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            SenseAlertDialog builder = new SenseAlertDialog(getActivity());

            builder.setTitle(R.string.dialog_title_sense_already_linked);
            builder.setMessage(R.string.dialog_message_sense_already_linked);
            builder.setPositiveButton(android.R.string.yes, this);
            builder.setNegativeButton(android.R.string.no, this);

            return builder;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            if (getTargetFragment() == null)
                return;

            if (which == DialogInterface.BUTTON_POSITIVE) {
                getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_ALREADY_LINKED, null);
            } else {
                getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_NOT_LINKED, null);
            }
        }
    }

    private static class SecurityTypeAdapter extends ArrayAdapter<sec_type> {
        private SecurityTypeAdapter(Context context) {
            super(context, R.layout.item_sec_type, sec_type.values());
        }

        private @StringRes int getTitle(int position) {
            switch (getItem(position)) {
                case SL_SCAN_SEC_TYPE_OPEN:
                    return R.string.sec_type_open;

                case SL_SCAN_SEC_TYPE_WEP:
                    return R.string.sec_type_wep;

                case SL_SCAN_SEC_TYPE_WPA:
                    return R.string.sec_type_wpa;

                case SL_SCAN_SEC_TYPE_WPA2:
                    return R.string.sec_type_wpa2;

                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text = (TextView) super.getView(position, convertView, parent);
            text.setText(getTitle(position));
            return text;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView text = (TextView) super.getDropDownView(position, convertView, parent);
            text.setText(getTitle(position));
            return text;
        }
    }
}
