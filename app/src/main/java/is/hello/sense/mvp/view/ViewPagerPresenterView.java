package is.hello.sense.mvp.view;


import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;

import is.hello.sense.R;
import is.hello.sense.mvp.adapters.StaticSubPresenterFragmentAdapter;
import is.hello.sense.mvp.presenters.ViewPagerPresenterFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;


@SuppressLint("ViewConstructor")
public final class ViewPagerPresenterView extends PresenterView {

    private final ExtendedViewPager viewPager;
    private final TabLayout tabLayout;

    /**
     * @param fragment - Fragment providing initialization settings and callbacks.
     *                 Don't keep a reference to this.
     */
    public ViewPagerPresenterView(@NonNull final ViewPagerPresenterFragment fragment) {
        super(fragment.getActivity());
        this.viewPager = (ExtendedViewPager) findViewById(R.id.view_view_pager_extended_view_pager);
        this.tabLayout = (TabLayout) findViewById(R.id.view_view_pager_tab_layout);
        this.tabLayout.setupWithViewPager(this.viewPager);
        createTabsAndPager(fragment);
    }

    //region PresenterView
    @Override
    protected int getLayoutRes() {
        return R.layout.view_view_pager_view;
    }

    @Override
    public void releaseViews() {
        this.tabLayout.removeAllViews();
        this.viewPager.removeAllViews();
    }

    @Override
    protected boolean useAppCompat() {
        return true;
    }
    //endregion

    //region methods

    public void createTabsAndPager(@NonNull final ViewPagerPresenterFragment fragment) {
        final StaticSubPresenterFragmentAdapter.Item[] items = fragment.getViewPagerItems();

        // ViewPager
        final StaticSubPresenterFragmentAdapter adapter =
                new StaticSubPresenterFragmentAdapter(fragment.getDesiredFragmentManager(),
                                                      items);
        this.viewPager.setOffscreenPageLimit(0);
        this.viewPager.setAdapter(adapter);
        this.viewPager.setEnabled(true);

        // TabLayout
        tabLayout.removeAllTabs();
        for (final StaticSubPresenterFragmentAdapter.Item item : items) {
            this.tabLayout.addTab(this.tabLayout.newTab().setText(item.getTitle()));
        }
        final TabLayout.Tab firstTab = this.tabLayout.getTabAt(fragment.getStartingItemPosition());
        if (firstTab != null) {
            firstTab.select();
        }
        setTabLayoutVisible(true);
    }

    public void setTabLayoutVisible(final boolean visible) {
        tabLayout.setVisibility(visible ? VISIBLE : GONE);
    }

    public void lockViewPager(final int position) {
        viewPager.setScrollingEnabled(false);
        viewPager.setCurrentItem(position);
    }

    public void unlockViewPager() {
        viewPager.setScrollingEnabled(true);
    }

    public void removeTabs() {
        tabLayout.removeAllTabs();
        setTabLayoutVisible(false);
    }

    //endregion

}

