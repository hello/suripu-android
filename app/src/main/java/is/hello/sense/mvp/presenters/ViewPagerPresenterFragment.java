package is.hello.sense.mvp.presenters;


import android.app.FragmentManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

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
public abstract class ViewPagerPresenterFragment extends PresenterFragment<ViewPagerPresenterView>
        implements ViewPagerPresenter, FabPresenter {

    private BaseViewPagerPresenterDelegate viewPagerDelegate;

    //region PresenterFragment
    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            viewPagerDelegate = newViewPagerDelegateInstance();
            presenterView = new ViewPagerPresenterView(this);
        }
    }
    //endregion

    //region ViewPagePresenter
    @NonNull
    @Override
    public StaticFragmentAdapter.Item[] getViewPagerItems() {
        // should never happen but lets be safe.
        if (viewPagerDelegate == null) {
            return new StaticFragmentAdapter.Item[0];
        }
        return viewPagerDelegate.getViewPagerItems();
    }

    @Override
    public int getStartingItemPosition() {
        // should never happen but lets be safe.
        if (viewPagerDelegate == null) {
            return 0;
        }
        return viewPagerDelegate.getStartingItemPosition();
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
    //endregion

    //region FabPresenter
    @Override
    public void setFabVisible(final boolean visible){
        if(presenterView != null){
            presenterView.setFabVisible(visible);
        }
    }

    @Override
    public void updateFab(@DrawableRes final int iconRes,
                   @Nullable final View.OnClickListener listener){
        if(presenterView != null){
            presenterView.updateFab(iconRes,
                                    listener);
        }
    }

    @Override
    public void setFabLoading(final boolean loading){
        if(presenterView != null){
            presenterView.setFabLoading(loading);
        }
    }
    //endregion
}
