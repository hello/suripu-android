package is.hello.sense.ui.fragments;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.util.Operation;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.bluetooth.sense.model.SenseLedAnimation;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.Logger;
import rx.functions.Action1;

/**
 * Extends InjectionFragment to add support for displaying
 * in-app and on Sense loading indicators.
 */
public abstract class HardwareFragment extends InjectionFragment {
    public @Inject HardwarePresenter hardwarePresenter;

    private LoadingDialogFragment loadingDialogFragment;

    protected boolean isPairOnlySession() {
        return getActivity().getIntent().getBooleanExtra(OnboardingActivity.EXTRA_PAIR_ONLY, false);
    }

    protected boolean isWifiOnlySession() {
        return getActivity().getIntent().getBooleanExtra(OnboardingActivity.EXTRA_WIFI_CHANGE_ONLY, false);
    }


    //region Activity

    protected void showBlockingActivity(@StringRes int titleRes) {
        if (loadingDialogFragment == null) {
            stateSafeExecutor.execute(() -> {
                this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(),
                        getString(titleRes), LoadingDialogFragment.OPAQUE_BACKGROUND);
            });
        } else {
            loadingDialogFragment.setTitle(getString(titleRes));
        }
    }

    protected void hideBlockingActivity(boolean success, @NonNull Runnable onCompletion) {
        stateSafeExecutor.execute(() -> {
            if (success) {
                LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                    this.loadingDialogFragment = null;
                    stateSafeExecutor.execute(onCompletion);
                });
            } else {
                LoadingDialogFragment.close(getFragmentManager());
                this.loadingDialogFragment = null;
                onCompletion.run();
            }
        });
    }


    protected void showHardwareActivity(@NonNull Runnable onCompletion,
                                        @NonNull Action1<Throwable> onError) {
        bindAndSubscribe(hardwarePresenter.runLedAnimation(SenseLedAnimation.BUSY),
                ignored -> onCompletion.run(),
                e -> {
                    Logger.error(getClass().getSimpleName(), "Error occurred when showing hardware activity.", e);
                    onError.call(e);
                });
    }

    protected void hideHardwareActivity(@NonNull Runnable onCompletion,
                                        @Nullable Action1<Throwable> onError) {
        if (hardwarePresenter.isConnected()) {
            bindAndSubscribe(hardwarePresenter.runLedAnimation(SenseLedAnimation.TRIPPY),
                             ignored -> onCompletion.run(),
                             e -> {
                                 Logger.error(getClass().getSimpleName(), "Error occurred when hiding hardware activity.", e);
                                 if (onError != null) {
                                     onError.call(e);
                                 } else {
                                     onCompletion.run();
                                 }
                             });
        } else {
            stateSafeExecutor.execute(onCompletion);
        }
    }

    protected void completeHardwareActivity(@NonNull Runnable onCompletion) {
        bindAndSubscribe(hardwarePresenter.runLedAnimation(SenseLedAnimation.STOP),
                ignored -> onCompletion.run(),
                e -> {
                    Logger.error(getClass().getSimpleName(), "Error occurred when completing hardware activity", e);

                    onCompletion.run();
                });
    }


    protected void hideAllActivityForSuccess(@NonNull Runnable onCompletion,
                                             @NonNull Action1<Throwable> onError) {
        hideHardwareActivity(() -> hideBlockingActivity(true, onCompletion),
                e -> hideBlockingActivity(false, () -> onError.call(e)));
    }

    protected void hideAllActivityForFailure(@NonNull Runnable onCompletion) {
        Runnable next = () -> hideBlockingActivity(false, onCompletion);
        hideHardwareActivity(next, ignored -> next.run());
    }

    //endregion


    //region Recovery

    protected void showSupportOptions() {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_SUPPORT_OPTIONS, null);

        SenseBottomSheet options = new SenseBottomSheet(getActivity());
        options.setTitle(R.string.title_recovery_options);
        options.addOption(new SenseBottomSheet.Option(0)
                .setTitle(R.string.action_factory_reset)
                .setTitleColor(getResources().getColor(R.color.destructive_accent))
                .setDescription(R.string.description_recovery_factory_reset));
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            options.addOption(new SenseBottomSheet.Option(1)
                    .setTitle("Debug")
                    .setTitleColor(getResources().getColor(R.color.light_accent))
                    .setDescription("If you're adventurous, but here there be dragons."));
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
                default: {
                    throw new IllegalArgumentException();
                }
            }
            return true;
        });
        options.show();
    }

    protected void promptForRecoveryFactoryReset() {
        Analytics.trackEvent(Analytics.TopView.EVENT_FACTORY_RESET, null);

        SenseAlertDialog confirmation = new SenseAlertDialog(getActivity());
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
                        if (state != Operation.CONNECTED) {
                            return;
                        }
                        performRecoveryFactoryReset();
                    },
                    this::presentFactoryResetError);
        } else {
            showHardwareActivity(() -> {
                bindAndSubscribe(hardwarePresenter.unsafeFactoryReset(),
                        ignored -> {
                            hideBlockingActivity(true, () -> {
                                Analytics.setSenseId("unpaired");

                                MessageDialogFragment powerCycleDialog = MessageDialogFragment.newInstance(R.string.title_power_cycle_sense_factory_reset,
                                        R.string.message_power_cycle_sense_factory_reset);
                                powerCycleDialog.showAllowingStateLoss(getFragmentManager(), MessageDialogFragment.TAG);

                                getOnboardingActivity().showSetupSense();
                            });
                        },
                        this::presentFactoryResetError);
            }, this::presentFactoryResetError);
        }
    }

    private void presentFactoryResetError(Throwable e) {
        hideBlockingActivity(false, () -> {
            ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e)
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
