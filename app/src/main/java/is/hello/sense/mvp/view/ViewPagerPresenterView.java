package is.hello.sense.mvp.view;


import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;

import is.hello.sense.R;
import is.hello.sense.mvp.presenters.ViewPagerPresenterFragment;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;


@SuppressLint("ViewConstructor")
public final class ViewPagerPresenterView extends PresenterView
        implements ViewPager.OnPageChangeListener {

    private final ViewPager viewPager;
    private final TabLayout tabLayout;
    private final StaticFragmentAdapter adapter;
    private final OnPageSelected onPageSelected;

    /**
     * @param fragment       - Fragment providing initialization settings and callbacks.
     *                       Don't keep a reference to this.
     * @param onPageSelected - Will allow us to create a callback and release it without referencing
     *                       the fragment.
     */
    public ViewPagerPresenterView(@NonNull final ViewPagerPresenterFragment fragment,
                                  @NonNull final OnPageSelected onPageSelected) {
        super(fragment.getActivity());
        this.viewPager = (ViewPager) findViewById(R.id.view_view_pager_extended_view_pager);
        this.tabLayout = (TabLayout) findViewById(R.id.view_view_pager_tab_layout);

        final StaticFragmentAdapter.Item[] items = fragment.getViewPagerItems();

        // ViewPager
        this.onPageSelected = onPageSelected;
        this.adapter = new StaticFragmentAdapter(fragment.getChildFragmentManager(), items);
        this.viewPager.setAdapter(this.adapter);
        this.viewPager.addOnPageChangeListener(this);

        // TabLayout
        for (final StaticFragmentAdapter.Item item : items) {
            this.tabLayout.addTab(this.tabLayout.newTab().setText(item.getTitle()));
        }
        final TabLayout.Tab firstTab = this.tabLayout.getTabAt(fragment.getStartingItemPosition());
        if (firstTab != null) {
            firstTab.select();
        }
        this.tabLayout.setupWithViewPager(this.viewPager);
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
        this.viewPager.removeOnPageChangeListener(this);
    }

    @Override
    protected boolean useAppCompat() {
        return true;
    }
    //endregion

    //region OnPageChangeListener
    @Override
    public void onPageScrolled(final int position,
                               final float positionOffset,
                               final int positionOffsetPixels) {
        // do nothing
    }

    @Override
    public void onPageSelected(final int position) {
        onPageSelected.onPageSelected(position);
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        // do nothing
    }
    //endregion

    /**
     * Helper interface so we don't need to store a reference to the Fragment. Will also let us
     * release the page listener on release.
     */
    public interface OnPageSelected {
        void onPageSelected(final int position);
    }
}

