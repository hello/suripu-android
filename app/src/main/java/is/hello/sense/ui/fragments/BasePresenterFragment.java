package is.hello.sense.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.presenters.BaseFragmentPresenter;
import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;

public abstract class BasePresenterFragment<T extends BaseFragmentPresenter> extends SenseFragment
        implements OnBackPressedInterceptor {
    public static final String ARG_PRESENTER_KEY = BasePresenter.class.getSimpleName() + ".ARG_PRESENTER_KEY";
    protected T presenter;
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
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getArguments().containsKey(ARG_PRESENTER_KEY)) {
            throw new Error("Missing presenter for fragment: " + getClass().getSimpleName());
        }
        presenter = (T) getArguments().getSerializable(ARG_PRESENTER_KEY);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        presenter.restoreState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.onResume();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        presenter.onSaveState(outState);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
