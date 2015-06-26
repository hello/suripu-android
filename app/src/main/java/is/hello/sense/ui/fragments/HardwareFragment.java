package is.hello.sense.ui.fragments;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.devices.SensePeripheral;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
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

    protected void showBlockingActivity(@StringRes int titleRes) {
        if (loadingDialogFragment == null) {
            stateSafeExecutor.execute(() -> {
                this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(), getString(titleRes), true);
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
        bindAndSubscribe(hardwarePresenter.runLedAnimation(SensePeripheral.LedAnimation.BUSY),
                         ignored -> onCompletion.run(),
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Error occurred when showing hardware activity.", e);
                             onError.call(e);
                         });
    }

    protected void hideHardwareActivity(@NonNull Runnable onCompletion,
                                        @Nullable Action1<Throwable> onError) {
        if (hardwarePresenter.isConnected()) {
            bindAndSubscribe(hardwarePresenter.runLedAnimation(SensePeripheral.LedAnimation.TRIPPY),
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
        bindAndSubscribe(hardwarePresenter.runLedAnimation(SensePeripheral.LedAnimation.STOP),
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


    protected OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }
}
