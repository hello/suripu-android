package is.hello.sense.flows.home.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

import is.hello.sense.flows.home.util.SoundsViewPagerPresenterDelegate;
import is.hello.sense.mvp.presenters.ViewPagerPresenterFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

public class SoundsPresenterFragment extends ViewPagerPresenterFragment {

    //region ViewPagerPresenterFragment
    @NonNull
    @Override
    protected BaseViewPagerPresenterDelegate newViewPagerDelegateInstance() {
        return new SoundsViewPagerPresenterDelegate(getResources());
    }
    //endRegion

    @Override
    protected boolean shouldAddViewPagerListener() {
        return true;
    }
}