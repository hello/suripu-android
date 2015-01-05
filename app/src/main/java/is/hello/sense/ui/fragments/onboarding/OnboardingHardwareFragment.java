package is.hello.sense.ui.fragments.onboarding;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;

public class OnboardingHardwareFragment extends InjectionFragment {
    @Inject HardwarePresenter hardwarePresenter;

    private LoadingDialogFragment loadingDialogFragment;

    protected void showBlockingActivity(@StringRes int titleRes) {
        if (loadingDialogFragment == null) {
            this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(), getString(titleRes), true);
        } else {
            loadingDialogFragment.setTitle(getString(titleRes));
        }
    }

    protected void hideBlockingActivity(boolean success, @NonNull Runnable onCompletion) {
        if (success) {
            LoadingDialogFragment.close(getFragmentManager());
            onCompletion.run();
        } else {
            LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                this.loadingDialogFragment = null;
                onCompletion.run();
            });
        }
    }


    protected void showHardwareActivity(@NonNull Runnable onCompletion) {
        bindAndSubscribe(hardwarePresenter.busyAnimation(),
                         ignored -> onCompletion.run(),
                         error -> onCompletion.run());
    }

    protected void hideHardwareActivity(@NonNull Runnable onCompletion) {
        bindAndSubscribe(hardwarePresenter.trippyAnimation(),
                         ignored -> onCompletion.run(),
                         error -> onCompletion.run());
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
