package is.hello.sense.mvp.presenters;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;
import is.hello.sense.mvp.util.FabPresenter;
import is.hello.sense.mvp.util.ViewPagerPresenter;
import is.hello.sense.mvp.view.ViewPagerPresenterView;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.util.NotTested;

/**
 * Any class Fragment that wants to host fragments should extend this.
 */
@NotTested
public abstract class ViewPagerPresenterFragment extends ControllerPresenterFragment<ViewPagerPresenterView>
        implements ViewPagerPresenter,
        FabPresenter,
        HomeActivity.ScrollUp,
        StaticFragmentAdapter.Controller {

    private BaseViewPagerPresenterDelegate viewPagerDelegate;

    //region PresenterFragment
    @Override
    public final void initializePresenterView() {
        if (this.presenterView == null) {
            this.viewPagerDelegate = newViewPagerDelegateInstance();
            this.presenterView = new ViewPagerPresenterView(this);
        }
    }


    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getCurrentFragment() == null) {
            return;
        }
        getCurrentFragment().setUserVisibleHint(isVisibleToUser);
    }

    //endregion

    //region ViewPagePresenter
    @NonNull
    @Override
    public StaticFragmentAdapter.Item[] getViewPagerItems() {
        // should never happen but lets be safe.
        if (this.viewPagerDelegate == null) {
            return new StaticFragmentAdapter.Item[0];
        }
        return this.viewPagerDelegate.getViewPagerItems();
    }

    @Override
    public int getStartingItemPosition() {
        // should never happen but lets be safe.
        if (this.viewPagerDelegate == null) {
            return BaseViewPagerPresenterDelegate.DEFAULT_STARTING_ITEM_POSITION;
        }
        return this.viewPagerDelegate.getStartingItemPosition();
    }

    @Override
    public int getOffscreenPageLimit() {
        if (this.viewPagerDelegate == null) {
            return BaseViewPagerPresenterDelegate.DEFAULT_OFFSCREEN_PAGE_LIMIT;
        }
        return this.viewPagerDelegate.getOffscreenPageLimit();
    }
    //endregion

    //region scollup
    @Override
    public void scrollUp() {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof HomeActivity.ScrollUp) {
            ((HomeActivity.ScrollUp) fragment).scrollUp();
        }
    }
    //endregion

    //region methods

    /**
     * Override this with your own view pager delegate.
     *
     * @return the view pager delegate to be used.
     */
    @NonNull
    protected abstract BaseViewPagerPresenterDelegate newViewPagerDelegateInstance();

    /**
     * Override this to use a different fragment manager
     *
     * @return false for {@link #getFragmentManager()} or true for {@link #getChildFragmentManager()}
     */
    protected boolean useChildFragmentManager() {
        return true;
    }

    public FragmentManager getDesiredFragmentManager() {
        return useChildFragmentManager() ? getChildFragmentManager() : getFragmentManager();
    }

    @Nullable
    public Fragment getCurrentFragment() {
        if (this.presenterView == null) {
            return null;
        }
        return this.presenterView.getCurrentFragment();
    }


    //endregion

    //region FabPresenter
    //@Override
    public void setFabSize(final float size) {
        if (this.presenterView != null) {
            this.presenterView.setFabSize(size);
        }
    }

    @Override
    public void setFabVisible(final boolean visible) {
        if (this.presenterView != null) {
            this.presenterView.setFabVisible(visible);
        }
    }

    @Override
    public void updateFab(@DrawableRes final int iconRes,
                          @Nullable final View.OnClickListener listener) {
        if (this.presenterView != null) {
            this.presenterView.updateFab(iconRes,
                                         listener);
        }
    }

    @Override
    public void setFabLoading(final boolean loading) {
        if (this.presenterView != null) {
            this.presenterView.setFabLoading(loading);
        }
    }
    //endregion

    //region controller
    @Override
    public void setVisibleToUser(final boolean isVisible) {
        super.setVisibleToUser(isVisible);
        final StaticFragmentAdapter.Controller controller = getCurrentController();
        if (controller == null || !controller.hasPresenterView()) {
            return;
        }
        controller.setVisibleToUser(isVisible);
    }

    //region

    @Nullable
    private StaticFragmentAdapter.Controller getCurrentController() {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof StaticFragmentAdapter.Controller) {
            return (StaticFragmentAdapter.Controller) fragment;
        }
        return null;
    }

}
