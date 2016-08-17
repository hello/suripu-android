package is.hello.sense.ui.fragments.sense;

import android.os.Bundle;
import android.support.annotation.StringRes;

import com.segment.analytics.Properties;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.ui.activities.SenseUpdateActivity;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public abstract class BasePairSenseFragment extends HardwareFragment {

    @Inject
    ApiService apiService;

    protected static final String OPERATION_LINK_ACCOUNT = "Linking account";
    protected static final String ARG_HAS_LINKED_ACCOUNT = "hasLinkedAccount";
    protected boolean hasLinkedAccount = false;

    public abstract void presentError(final Throwable e, final String operation);

    /**
     * Will be called when {@link this#finishUpOperations()} completes
     */
    protected abstract void onFinished();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.hasLinkedAccount = savedInstanceState.getBoolean(ARG_HAS_LINKED_ACCOUNT, false);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_HAS_LINKED_ACCOUNT, hasLinkedAccount);
    }

    protected boolean isPairUpgradedSenseSession() {
        return getActivity() instanceof SenseUpdateActivity;
    }

    protected @StringRes
    int getOnFinishedSuccessMessage(){
        return isPairUpgradedSenseSession() ? R.string.title_paired : R.string.action_done;
    }

    protected void sendOnCreateAnalytics(final boolean pairOnlySession) {
        final Properties properties = Analytics.createBluetoothTrackingProperties(getActivity());
        if (pairOnlySession) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIR_SENSE_IN_APP, properties);
        } else {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIR_SENSE, properties);
        }
    }

    protected void sendOnFinishedAnalytics(final boolean pairOnlySession) {
        if (pairOnlySession) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SENSE_PAIRED_IN_APP, null);
        } else {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SENSE_PAIRED, null);
        }
    }

    public void linkAccount() {
        if (hasLinkedAccount) {
            finishUpOperations();
        } else {
            showBlockingActivity(R.string.title_linking_account);

            bindAndSubscribe(hardwarePresenter.linkAccount(),
                             ignored -> {
                                 this.hasLinkedAccount = true;
                                 finishUpOperations();
                             },
                             error -> {
                                 Logger.error(getClass().getSimpleName(), "Could not link Sense to account", error);
                                 presentError(error, OPERATION_LINK_ACCOUNT);
                             });
        }
    }

    public void finishUpOperations() {
        setDeviceTimeZone();
    }

    private void setDeviceTimeZone() {
        showBlockingActivity(R.string.title_setting_time_zone);

        final SenseTimeZone timeZone = SenseTimeZone.fromDefault();
        bindAndSubscribe(apiService.updateTimeZone(timeZone),
                         ignored -> {
                             Logger.info(getClass().getSimpleName(), "Time zone updated.");

                             pushDeviceData();
                         },
                         e -> presentError(e, "Updating time zone"));
    }

    private void pushDeviceData() {
        showBlockingActivity(R.string.title_pushing_data);

        bindAndSubscribe(hardwarePresenter.pushData(),
                         ignored -> getDeviceFeatures(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not push Sense data, ignoring.", error);
                             getDeviceFeatures();
                         });
    }

    private void getDeviceFeatures() {
        showBlockingActivity(R.string.title_pushing_data);

        bindAndSubscribe(userFeaturesPresenter.storeFeaturesInPrefs(),
                         ignored -> onFinished(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not get features from Sense, ignoring.", error);
                             onFinished();
                         });
    }
}
