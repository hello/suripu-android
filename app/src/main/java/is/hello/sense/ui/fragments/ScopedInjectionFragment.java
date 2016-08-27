package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.common.SenseFragment;

/**
 * To be used when injecting fragments to a scoped object graph instead of application level graph.
 */
public abstract class ScopedInjectionFragment extends SenseFragment
implements BaseOutput{

    private final ScopedPresenterContainer scopedPresenterContainer = new ScopedPresenterContainer();

    /**
     * Will be called after injection of this fragment is made to the scoped object graph
     * which occurs after onAttach and before {@link this#onCreate(Bundle)}
     */
    public abstract void onInjected();

    @Override
    public boolean canObservableEmit(){
        return isAdded() && !getActivity().isFinishing();
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try{
            ((ScopedInjectionActivity) context).injectToScopedGraph(this);
            onInjected();
        } catch (final ClassCastException e){
            throw new ClassCastException(context.getClass() + " needs to be instanceof " + ScopedInjectionActivity.class.getSimpleName());
        }
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try{
            ((ScopedInjectionActivity) activity).injectToScopedGraph(this);
            onInjected();
        } catch (final ClassCastException e){
            throw new ClassCastException(activity.getClass() + " needs to be instanceof " + ScopedInjectionActivity.class.getSimpleName());
        }

    }

    /**
     * Releases references to presenters in container
     */
    @Override
    public void onDetach() {
        super.onDetach();
        scopedPresenterContainer.onDetach();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scopedPresenterContainer.setView(this);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null){
            scopedPresenterContainer.onRestoreState(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        scopedPresenterContainer.onSaveState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        scopedPresenterContainer.onDestroyView();
    }

    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);
        scopedPresenterContainer.onTrimMemory(level);
    }

    protected void addScopedPresenter(final BasePresenter presenter) {
        scopedPresenterContainer.add(presenter);
    }

    public static class ScopedPresenterContainer {
        final List<BasePresenter> presenters = new ArrayList<>();

        public void onDestroyView(){
            for(final BasePresenter p : presenters){
                p.onDestroyView();
            }
        }

        public void onDetach(){
            for(final BasePresenter p : presenters){
                p.onDetach();
            }
            presenters.clear();
        }

        public void add(final BasePresenter presenter) {
            presenters.add(presenter);
        }

        public void onRestoreState(@NonNull final Bundle inState) {
            for (final BasePresenter presenter : presenters) {
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
            for (final BasePresenter presenter : presenters) {
                final Bundle savedState = presenter.onSaveState();
                if (savedState != null) {
                    outState.putParcelable(presenter.getSavedStateKey(), savedState);
                }
                presenter.onSaveInteractorState(outState);
            }
        }

        public void setView(final BaseOutput view) {
            for(final BasePresenter p : presenters){
                p.setView(view);
            }
        }

        public void onTrimMemory(final int level) {
            for(final BasePresenter p : presenters){
                p.onTrimMemory(level);
            }
        }
    }
}
