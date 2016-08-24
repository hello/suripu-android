package is.hello.sense.ui.fragments;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
/**
 * Extends InjectionFragment to add support for displaying
 * in-app and on Sense loading indicators.
 */
public abstract class BaseHardwareFragment extends ScopedInjectionFragment {

    private LoadingDialogFragment loadingDialogFragment;

    protected boolean isPairOnlySession() {
        return getActivity().getIntent().getBooleanExtra(OnboardingActivity.EXTRA_PAIR_ONLY, false);
    }

    //region Activity

    @Override
    public void showBlockingActivity(@StringRes final int titleRes) {
        //todo wrap presenter call to view.showBlockingActivity with execute(() -> view.showBlockingActivity)
        if (loadingDialogFragment == null) {
            this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(),
                                                                    getString(titleRes),
                                                                    LoadingDialogFragment.OPAQUE_BACKGROUND);
        } else {
            loadingDialogFragment.setTitle(getString(titleRes));
        }
    }

    //todo wrap onCompletion runnable in stateSafeExecutor.bind() if non null
    protected void hideBlockingActivity(@StringRes final int text, @Nullable final Runnable onCompletion) {
         LoadingDialogFragment.closeWithMessageTransition(getFragmentManager(),
                                                          () -> {
                                                              this.loadingDialogFragment = null;
                                                        if (onCompletion != null) {
                                                            onCompletion.run();
                                                        }
                                                    },
                                                    text);
    }

    //todo wrap onCompletion runnable in stateSafeExecutor.bind() if non null
    protected void hideBlockingActivity(final boolean success, @NonNull final Runnable onCompletion) {
            if (success) {
                LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                    this.loadingDialogFragment = null;
                    onCompletion.run();
                });
            } else {
                LoadingDialogFragment.close(getFragmentManager());
                this.loadingDialogFragment = null;
                onCompletion.run();
            }
    }
}
