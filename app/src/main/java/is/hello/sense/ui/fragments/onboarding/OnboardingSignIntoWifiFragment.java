package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONObject;

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

import static is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos.wifi_endpoint.sec_type;

public class OnboardingSignIntoWifiFragment extends HardwareFragment implements AdapterView.OnItemSelectedListener {
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

        JSONObject properties = Analytics.createProperties(
            Analytics.Onboarding.PROP_WIFI_IS_OTHER, (network == null)
        );
        Analytics.trackEvent(Analytics.Onboarding.EVENT_WIFI_PASSWORD, properties);

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
            updatePasswordField();

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

            if (network.getSecurityType() == sec_type.SL_SCAN_SEC_TYPE_OPEN) {
                title.setText(R.string.title_sign_into_wifi_selection_open);
            } else {
                title.setText(R.string.title_sign_into_wifi_selection);
            }
        } else {
            this.networkSecurity = (Spinner) otherContainer.findViewById(R.id.fragment_onboarding_sign_into_wifi_security);
            networkSecurity.setAdapter(new SecurityTypeAdapter(getActivity()));
            networkSecurity.setSelection(sec_type.SL_SCAN_SEC_TYPE_WPA2_VALUE);
            networkSecurity.setOnItemSelectedListener(this);

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


    //region Password Field

    private sec_type getSecurityType() {
        if (network != null) {
            return network.getSecurityType();
        } else {
            return (sec_type) networkSecurity.getSelectedItem();
        }
    }

    private void updatePasswordField() {
        sec_type securityType = getSecurityType();
        if (securityType == sec_type.SL_SCAN_SEC_TYPE_WEP) {
            networkPassword.setFilters(new InputFilter[]{new HexInputFilter()});
            networkPassword.setVisibility(View.VISIBLE);
            if (!HexInputFilter.isValidHex(networkPassword.getText())) {
                networkPassword.setText(null);
            }
            networkPassword.requestFocus();
        } else if (securityType == sec_type.SL_SCAN_SEC_TYPE_OPEN) {
            networkPassword.setFilters(new InputFilter[0]);
            networkPassword.setVisibility(View.GONE);
            networkPassword.setText(null);
            networkPassword.clearFocus();
        } else {
            networkPassword.setFilters(new InputFilter[0]);
            networkPassword.setVisibility(View.VISIBLE);
            networkPassword.requestFocus();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updatePasswordField();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        updatePasswordField();
    }

    //endregion


    private void sendWifiCredentials() {
        String networkName = this.networkName.getText().toString();
        String password = this.networkPassword.getText().toString();

        if (TextUtils.isEmpty(networkName) ||
                (TextUtils.isEmpty(password) && network != null &&
                        network.getSecurityType() != sec_type.SL_SCAN_SEC_TYPE_OPEN)) {
            return;
        }

        showBlockingActivity(R.string.title_connecting_network);

        if (!hardwarePresenter.hasPeripheral()) {
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(),
                             ignored -> sendWifiCredentials(),
                             e -> presentError(e, "Discovery"));
            return;
        }

        if (!hardwarePresenter.isConnected()) {
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(), status -> {
                if (status != HelloPeripheral.ConnectStatus.CONNECTED)
                    return;

                sendWifiCredentials();
            }, e -> presentError(e, "Connecting to Sense"));

            return;
        }

        showHardwareActivity(() -> {
            if (hasConnectedToNetwork) {
                sendAccessToken();
                return;
            }

            sec_type securityType = getSecurityType();

            JSONObject properties = Analytics.createProperties(
                Analytics.Onboarding.PROP_WIFI_SECURITY_TYPE, securityType.toString()
            );
            Analytics.trackEvent(Analytics.Onboarding.EVENT_WIFI_CREDENTIALS_SUBMITTED, properties);

            bindAndSubscribe(hardwarePresenter.sendWifiCredentials(networkName, networkName, securityType, password), ignored -> {
                this.hasConnectedToNetwork = true;
                preferences.edit()
                        .putString(PreferencesPresenter.PAIRED_DEVICE_SSID, networkName)
                        .apply();
                sendAccessToken();
            }, e -> presentError(e, "Setting WiFi"));
        }, e -> presentError(e, "Turning on LEDs"));
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
                             e -> presentError(e, "Linking account"));
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
                         e -> presentError(e, "Updating time zone"));
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
        if (getActivity().getIntent().getBooleanExtra(OnboardingActivity.EXTRA_WIFI_CHANGE_ONLY, false)) {
            completeHardwareActivity(() -> getOnboardingActivity().showPairPill(true), null);
        } else {
            hideAllActivityForSuccess(() -> getOnboardingActivity().showPairPill(true),
                                      e -> presentError(e, "Turning off LEDs"));
        }
    }


    public void presentError(Throwable e, @NonNull String operation) {
        hideAllActivityForFailure(() -> {
            ErrorDialogFragment dialogFragment = ErrorDialogFragment.presentBluetoothError(getFragmentManager(), e);
            dialogFragment.setErrorOperation(operation);
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

    private static class HexInputFilter implements InputFilter {
        private static boolean isHex(char c) {
            char lowerC = Character.toLowerCase(c);
            return ((lowerC >= '0' && lowerC <= '9') ||
                    (lowerC >= 'a' && lowerC <= 'f'));
        }

        private static boolean isValidHex(@NonNull CharSequence sequence) {
            for (int i = 0, length = sequence.length(); i < length; i++) {
                if (!isHex(sequence.charAt(i))) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                if (!isHex(source.charAt(i))) {
                    return source.subSequence(start, end - 1);
                }
            }

            return null;
        }
    }
}
