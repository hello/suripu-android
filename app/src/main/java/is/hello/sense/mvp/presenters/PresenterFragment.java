package is.hello.sense.mvp.presenters;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.presenters.outputs.HelpOutput;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Logger;

public abstract class PresenterFragment<T extends PresenterView>
        extends ObserverFragment
        implements HelpOutput {

    protected boolean animatorContextFromActivity = false;
    protected LoadingDialogFragment loadingDialogFragment;
    /**
     * Safe to assume this exists at and after {@link PresenterFragment#onViewCreated(View, Bundle)}
     * Reference is removed at {@link  PresenterFragment#onDestroyView()} and {@link PresenterFragment#onDetach()}
     */
    @VisibleForTesting
    public T presenterView;

    /**
     * Only called in {@link PresenterFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)} method.
     */
    public abstract void initializePresenterView();

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
    }

    @CallSuper
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @CallSuper
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @CallSuper
    @NonNull
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        initializePresenterView();
        return presenterView;
    }

    @CallSuper
    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.viewCreated();
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        presenterView.resume();
    }

    @CallSuper
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (presenterView != null) {
            this.presenterView.destroyView();
        }
        onRelease();
    }

    @CallSuper
    @Override
    public void onDetach() {
        super.onDetach();
        if (presenterView != null) {
            this.presenterView.detach();
        }
        onRelease();
    }

    @CallSuper
    protected void onRelease() {
        this.animatorContext = null;
        this.animatorContextFromActivity = false;
        this.presenterView = null;
        this.loadingDialogFragment = null;
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

    public void hideBlockingActivity(final boolean success, @Nullable final Runnable onCompletion) {
        if (success) {
            LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                this.loadingDialogFragment = null;
                if(onCompletion != null) {
                    onCompletion.run();
                }
            });
        } else {
            LoadingDialogFragment.close(getFragmentManager());
            this.loadingDialogFragment = null;
            if(onCompletion != null) {
                onCompletion.run();
            }
        }
    }

    public void showBlockingActivity(@StringRes final int titleRes) {
        showBlockingActivity(getString(titleRes));
    }

    public void showBlockingActivity(final String titleRes) {
        if (loadingDialogFragment == null) {
            this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(),
                                                                    titleRes,
                                                                    LoadingDialogFragment.OPAQUE_BACKGROUND);
        } else {
            loadingDialogFragment.setTitle(titleRes);
        }
    }

    public void showErrorDialog(@NonNull final ErrorDialogFragment.PresenterBuilder builder) {
        builder.build().showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    public void showAlertDialog(@NonNull final SenseAlertDialog.Builder builder) {
        builder.build(getActivity()).show();
    }
    //endregion

    //region HelpOutput

    @Override
    public void showHelpUri(@NonNull final Uri uri) {
        UserSupport.openUri(getActivity(), uri);
    }

    @Override
    public void showHelpUri(@NonNull final String uri) {
        showHelpUri(Uri.parse(uri));
    }

    @Override
    public void showHelpUri(@NonNull final UserSupport.HelpStep helpStep) {
        UserSupport.showForHelpStep(getActivity(), helpStep);
    }

    @Override
    public void showHelpUri(@NonNull final UserSupport.DeviceIssue deviceIssue) {
        UserSupport.showForDeviceIssue(getActivity(), deviceIssue);
    }

    //endregion

    public void finishActivity() {
        getActivity().finish();
    }
}
