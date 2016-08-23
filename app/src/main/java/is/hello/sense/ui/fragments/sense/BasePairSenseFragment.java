package is.hello.sense.ui.fragments.sense;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

import com.segment.analytics.Properties;

import javax.inject.Inject;

import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.presenters.BasePairSensePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.fragments.BaseHardwareFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.Logger;
import is.hello.sense.util.SkippableFlow;

public abstract class BasePairSenseFragment extends BaseHardwareFragment
implements BasePairSensePresenter.Output{

    @Inject
    protected BasePairSensePresenter presenter;

    @Inject
    ApiService apiService;

    protected static final String OPERATION_LINK_ACCOUNT = "Linking account";

    public abstract void presentError(final Throwable e, final String operation);

    /**
     * Will be called when {@link this#finishUpOperations()} completes
     */
    protected abstract void onFinished();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addScopedPresenter(presenter);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    protected void sendOnCreateAnalytics(final boolean pairOnlySession) {
        final Properties properties = Analytics.createBluetoothTrackingProperties(getActivity());
        Analytics.trackEvent(presenter.getOnCreateAnalyticsEvent(), properties);
    }

    protected void sendOnFinishedAnalytics() {
        Analytics.trackEvent(presenter.getOnFinishAnalyticsEvent(), null);
    }

    @Override
    public void showBlockingMessage(@StringRes final int blockingRes){
        showBlockingActivity(blockingRes);
    }

    @Override
    public void requestLinkAccount(){
        showBlockingActivity(R.string.title_linking_account);

        bindAndSubscribe(hardwarePresenter.linkAccount(),
                         ignored -> presenter.updateLinkedAccount(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not link Sense to account", error);
                             presentError(error, OPERATION_LINK_ACCOUNT);
                         });
    }

    @Override
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


    protected void showSupportOptions() {
        if (isPairOnlySession() || getOnboardingActivity() == null) {
            return;
        }

        Analytics.trackEvent(Analytics.Onboarding.EVENT_SUPPORT_OPTIONS, null);

        final SenseBottomSheet options = new SenseBottomSheet(getActivity());
        options.setTitle(R.string.title_recovery_options);
        options.addOption(new SenseBottomSheet.Option(0)
                                  .setTitle(R.string.action_factory_reset)
                                  .setTitleColor(ContextCompat.getColor(getActivity(), R.color.destructive_accent))
                                  .setDescription(R.string.description_recovery_factory_reset));
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            options.addOption(new SenseBottomSheet.Option(1)
                                      .setTitle("Debug")
                                      .setTitleColor(ContextCompat.getColor(getActivity(), R.color.light_accent))
                                      .setDescription("If you're adventurous, but here there be dragons."));
            if (!isPairOnlySession() && getActivity() instanceof SkippableFlow) {
                options.addOption(new SenseBottomSheet.Option(2)
                                          .setTitle("Skip to End")
                                          .setTitleColor(ContextCompat.getColor(getActivity(), R.color.light_accent))
                                          .setDescription("If you're in a hurry."));
            }
        }
        options.setOnOptionSelectedListener(option -> {
            switch (option.getOptionId()) {
                case 0: {
                    promptForRecoveryFactoryReset();
                    break;
                }
                case 1: {
                    Distribution.startDebugActivity(getActivity());
                    break;
                }
                case 2: {
                    ((SkippableFlow) getActivity()).skipToEnd();
                    break;
                }
                default: {
                    throw new IllegalArgumentException();
                }
            }
            return true;
        });
        options.show();
    }

    protected void promptForRecoveryFactoryReset() {
        Analytics.trackEvent(Analytics.Backside.EVENT_FACTORY_RESET, null);
        final SenseAlertDialog confirmation = new SenseAlertDialog(getActivity());
        confirmation.setTitle(R.string.dialog_title_factory_reset);
        confirmation.setMessage(R.string.dialog_message_factory_reset);
        confirmation.setNegativeButton(android.R.string.cancel, null);
        confirmation.setPositiveButton(R.string.action_factory_reset,
                                       (ignored, which) -> performRecoveryFactoryReset());
        confirmation.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        confirmation.show();
    }

    private void performRecoveryFactoryReset() {
        showBlockingActivity(R.string.dialog_loading_message);

        if (!hardwarePresenter.hasPeripheral()) {
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(),
                             ignored -> performRecoveryFactoryReset(),
                             this::presentFactoryResetError);
        } else if (!hardwarePresenter.isConnected()) {
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(),
                             state -> {
                                 if (state != ConnectProgress.CONNECTED) {
                                     return;
                                 }
                                 performRecoveryFactoryReset();
                             },
                             this::presentFactoryResetError);
        } else {
            showHardwareActivity(() -> bindAndSubscribe(hardwarePresenter.unsafeFactoryReset(),
                                                        ignored -> hideBlockingActivity(true, () -> {
                                                            Analytics.setSenseId("unpaired");

                                                            final MessageDialogFragment powerCycleDialog = MessageDialogFragment.newInstance(R.string.title_power_cycle_sense_factory_reset,
                                                                                                                                             R.string.message_power_cycle_sense_factory_reset);
                                                            powerCycleDialog.showAllowingStateLoss(getFragmentManager(), MessageDialogFragment.TAG);

                                                            userFeaturesPresenter.reset();
                                                            getOnboardingActivity().showSetupSense(); //todo return a flow result. Requires activity changes
                                                        }),
                                                        this::presentFactoryResetError), this::presentFactoryResetError);
        }
    }

    private void presentFactoryResetError(final Throwable e) {
        hideBlockingActivity(false, () -> {
            final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getActivity())
                    .withOperation("Recovery Factory Reset")
                    .withSupportLink()
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    //endregion


    protected OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }
}

