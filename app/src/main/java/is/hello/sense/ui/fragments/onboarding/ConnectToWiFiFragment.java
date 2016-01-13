package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.segment.analytics.Properties;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.util.Operation;
import is.hello.commonsense.bluetooth.errors.SenseSetWifiValidationError;
import is.hello.commonsense.bluetooth.model.SenseConnectToWiFiUpdate;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_endpoint;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import is.hello.sense.util.Logger;

import static is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_endpoint.sec_type;

public class ConnectToWiFiFragment extends HardwareFragment
        implements AdapterView.OnItemSelectedListener {
    public static final String ARG_USE_IN_APP_EVENTS = ConnectToWiFiFragment.class.getName() + ".ARG_USE_IN_APP_EVENTS";
    public static final String ARG_SEND_ACCESS_TOKEN = ConnectToWiFiFragment.class.getName() + ".ARG_SEND_ACCESS_TOKEN";
    public static final String ARG_SCAN_RESULT = ConnectToWiFiFragment.class.getName() + ".ARG_SCAN_RESULT";

    private static final int ERROR_REQUEST_CODE = 0x30;

    @Inject ApiService apiService;

    private boolean useInAppEvents;
    private boolean sendAccessToken;

    private EditText networkName;
    private EditText networkPassword;
    private Spinner networkSecurity;

    private @Nullable wifi_endpoint network;

    private boolean hasConnectedToNetwork = false;
    private boolean hasSentAccessToken = false;


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.useInAppEvents = getArguments().getBoolean(ARG_USE_IN_APP_EVENTS);
        this.sendAccessToken = getArguments().getBoolean(ARG_SEND_ACCESS_TOKEN, true);

        this.network = (wifi_endpoint) getArguments().getSerializable(ARG_SCAN_RESULT);
        if (savedInstanceState != null) {
            this.hasConnectedToNetwork = savedInstanceState.getBoolean("hasConnectedToNetwork", false);
            this.hasSentAccessToken = savedInstanceState.getBoolean("hasSentAccessToken", false);
        }

        Properties properties = Analytics.createProperties(
                Analytics.Onboarding.PROP_WIFI_IS_OTHER, (network == null)
                                                          );

        int rssi = 0;
        if (network!= null){
           rssi =  network.getRssi();
        }
        properties.put(Analytics.Onboarding.PROP_WIFI_RSSI, rssi);

        if (useInAppEvents) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_WIFI_PASSWORD_IN_APP, properties);
        } else {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_WIFI_PASSWORD, properties);
        }

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_connect_to_wifi, container, false);

        this.networkName = (EditText) view.findViewById(R.id.fragment_connect_to_wifi_network);
        this.networkPassword = (EditText) view.findViewById(R.id.fragment_connect_to_wifi_password);
        networkPassword.setOnEditorActionListener(new EditorActionHandler(this::sendWifiCredentials));

        final Button continueButton = (Button) view.findViewById(R.id.fragment_connect_to_wifi_continue);
        Views.setSafeOnClickListener(continueButton, ignored -> sendWifiCredentials());

        final TextView title = (TextView) view.findViewById(R.id.fragment_connect_to_wifi_title);
        final TextView networkInfo = (TextView) view.findViewById(R.id.fragment_connect_to_wifi_info);
        final ViewGroup otherContainer = (ViewGroup) view.findViewById(R.id.fragment_connect_to_wifi_other_container);

        if (network != null) {
            networkName.setText(network.getSsid());
            updatePasswordField();

            final SpannableStringBuilder networkInfoBuilder = new SpannableStringBuilder();
            networkInfoBuilder.append(getString(R.string.label_wifi_network_name));
            final int start = networkInfoBuilder.length();
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
            this.networkSecurity = (Spinner) otherContainer.findViewById(R.id.fragment_connect_to_wifi_security);
            networkSecurity.setAdapter(new SecurityTypeAdapter(getActivity()));
            networkSecurity.setSelection(sec_type.SL_SCAN_SEC_TYPE_WPA2_VALUE);
            networkSecurity.setOnItemSelectedListener(this);

            networkName.requestFocus();

            networkInfo.setVisibility(View.GONE);
            otherContainer.setVisibility(View.VISIBLE);

            title.setText(R.string.title_sign_into_wifi_other);
        }

        final OnboardingToolbar toolbar = OnboardingToolbar.of(this, view);
        if (getActivity().getActionBar() != null) {
            toolbar.hide();
            setHasOptionsMenu(true);
        } else {
            toolbar.setWantsBackButton(true)
                   .setOnHelpClickListener(ignored -> {
                       UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.SIGN_INTO_WIFI);
                   })
                   .setOnHelpLongClickListener(ignored -> {
                       showSupportOptions();
                       return true;
                   });
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
        outState.putBoolean("hasSentAccessToken", hasSentAccessToken);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ERROR_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            sendWifiCredentials();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.help, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help: {
                UserSupport.showForOnboardingStep(getActivity(),
                                                  UserSupport.OnboardingStep.SIGN_INTO_WIFI);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    //endregion


    //region Password Field

    private sec_type getSecurityType() {
        if (network != null) {
            return network.getSecurityType();
        } else {
            return (sec_type) networkSecurity.getSelectedItem();
        }
    }

    private void updatePasswordField() {
        final sec_type securityType = getSecurityType();
        if (securityType == sec_type.SL_SCAN_SEC_TYPE_WEP) {
            networkPassword.setVisibility(View.VISIBLE);
            networkPassword.requestFocus();
        } else if (securityType == sec_type.SL_SCAN_SEC_TYPE_OPEN) {
            networkPassword.setVisibility(View.GONE);
            networkPassword.setText(null);
            networkPassword.clearFocus();
        } else {
            networkPassword.setVisibility(View.VISIBLE);
            networkPassword.requestFocus();
        }
    }

    private boolean validatePasswordAsWepKey(@NonNull String password) {
        for (int i = 0, length = password.length(); i < length; i++) {
            final char c = Character.toLowerCase(password.charAt(i));
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
                return false;
            }
        }

        return true;
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
        final String networkName = this.networkName.getText().toString();
        final String password = this.networkPassword.getText().toString();

        final sec_type securityType = getSecurityType();
        if (TextUtils.isEmpty(networkName) ||
                (TextUtils.isEmpty(password) &&
                        securityType != sec_type.SL_SCAN_SEC_TYPE_OPEN)) {
            return;
        }

        if (securityType == sec_type.SL_SCAN_SEC_TYPE_WEP &&
                !validatePasswordAsWepKey(password)) {
            presentError(new SenseSetWifiValidationError(SenseSetWifiValidationError.Reason.MALFORMED_BYTES),
                    "WEP Validation");
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
                if (status != Operation.CONNECTED)
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

            final Properties properties = Analytics.createProperties(
                Analytics.Onboarding.PROP_WIFI_SECURITY_TYPE, securityType.toString()
            );
            final String updateEvent;
            if (useInAppEvents) {
                Analytics.trackEvent(Analytics.Onboarding.EVENT_WIFI_CREDENTIALS_SUBMITTED_IN_APP, properties);
                updateEvent = Analytics.Onboarding.EVENT_SENSE_WIFI_UPDATE_IN_APP;
            } else {
                Analytics.trackEvent(Analytics.Onboarding.EVENT_WIFI_CREDENTIALS_SUBMITTED, properties);
                updateEvent = Analytics.Onboarding.EVENT_SENSE_WIFI_UPDATE;
            }

            final AtomicReference<SenseConnectToWiFiUpdate> lastState = new AtomicReference<>(null);
            bindAndSubscribe(hardwarePresenter.sendWifiCredentials(networkName, securityType, password), status -> {
                Properties updateProperties = Analytics.createProperties(
                    Analytics.Onboarding.PROP_SENSE_WIFI_STATUS, status.state.toString(),
                    Analytics.Onboarding.PROP_SENSE_WIFI_HTTP_RESPONSE_CODE, status.httpResponseCode,
                    Analytics.Onboarding.PROP_SENSE_WIFI_SOCKET_ERROR_CODE, status.socketErrorCode
                );
                Analytics.trackEvent(updateEvent, updateProperties);

                lastState.set(status);

                if (status.state == SenseCommandProtos.wifi_connection_state.CONNECTED) {
                    this.hasConnectedToNetwork = true;
                    sendAccessToken();
                } else {
                    showBlockingActivity(Styles.getWiFiConnectStatusMessage(status));
                }
            }, e -> {
                final String operation = lastState.get() == null
                        ? "Setting WiFi"
                        : lastState.get().toString();
                presentError(e, operation);
            });
        }, e -> presentError(e, "Turning on LEDs"));
    }

    private void sendAccessToken() {
        if (hasSentAccessToken || !sendAccessToken) {
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

        final SenseTimeZone timeZone = SenseTimeZone.fromDefault();
        bindAndSubscribe(apiService.updateTimeZone(timeZone),
                         ignored -> {
                             Logger.info(ConnectToWiFiFragment.class.getSimpleName(), "Time zone updated.");

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
        if (getFragmentNavigation() instanceof OnboardingActivity && !isPairOnlySession()) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SENSE_PAIRED, null);
        } else {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SENSE_PAIRED_IN_APP, null);
        }

        hideAllActivityForSuccess(() -> {
            getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, null);
        }, e -> {
            getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, null);
            presentError(e, "Turning off LEDs");
        });
    }


    public void presentError(Throwable e, @NonNull String operation) {
        hideAllActivityForFailure(() -> {
            final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getResources())
                    .withOperation(operation);

            if (e instanceof SenseSetWifiValidationError &&
                    ((SenseSetWifiValidationError) e).reason == SenseSetWifiValidationError.Reason.MALFORMED_BYTES) {
                final Uri uri = UserSupport.DeviceIssue.SENSE_ASCII_WEP.getUri();
                final Intent intent = UserSupport.createViewUriIntent(getResources(), uri);
                errorDialogBuilder.withAction(intent, R.string.action_support);
            } else {
                errorDialogBuilder.withSupportLink();
            }

            final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
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
            final TextView text = (TextView) super.getView(position, convertView, parent);
            text.setText(getTitle(position));
            return text;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            final TextView text = (TextView) super.getDropDownView(position, convertView, parent);
            text.setText(getTitle(position));
            return text;
        }
    }

}
