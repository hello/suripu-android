package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Logger;

public abstract class BasePresenterFragment extends ScopedInjectionFragment {

    protected boolean animatorContextFromActivity = false;
    protected LoadingDialogFragment loadingDialogFragment;

    protected abstract BasePresenter getPresenter();

    @Nullable
    protected AnimatorContext animatorContext;

    @CallSuper
    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        if (animatorContext == null && context instanceof AnimatorContext.Scene) {
            this.animatorContext = ((AnimatorContext.Scene) context).getAnimatorContext();
            this.animatorContextFromActivity = true;
        }
        setView();
    }

    @CallSuper
    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return;
        }
        if (animatorContext == null && activity instanceof AnimatorContext.Scene) {
            this.animatorContext = ((AnimatorContext.Scene) activity).getAnimatorContext();
            this.animatorContextFromActivity = true;
        }
        setView();
    }

    private void setView() {
        if (getPresenter() == null) {
            throw new Error(getClass().getSimpleName() + " is missing a presenter");
        }
        getPresenter().setView(this);

    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        getPresenter().onResume();

    }

    @CallSuper
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPresenter().onCreate(savedInstanceState);
    }

    @CallSuper
    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getPresenter().onViewCreated();
    }

    @CallSuper
    @Override
    public void onPause() {
        super.onPause();
        getPresenter().onPause();
    }

    @CallSuper
    @Override
    public void onStop() {
        super.onStop();
        getPresenter().onStop();
    }


    @CallSuper
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getPresenter().onDestroyView();
    }

    @CallSuper
    @Override
    public void onDetach() {
        super.onDetach();
        this.animatorContext = null;
        this.animatorContextFromActivity = false;
    }

    @NonNull
    public AnimatorContext getAnimatorContext() {
        if (animatorContext == null) {
            this.animatorContext = new AnimatorContext(getClass().getSimpleName());
            Logger.debug(getClass().getSimpleName(), "Creating animator context");
        }

        return animatorContext;
    }
    //region BaseOutput

    public void hideBlockingActivity(@StringRes final int text, @Nullable final Runnable onCompletion) {
        LoadingDialogFragment.closeWithMessageTransition(getFragmentManager(),
                                                         () -> {
                                                             loadingDialogFragment = null;
                                                             if (onCompletion != null) {
                                                                 onCompletion.run();
                                                             }
                                                         },
                                                         text);
    }

    public void hideBlockingActivity(final boolean success, @NonNull final Runnable onCompletion) {
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

    public void showBlockingActivity(@StringRes final int titleRes) {
        if (loadingDialogFragment == null) {
            this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(),
                                                                    getString(titleRes),
                                                                    LoadingDialogFragment.OPAQUE_BACKGROUND);
        } else {
            loadingDialogFragment.setTitle(getString(titleRes));
        }
    }

    public void showErrorDialog(@NonNull final ErrorDialogFragment.PresenterBuilder builder) {
        builder.build().showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    public void showHelpUri(@NonNull final Uri uri) {
        UserSupport.openUri(getActivity(), uri);
    }

    public void showHelpUri(@NonNull final String uri) {
        showHelpUri(Uri.parse(uri));
    }

    public void showHelpUri(@NonNull final UserSupport.HelpStep helpStep) {
        UserSupport.showForHelpStep(getActivity(), helpStep);
    }

    public void showHelpUri(@NonNull final UserSupport.DeviceIssue deviceIssue) {
        UserSupport.showForDeviceIssue(getActivity(), deviceIssue);
    }

    public void finishActivity() {
        getActivity().finish();
    }

    //endregion

}
