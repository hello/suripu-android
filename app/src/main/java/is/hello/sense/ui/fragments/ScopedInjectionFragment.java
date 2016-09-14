package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.common.SenseFragment;

/**
 * To be used when injecting fragments to a scoped object graph instead of application level graph.
 */
public abstract class ScopedInjectionFragment extends SenseFragment
        implements BaseOutput {

    /**
     * Will be called after injection of this fragment is made to the scoped object graph
     * which occurs after onAttach and before {@link this#onCreate(Bundle)}
     */
    public void onInjected() {

    }

    public void inject(@NonNull final Context context) {
        try {
            ((ScopedInjectionActivity) context).injectToScopedGraph(this);
            onInjected();
        } catch (final ClassCastException e) {
            throw new ClassCastException(context.getClass() + " needs to be instanceof " + ScopedInjectionActivity.class.getSimpleName());
        }
    }

    @Override
    public boolean canObservableEmit() {
        return isAdded() && !getActivity().isFinishing();
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        inject(context);
    }

    /**
     * Will still be called by devices with api >= 23 until migrate SenseFragment to extend Support Library Fragment.
     */
    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            inject(activity);
        }
    }
}
