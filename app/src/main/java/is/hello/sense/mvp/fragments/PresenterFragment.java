package is.hello.sense.mvp.fragments;


import android.content.Intent;
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
        getSensePresenter().bindAndSubscribeAll();
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        getSensePresenterOrEmpty().getInteractorContainer().onResume();
    }

    @CallSuper
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getSensePresenterOrEmpty().getInteractorContainer().onDestroyView();
        onRelease();
    }

    @CallSuper
    @Override
    public void onDetach() {
        super.onDetach();
        getSensePresenterOrEmpty().getInteractorContainer().onDetach();
        onRelease();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getSensePresenterOrEmpty().onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        getSensePresenterOrEmpty().onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @CallSuper
    protected void onRelease() {
        this.sensePresenter = null;
    }

    private SensePresenter getSensePresenterOrEmpty() {
        if (this.sensePresenter == null) {
            return new SensePresenter.EmptySensePresenter(this);
        }
        return this.sensePresenter;
    }

    public SP getSensePresenter() {
        if (this.sensePresenter == null) {
            this.sensePresenter = initializeSensePresenter();
        }
        return this.sensePresenter;
    }

}
