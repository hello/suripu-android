package is.hello.sense.mvp.presenters;


import android.app.Fragment;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;
import is.hello.sense.mvp.util.ViewPagerPresenter;
import is.hello.sense.mvp.view.ViewPagerPresenterView;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.util.NotTested;

/**
 * Any class Fragment that wants to host fragments should extend this.
 */
@NotTested
public abstract class ViewPagerPresenterFragment extends PresenterFragment<ViewPagerPresenterView>
        implements ViewPagerPresenter {

    private int currentPosition = 0;

    private BaseViewPagerPresenterDelegate viewPagerDelegate;

    //region PresenterFragment
    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            viewPagerDelegate = newViewPagerDelegateInstance();
            currentPosition = viewPagerDelegate.getStartingItemPosition();
            presenterView = new ViewPagerPresenterView(this,
                                                       this::onPageSelected);
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
     * Called when {@link ViewPagerPresenterView#viewPager} selects a new page. This fragment will
     * track the currently selected page internall with {@link #currentPosition} and call
     * {@link SenseFragment#pauseFromViewPager()} for that Fragment before
     * {@link SenseFragment#resumeFromViewPager()} for the Fragment at the new position.
     *
     * @param position of new fragment within the view pager and fragment manager.
     */
    private void onPageSelected(final int position) {
        onPageUnselected(currentPosition);
        currentPosition = position;
        final Fragment fragment = getChildFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_view_pager_extended_view_pager + ":" + position);
        if (fragment instanceof SenseFragment) {
            ((SenseFragment) fragment).resumeFromViewPager();
        }
    }

    /**
     * Will call {@link SenseFragment#pauseFromViewPager()} for the Fragment at this position.
     *
     * @param position of new fragment withing view pager and fragment manager.
     */
    private void onPageUnselected(final int position) {
        final Fragment fragment = getChildFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_view_pager_extended_view_pager + ":" + position);
        if (fragment instanceof SenseFragment) {
            ((SenseFragment) fragment).pauseFromViewPager();
        }
    }

    /**
     * Override this with your own view pager delegate.
     *
     * @return the view pager delegate to be used.
     */
    @NonNull
    protected abstract BaseViewPagerPresenterDelegate newViewPagerDelegateInstance();
    //endregion
}
