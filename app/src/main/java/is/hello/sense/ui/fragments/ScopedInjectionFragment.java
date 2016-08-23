package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.presenters.ScopedPresenter;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.common.SenseFragment;

/**
 * To be used when injecting fragments to a scoped object graph instead of application level graph.
 */
public abstract class ScopedInjectionFragment extends SenseFragment
implements BaseOutput{

    private final ScopedPresenterContainer scopedPresenterContainer = new ScopedPresenterContainer();

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

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try{
            ((ScopedInjectionActivity) activity).injectToScopedGraph(this);
        } catch (final ClassCastException e){
            throw new ClassCastException(activity.getClass() + " needs to be instanceof " + ScopedInjectionActivity.class.getSimpleName());
        }

    }



    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scopedPresenterContainer.onCreate(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        scopedPresenterContainer.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Any other call to this method is due to configuration change or low memory.
        // We want to release the presenter only when the fragment is truly done.
        scopedPresenterContainer.onDestroy();
    }

    protected void addScopedPresenter(final ScopedPresenter presenter) {
        scopedPresenterContainer.add(presenter);
    }

    //todo if it is only possible to inject one presenter at a time this can be removed
    public static class ScopedPresenterContainer {
        final List<ScopedPresenter> presenters = new ArrayList<>();

        public void onDestroyView(){
            for(final ScopedPresenter p : presenters){
                p.onDestroyView();
            }
        }

        public void onDestroy(){
            for(final ScopedPresenter p : presenters){
                p.onDestroy();
            }
            presenters.clear();
        }

        public void add(final ScopedPresenter presenter) {
            presenters.add(presenter);
        }

        public void onRestoreState(@NonNull final Bundle inState) {
            for (final ScopedPresenter presenter : presenters) {
                if (presenter.isStateRestored()) {
                    continue;
                }

                final Bundle savedState = inState.getParcelable(presenter.getSavedStateKey());
                if (savedState != null) {
                    presenter.onRestoreState(savedState);
                }
            }
        }

        public void onSaveState(final Bundle outState) {
            for (final ScopedPresenter presenter : presenters) {
                final Bundle savedState = presenter.onSaveState();
                if (savedState != null) {
                    outState.putParcelable(presenter.getSavedStateKey(), savedState);
                }
            }
        }

        public void onCreate(final BaseOutput view) {
            for(final ScopedPresenter p : presenters){
                p.setView(view);
            }
        }
    }
}
