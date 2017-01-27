package is.hello.sense.flows.home.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

import is.hello.sense.flows.home.util.SoundsViewPagerPresenterDelegate;
import is.hello.sense.mvp.presenters.ViewPagerPresenterFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

public class SoundsPresenterFragment extends ViewPagerPresenterFragment
        implements ViewPager.OnPageChangeListener {

    //region ViewPagerPresenterFragment
    @NonNull
    @Override
    protected BaseViewPagerPresenterDelegate newViewPagerDelegateInstance() {
        return new SoundsViewPagerPresenterDelegate(getResources());
    }
    //endRegion
    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.addViewPagerListener(this);
    }

    @Override
    public void onDestroyView() {
        if (presenterView != null) {
            presenterView.removeViewPagerListener(this);
        }
        super.onDestroyView();
    }

    @Override
    public void onPageScrolled(final int position,
                               final float positionOffset,
                               final int positionOffsetPixels) {
        setFabSize(Math.min(1f, Math.abs(1f - positionOffset * 2)));
    }

    @Override
    public void onPageSelected(final int position) {
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
    }
}