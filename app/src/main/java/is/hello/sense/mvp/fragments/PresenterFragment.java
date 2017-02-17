package is.hello.sense.mvp.fragments;


import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.mvp.presenters.SensePresenter;
import is.hello.sense.ui.common.SenseFragment;

public abstract class PresenterFragment<SP extends SensePresenter> extends SenseFragment {

    private SP sensePresenter;

    public abstract SP initializeSensePresenter();


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
        //debugLog("onCreateView- initializeSensePresenter"); // useful for debugging
        return getSensePresenter().getSenseView();
    }

    @CallSuper
    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
    }

    @CallSuper
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        onRelease();
    }

    @CallSuper
    @Override
    public void onDetach() {
        super.onDetach();
        onRelease();
    }

    @CallSuper
    protected void onRelease() {
        this.sensePresenter = null;
    }

    public SP getSensePresenter() {
        if (this.sensePresenter == null) {
            this.sensePresenter = initializeSensePresenter();
        }
        return this.sensePresenter;
    }


}
