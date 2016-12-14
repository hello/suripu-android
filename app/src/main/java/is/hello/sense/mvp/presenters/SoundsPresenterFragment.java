package is.hello.sense.mvp.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

import is.hello.sense.flows.home.util.SoundsViewPagerPresenterDelegate;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

//todo move to is.hello.sense.flows.home.ui.fragments and replace SoundsFragment
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.addViewPagerListener(this);
    }

    @Override
    public void onDestroyView() {
        if(presenterView != null){
            presenterView.removeViewPagerListener(this);
        }
        super.onDestroyView();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(final int position) {
        //todo update to support sleep sounds too
        setFabVisible(position == getStartingItemPosition());
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}