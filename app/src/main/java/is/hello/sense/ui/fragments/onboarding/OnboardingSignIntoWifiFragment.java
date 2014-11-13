package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import is.hello.sense.util.Logger;
import rx.functions.Action1;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle.wifi_endpoint.sec_type;

public class OnboardingSignIntoWifiFragment extends InjectionFragment {
    private static final String ARG_SCAN_RESULT = OnboardingSignIntoWifiFragment.class.getName() + ".ARG_SCAN_RESULT";

    private static final int ERROR_REQUEST_CODE = 0x30;

    @Inject ApiService apiService;
    @Inject PreferencesPresenter preferences;
    @Inject HardwarePresenter hardwarePresenter;

    private EditText networkName;
    private EditText networkPassword;
    private Spinner networkSecurity;

    private LoadingDialogFragment loadingDialogFragment;

    private @Nullable MorpheusBle.wifi_endpoint network;

    private boolean hasConnectedToNetwork = false;

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

        Button continueButton = (Button) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_continue);
        continueButton.setOnClickListener(ignored -> sendWifiCredentials());

        if (network != null) {
            this.networkName.setText(network.getSsid());
            if (network.getSecurityType() == sec_type.SL_SCAN_SEC_TYPE_OPEN) {
                this.networkPassword.setVisibility(View.GONE);
            } else {
                this.networkPassword.setVisibility(View.VISIBLE);
                this.networkPassword.requestFocus();
            }
            securityContainer.setVisibility(View.GONE);
        } else {
            networkSecurity.setAdapter(new SecurityTypeAdapter(getActivity()));
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (network != null && network.getSecurityType() == sec_type.SL_SCAN_SEC_TYPE_OPEN) {
            sendWifiCredentials();
        }
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
        this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(), getString(R.string.title_connecting_network), true);
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

        LoadingDialogFragment.close(getFragmentManager());

        OnboardingActivity activity = (OnboardingActivity) getActivity();
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

        if (hardwarePresenter.getPeripheral() == null) {
            Action1<Throwable> onError = this::presentError;
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(),
                             peripheral -> bindAndSubscribe(hardwarePresenter.connectToPeripheral(peripheral), status -> {
                                 if (status != HelloPeripheral.ConnectStatus.CONNECTED)
                                     return;

                                 sendWifiCredentials();
                             }, onError),
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
            loadingDialogFragment.setTitle(getString(R.string.title_linking_account));
            bindAndSubscribe(hardwarePresenter.linkAccount(), ignored -> finishedSettingWifi(), this::presentError);
        }
    }


    public void presentError(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        if (hardwarePresenter.isErrorFatal(e)) {
            ErrorDialogFragment.presentFatalBluetoothError(getFragmentManager(), getActivity());
        } else {
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
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
