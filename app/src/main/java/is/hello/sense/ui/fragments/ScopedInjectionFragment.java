package is.hello.sense.ui.fragments;

import android.content.Context;

import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.common.InjectionFragment;

/**
 * To be used when injecting fragments to a scoped object graph instead of application level graph.
 */
public abstract class ScopedInjectionFragment extends InjectionFragment{

    @Override
    protected boolean shouldInjectToMainGraphObject() {
        return false;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try{
            ((ScopedInjectionActivity) context).injectToScopedGraph(this);
        } catch (final ClassCastException e){
            throw new ClassCastException(context.getClass() + " needs to be instanceof " + ScopedInjectionActivity.class.getSimpleName());
        }

    }
}
