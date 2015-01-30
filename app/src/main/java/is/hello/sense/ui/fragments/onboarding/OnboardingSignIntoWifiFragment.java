package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
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
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import is.hello.sense.util.Logger;
import rx.functions.Action1;

import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.wifi_endpoint.sec_type;

public class OnboardingSignIntoWifiFragment extends HardwareFragment {
    private static final String ARG_SCAN_RESULT = OnboardingSignIntoWifiFragment.class.getName() + ".ARG_SCAN_RESULT";

    private static final int ERROR_REQUEST_CODE = 0x30;

    @Inject ApiService apiService;
    @Inject PreferencesPresenter preferences;

    private EditText networkName;
    private EditText networkPassword;
    private Spinner networkSecurity;

    private @Nullable SenseCommandProtos.wifi_endpoint network;

    private boolean hasConnectedToNetwork = false;
    private boolean hasSentAccessToken = false;

    public static OnboardingSignIntoWifiFragment newInstance(@Nullable SenseCommandProtos.wifi_endpoint network) {
        OnboardingSignIntoWifiFragment fragment = new OnboardingSignIntoWifiFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_SCAN_RESULT, network);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.network = (SenseCommandProtos.wifi_endpoint) getArguments().getSerializable(ARG_SCAN_RESULT);
        if (savedInstanceState != null) {
            this.hasConnectedToNetwork = savedInstanceState.getBoolean("hasConnectedToNetwork", false);
            this.hasSentAccessToken = savedInstanceState.getBoolean("hasSentAccessToken", false);
        }

        Analytics.trackEvent(Analytics.Onboarding.EVENT_WIFI_PASSWORD, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_sign_into_wifi, container, false);

        this.networkName = (EditText) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_network);
        this.networkPassword = (EditText) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_password);
        networkPassword.setOnEditorActionListener(new EditorActionHandler(this::sendWifiCredentials));

        Button continueButton = (Button) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_continue);
        Views.setSafeOnClickListener(continueButton, ignored -> sendWifiCredentials());

        TextView title = (TextView) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_title);
        TextView networkInfo = (TextView) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_info);
        ViewGroup otherContainer = (ViewGroup) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_other_container);

        if (network != null) {
            networkName.setText(network.getSsid());
            if (network.getSecurityType() == sec_type.SL_SCAN_SEC_TYPE_OPEN) {
                this.networkPassword.setVisibility(View.GONE);
            } else {
                this.networkPassword.setVisibility(View.VISIBLE);
                this.networkPassword.requestFocus();
            }

            SpannableStringBuilder networkInfoBuilder = new SpannableStringBuilder();
            networkInfoBuilder.append(getString(R.string.label_wifi_network_name));
            int start = networkInfoBuilder.length();
            networkInfoBuilder.append(network.getSsid());
            networkInfoBuilder.setSpan(
                    new ForegroundColorSpan(getResources().getColor(R.color.text_dark)),
                    start, networkInfoBuilder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            networkInfo.setText(networkInfoBuilder);

            networkInfo.setVisibility(View.VISIBLE);
            otherContainer.setVisibility(View.GONE);

            title.setText(R.string.title_sign_into_wifi_selection);
        } else {
            this.networkSecurity = (Spinner) otherContainer.findViewById(R.id.fragment_onboarding_sign_into_wifi_security);
            networkSecurity.setAdapter(new SecurityTypeAdapter(getActivity()));
            networkSecurity.setSelection(sec_type.SL_SCAN_SEC_TYPE_WPA2_VALUE);

            networkName.requestFocus();

            networkInfo.setVisibility(View.GONE);
            otherContainer.setVisibility(View.VISIBLE);

            title.setText(R.string.title_sign_into_wifi_other);
        }

        OnboardingToolbar.of(this, view)
                .setWantsBackButton(true)
                .setOnHelpClickListener(ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.SIGN_INTO_WIFI));

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
        outState.putBoolean("hasSentAccessToken", hasSentAccessToken);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ERROR_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            sendWifiCredentials();
        }
    }


    private void sendWifiCredentials() {
        String networkName = this.networkName.getText().toString();
        String password = this.networkPassword.getText().toString();

        if (TextUtils.isEmpty(networkName) ||
                (TextUtils.isEmpty(password) && network != null &&
                        network.getSecurityType() != sec_type.SL_SCAN_SEC_TYPE_OPEN)) {
            return;
        }

        showBlockingActivity(R.string.title_connecting_network);

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

        showHardwareActivity(() -> {
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
                preferences.edit()
                           .putString(PreferencesPresenter.PAIRED_DEVICE_SSID, networkName)
                           .apply();
                sendAccessToken();
            }, this::presentError);
        });
    }

    private void sendAccessToken() {
        if (hasSentAccessToken || getActivity().getIntent().getBooleanExtra(OnboardingActivity.EXTRA_WIFI_CHANGE_ONLY, false)) {
            setDeviceTimeZone();
        } else {
            showBlockingActivity(R.string.title_linking_account);

            bindAndSubscribe(hardwarePresenter.linkAccount(),
                             ignored -> {
                                 this.hasSentAccessToken = true;
                                 setDeviceTimeZone();
                             },
                             this::presentError);
        }
    }

    private void setDeviceTimeZone() {
        showBlockingActivity(R.string.title_setting_time_zone);

        SenseTimeZone timeZone = SenseTimeZone.fromDefault();
        bindAndSubscribe(apiService.updateTimeZone(timeZone),
                         ignored -> {
                             Logger.info(OnboardingSignIntoWifiFragment.class.getSimpleName(), "Time zone updated.");

                             preferences.edit()
                                     .putString(PreferencesPresenter.PAIRED_DEVICE_TIME_ZONE, timeZone.timeZoneId)
                                     .apply();

                             pushDeviceData();
                         },
                         this::presentError);
    }

    private void pushDeviceData() {
        showBlockingActivity(R.string.title_pushing_data);

        bindAndSubscribe(hardwarePresenter.pushData(),
                         ignored -> finished(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not push Sense data, ignoring.", error);
                             finished();
                         });
    }

    private void finished() {
        hideAllActivity(true, () -> getOnboardingActivity().showPairPill(true));
    }


    public void presentError(Throwable e) {
        hideAllActivity(false, () -> {
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
        });
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
