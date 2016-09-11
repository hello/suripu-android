package is.hello.sense.mvp.presenters;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import is.hello.sense.SenseApplication;
import is.hello.sense.interactors.Interactor;
import is.hello.sense.interactors.InteractorContainer;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.common.SenseFragment;

/**
 * To be used when injecting fragments to a scoped object graph instead of application level graph.
 */
public abstract class ScopedInjectionFragment extends SenseFragment {
    private InteractorContainer interactorContainer = new InteractorContainer();

    /**
     * Will be called after injection of this fragment is made to the scoped object graph
     * which occurs after onAttach and before {@link this#onCreate(Bundle)}
     */
    public void onInjected() {

    }

    @CallSuper
    public final void inject(@NonNull final Context context) {
        try {
            ((ScopedInjectionActivity) context).injectToScopedGraph(this);
            onInjected();
        } catch (final ClassCastException e) {
            SenseApplication.getInstance().inject(this); //todo temporary until we phase out old ScopedInjectionActivity
            // throw new ClassCastException(context.getClass() + " needs to be instanceof " + ScopedInjectionActivity.class.getSimpleName());
        }
    }


    @CallSuper
    public final boolean canObservableEmit() {
        return isAdded() && !getActivity().isFinishing();
    }

    @CallSuper
    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        inject(context);
    }

    /**
     * Will still be called by devices with api >= 23 until migrate SenseFragment to extend Support Library Fragment.
     */
    @CallSuper
    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return;
        }
        inject(activity);
    }


    @CallSuper
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            interactorContainer.onRestoreState(savedInstanceState);
        }
    }

    @CallSuper
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        interactorContainer.onSaveState(outState);
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        interactorContainer.onContainerResumed();
    }

    @CallSuper
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.interactorContainer.onContainerDestroyed();
    }

    @CallSuper
    @Override
    public void onDetach() {
        super.onDetach();
        this.interactorContainer = null;
    }

    @CallSuper
    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);
        interactorContainer.onTrimMemory(level);
    }

    protected final void addInteractor(@NonNull final Interactor interactor) {
        interactorContainer.addInteractor(interactor);
    }
}
