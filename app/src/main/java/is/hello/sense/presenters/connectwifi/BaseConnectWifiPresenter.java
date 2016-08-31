package is.hello.sense.presenters.connectwifi;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.View;

import com.segment.analytics.Properties;

import java.util.concurrent.atomic.AtomicReference;

import is.hello.commonsense.bluetooth.errors.SenseSetWifiValidationError;
import is.hello.commonsense.bluetooth.model.SenseConnectToWiFiUpdate;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.presenters.BasePairSensePresenter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

public abstract class BaseConnectWifiPresenter extends BasePairSensePresenter<BaseConnectWifiPresenter.Output> {

    private static final String ARG_CONNECTED_TO_NETWORK = BaseConnectWifiPresenter.class.getSimpleName() + ".ARG_CONNECTED_TO_NETWORK";
    private boolean hasConnectedToNetwork = false;

    public BaseConnectWifiPresenter(final HardwareInteractor hardwareInteractor,
                                    final UserFeaturesInteractor userFeaturesInteractor,
                                    final ApiService apiService) {
        super(hardwareInteractor, userFeaturesInteractor, apiService);
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle bundle = super.onSaveState();
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putBoolean(ARG_CONNECTED_TO_NETWORK, hasConnectedToNetwork);
        return bundle;
    }

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        super.onRestoreState(savedState);
        this.hasConnectedToNetwork = savedState.getBoolean(ARG_CONNECTED_TO_NETWORK);
    }

    public abstract String getOnCreateAnalyticsEvent();

    public abstract String getOnSubmitWifiCredentialsAnalyticsEvent();

    public abstract String getWifiAnalyticsEvent();

    private void sendWifiCredentialsSubmittedAnalytics(final SenseCommandProtos.wifi_endpoint.sec_type securityType) {
        final Properties properties = Analytics.createProperties(
                Analytics.Onboarding.PROP_WIFI_SECURITY_TYPE, securityType.toString()
                                                                );
        Analytics.trackEvent(getOnSubmitWifiCredentialsAnalyticsEvent(), properties);
    }

    //@Override
    public void presentError(final Throwable e, @NonNull final String operation) {
        hideAllActivityForFailure(() -> {

            if (e instanceof SenseSetWifiValidationError &&
                    ((SenseSetWifiValidationError) e).reason == SenseSetWifiValidationError.Reason.MALFORMED_BYTES) {
                view.presentWifiValidationErrorDialog(
                        e,
                        operation,
                        UserSupport.DeviceIssue.SENSE_ASCII_WEP.getUri(),
                        R.string.action_support);
            } else {
                view.presentLinkedAccountErrorDialog(
                        e,
                        operation,
                        R.string.failed_to_link_account);
            }
        });
    }

    public void sendWifiCredentials() {
        final String networkName = view.getNetworkName();
        final String password = view.getNetworkPassword();
        final SenseCommandProtos.wifi_endpoint.sec_type securityType = view.getSecurityType();
        if (TextUtils.isEmpty(networkName) ||
                (TextUtils.isEmpty(password) &&
                        securityType != SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN)) {
            return;
        }

        if (securityType == SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_WEP &&
                !validatePasswordAsWepKey(password)) {
            presentError(new SenseSetWifiValidationError(SenseSetWifiValidationError.Reason.MALFORMED_BYTES),
                         "WEP Validation");
            return;
        }

        showBlockingActivity(R.string.title_connecting_network);

        if (!hardwareInteractor.hasPeripheral()) {
            bindAndSubscribe(hardwareInteractor.rediscoverLastPeripheral(),
                             ignored -> sendWifiCredentials(),
                             e -> presentError(e, "Discovery"));
            return;
        }

        if (!hardwareInteractor.isConnected()) {
            bindAndSubscribe(hardwareInteractor.connectToPeripheral(), status -> {
                if (status != ConnectProgress.CONNECTED) {
                    return;
                }

                sendWifiCredentials();
            }, e -> presentError(e, "Connecting to Sense"));

            return;
        }

        showHardwareActivity(() -> {
            if (hasConnectedToNetwork) {
                sendAccessToken();
                return;
            }

            sendWifiCredentialsSubmittedAnalytics(securityType);

            final String updateEvent = getWifiAnalyticsEvent();
            final AtomicReference<SenseConnectToWiFiUpdate> lastState = new AtomicReference<>(null);
            bindAndSubscribe(hardwareInteractor.sendWifiCredentials(networkName, securityType, password),
                             status -> {
                                 final Properties updateProperties = Analytics.createProperties(
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
                                ? "Setting WiFi" : lastState.get().toString();
                        presentError(e, operation);
                    });
        }, e -> presentError(e, "Turning on LEDs"));
    }

    private void sendAccessToken() {
        if (view.sendAccessToken()) {
            finishUpOperations();
        } else {
            checkLinkedAccount();
        }
    }

    private boolean validatePasswordAsWepKey(@NonNull final String password) {
        for (int i = 0, length = password.length(); i < length; i++) {
            final char c = Character.toLowerCase(password.charAt(i));
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
                return false;
            }
        }

        return true;
    }

    public void updatePasswordField() {
        final SenseCommandProtos.wifi_endpoint.sec_type securityType = view.getSecurityType();
        if (securityType == SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_WEP) {
            view.setNetworkPassword(View.VISIBLE, true, false);
        } else if (securityType == SenseCommandProtos.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN) {
            view.setNetworkPassword(View.GONE, false, true);
        } else {
            view.setNetworkPassword(View.VISIBLE, true, false);
        }
    }


    public interface Output extends BasePairSensePresenter.Output {

        void presentWifiValidationErrorDialog(Throwable e,
                                              String operation,
                                              Uri supportUri,
                                              @StringRes int actionStringRes);

        void presentLinkedAccountErrorDialog(Throwable e,
                                             String operation,
                                             @StringRes int titleRes);

        SenseCommandProtos.wifi_endpoint.sec_type getSecurityType();

        void setNetworkPassword(int visibility,
                                boolean requestFocus,
                                boolean clearInput);

        String getNetworkName();

        String getNetworkPassword();

        boolean sendAccessToken();
    }
}
