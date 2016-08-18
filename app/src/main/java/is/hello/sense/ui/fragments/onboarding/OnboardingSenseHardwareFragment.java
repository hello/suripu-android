package is.hello.sense.ui.fragments.onboarding;

import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;

import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.fragments.BaseHardwareFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;

public abstract class OnboardingSenseHardwareFragment extends BaseHardwareFragment{
    //todo the recovery functions below force this fragment to depend on the activity creating it. We should phase away from this.
    //region Recovery

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
            if (!isPairOnlySession()) {
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
                    getOnboardingActivity().showHomeActivity(OnboardingActivity.FLOW_REGISTER); //todo return a flow result. Requires activity changes
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

                                                            MessageDialogFragment powerCycleDialog = MessageDialogFragment.newInstance(R.string.title_power_cycle_sense_factory_reset,
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


    protected OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }
}
