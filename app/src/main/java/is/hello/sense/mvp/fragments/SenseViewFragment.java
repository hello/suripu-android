package is.hello.sense.mvp.fragments;

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
import is.hello.sense.mvp.view.SenseView;
import is.hello.sense.presenters.outputs.HelpOutput;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Logger;

public abstract class SenseViewFragment<T extends SenseView>
        extends ObserverFragment
        implements HelpOutput {

    protected boolean animatorContextFromActivity = false;
    protected LoadingDialogFragment loadingDialogFragment;
    /**
     * Safe to assume this exists at and after {@link SenseViewFragment#onViewCreated(View, Bundle)}
     * Reference is removed at {@link  SenseViewFragment#onDestroyView()} and {@link SenseViewFragment#onDetach()}
     */
    @VisibleForTesting
    public T senseView;

    /**
     * Only called in {@link SenseViewFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)} method.
     */
    public abstract void initializeSenseView();

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
        //debugLog("onCreateView- initializeSenseView"); // useful for debugging
        initializeSenseView(); // todo force this to return a new instance
        return senseView;
    }

    @CallSuper
    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (senseView != null) { //todo remove check after forcing new instance
            senseView.viewCreated();
        }
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        if (senseView != null) {//todo remove check after forcing new instance
            senseView.resume();
        }
    }

    @CallSuper
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (senseView != null) {
            this.senseView.destroyView();
        }
        onRelease();
    }

    @CallSuper
    @Override
    public void onDetach() {
        super.onDetach();
        if (senseView != null) {
            this.senseView.detach();
        }
        onRelease();
    }

    @CallSuper
    protected void onRelease() {
        this.animatorContext = null;
        this.animatorContextFromActivity = false;
        this.senseView = null;
        this.loadingDialogFragment = null;
    }

    public final boolean hasPresenterView() {
       // debugLog("HasPresenterView: " + (senseView != null)); //useful for debugging
        return senseView != null;
    }

    @NonNull
    public AnimatorContext getAnimatorContext() {
        if (animatorContext == null) {
            this.animatorContext = new AnimatorContext(getClass().getSimpleName());
            Logger.debug(getClass().getSimpleName(), "Creating animator context");
        }

        return animatorContext;
    }
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
}
