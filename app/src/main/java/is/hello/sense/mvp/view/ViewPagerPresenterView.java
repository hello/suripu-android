package is.hello.sense.mvp.view;


import android.annotation.SuppressLint;
import android.app.Fragment;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.mvp.presenters.ViewPagerPresenterFragment;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.widget.ExtendedViewPager;


@SuppressLint("ViewConstructor")
public final class ViewPagerPresenterView extends PresenterView {

    private final ExtendedViewPager viewPager;
    private final TabLayout tabLayout;
    private final FloatingActionButton fab;

    /**
     * @param fragment - Fragment providing initialization settings and callbacks.
     *                 Don't keep a reference to this.
     */
    public ViewPagerPresenterView(@NonNull final ViewPagerPresenterFragment fragment) {
        super(fragment.getActivity());
        this.viewPager = (ExtendedViewPager) findViewById(R.id.view_view_pager_extended_view_pager);
        this.tabLayout = (TabLayout) findViewById(R.id.view_view_pager_tab_layout);
        this.tabLayout.setupWithViewPager(this.viewPager);
        this.fab = (FloatingActionButton) findViewById(R.id.view_view_pager_fab);
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
        this.tabLayout.removeAllTabs();
        this.viewPager.removeAllViews();
        this.fab.setOnClickListener(null);
    }
    //endregion

    //region methods

    public void createTabsAndPager(@NonNull final ViewPagerPresenterFragment fragment) {

        final StaticFragmentAdapter.Item[] items = fragment.getViewPagerItems();

        // ViewPager
        final StaticFragmentAdapter adapter =
                new StaticFragmentAdapter(fragment.getDesiredFragmentManager(),
                                          items);
        this.viewPager.setOffscreenPageLimit(2);
        this.viewPager.setAdapter(adapter);
        this.viewPager.setEnabled(true);

        // TabLayout
        tabLayout.removeAllTabs();
        for (final StaticFragmentAdapter.Item item : items) {
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

    public void addViewPagerListener(final ViewPager.OnPageChangeListener listener) {
        viewPager.addOnPageChangeListener(listener);
    }

    public void removeViewPagerListener(final ViewPager.OnPageChangeListener listener) {
        viewPager.removeOnPageChangeListener(listener);
    }

    public int getCurrentFragmentPosition() {
        return viewPager.getCurrentItem();
    }
    //endregion

    //region fab methods

    public void setFabSize(final float size) {
        if (size >= 0.5) {
            this.fab.setScaleX(size);
            this.fab.setScaleY(size);
            this.fab.setAlpha(Anime.interpolateFloats(size, 0, 2) - 1);
        } else {
            this.fab.setAlpha(0f);
        }
    }

    public void setFabVisible(final boolean visible) {
        if (visible) {
            this.fab.show();
        } else if (this.fab.getVisibility() == VISIBLE) {
            this.fab.hide();
        }
    }

    public void updateFab(final @DrawableRes int resource,
                          final @Nullable View.OnClickListener listener) {
        this.setFabLoading(false);
        fab.setOnClickListener(listener);
        fab.setImageResource(resource);
    }

    public void setFabLoading(final boolean loading) {
        fab.setClickable(!loading);
        fab.setLongClickable(!loading);
        fab.setFocusable(!loading);
        if (loading) {
            fab.setOnClickListener(null);
            fab.setImageResource(R.drawable.sound_loading_icon);
            final Animation animation = AnimationUtils.loadAnimation(context, R.anim.rotate_360);
            animation.setRepeatCount(Animation.INFINITE);
            fab.startAnimation(animation);
        } else {
            fab.clearAnimation();
        }
    }
    //endregion

}

