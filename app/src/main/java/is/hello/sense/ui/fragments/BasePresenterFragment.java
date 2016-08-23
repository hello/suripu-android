package is.hello.sense.ui.fragments;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;

public abstract class BasePresenterFragment extends ScopedInjectionFragment
        implements OnBackPressedInterceptor {

    protected boolean animatorContextFromActivity = false;
    protected LoadingDialogFragment loadingDialogFragment;

    @Nullable
    protected AnimatorContext animatorContext;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        if (animatorContext == null && context instanceof AnimatorContext.Scene) {
            this.animatorContext = ((AnimatorContext.Scene) context).getAnimatorContext();
            this.animatorContextFromActivity = true;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.animatorContext = null;
        this.animatorContextFromActivity = false;
    }

    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);
        presenter.onTrimMemory(level);
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        presenter.execute(defaultBehavior);
        return false;
    }

    public void showBlockingActivity(@StringRes final int titleRes) {
        if (loadingDialogFragment == null) {
            presenter.execute(() -> this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(),
                                                                                            getString(titleRes),
                                                                                            LoadingDialogFragment.OPAQUE_BACKGROUND));
        } else {
            loadingDialogFragment.setTitle(getString(titleRes));
        }
    }

    public void hideBlockingActivity(@StringRes final int text, @Nullable final Runnable onCompletion) {
        presenter
                .execute(() -> LoadingDialogFragment
                        .closeWithMessageTransition(getFragmentManager(),
                                                    () -> {
                                                        this.loadingDialogFragment = null;
                                                        if (onCompletion != null) {
                                                            presenter.execute(onCompletion);
                                                        }
                                                    },
                                                    text));
    }

    public void hideBlockingActivity(final boolean success, @NonNull final Runnable onCompletion) {
        presenter.execute(() -> {
            if (success) {
                LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                    this.loadingDialogFragment = null;
                    presenter.execute(onCompletion);
                });
            } else {
                LoadingDialogFragment.close(getFragmentManager());
                this.loadingDialogFragment = null;
                onCompletion.run();
            }
        });
    }

}
