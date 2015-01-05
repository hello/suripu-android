package is.hello.sense.ui.fragments;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;

/**
 * Extends InjectionFragment to add support for displaying
 * in-app and on Sense loading indicators.
 */
public abstract class HardwareFragment extends InjectionFragment {
    public @Inject HardwarePresenter hardwarePresenter;

    private LoadingDialogFragment loadingDialogFragment;

    protected void showBlockingActivity(@StringRes int titleRes) {
        if (loadingDialogFragment == null) {
            coordinator.postOnResume(() -> {
                this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(), getString(titleRes), true);
            });
        } else {
            loadingDialogFragment.setTitle(getString(titleRes));
        }
    }

    protected void hideBlockingActivity(boolean success, @NonNull Runnable onCompletion) {
        coordinator.postOnResume(() -> {
            if (success) {
                LoadingDialogFragment.close(getFragmentManager());
                onCompletion.run();
            } else {
                LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                    this.loadingDialogFragment = null;
                    onCompletion.run();
                });
            }
        });
    }


    protected void showHardwareActivity(@NonNull Runnable onCompletion) {
        bindAndSubscribe(hardwarePresenter.busyAnimation(),
                         ignored -> onCompletion.run(),
                         error -> onCompletion.run());
    }

    protected void hideHardwareActivity(@NonNull Runnable onCompletion) {
        if (hardwarePresenter.getPeripheral() != null) {
            bindAndSubscribe(hardwarePresenter.trippyAnimation(),
                             ignored -> onCompletion.run(),
                             error -> onCompletion.run());
        } else {
            onCompletion.run();
        }
    }

    protected void completeHardwareActivity(@NonNull Runnable onCompletion) {
        bindAndSubscribe(hardwarePresenter.stopAnimation(true),
                         ignored -> onCompletion.run(),
                         error -> onCompletion.run());
    }


    protected void hideAllActivity(boolean success, @NonNull Runnable onCompletion) {
        hideHardwareActivity(() -> hideBlockingActivity(success, onCompletion));
    }



    protected OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }
}
