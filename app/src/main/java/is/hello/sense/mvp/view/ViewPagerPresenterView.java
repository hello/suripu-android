package is.hello.sense.mvp.view;


import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;

import is.hello.sense.R;
import is.hello.sense.mvp.adapters.StaticSubPresenterFragmentAdapter;
import is.hello.sense.mvp.presenters.ViewPagerPresenterFragment;


@SuppressLint("ViewConstructor")
public final class ViewPagerPresenterView extends PresenterView {

    private final ViewPager viewPager;
    private final TabLayout tabLayout;
    private final StaticSubPresenterFragmentAdapter adapter;

    /**
     * @param fragment - Fragment providing initialization settings and callbacks.
     *                 Don't keep a reference to this.
     */
    public ViewPagerPresenterView(@NonNull final ViewPagerPresenterFragment fragment) {
        super(fragment.getActivity());
        this.viewPager = (ViewPager) findViewById(R.id.view_view_pager_extended_view_pager);
        this.tabLayout = (TabLayout) findViewById(R.id.view_view_pager_tab_layout);

        final StaticSubPresenterFragmentAdapter.Item[] items = fragment.getViewPagerItems();

        // ViewPager
        this.adapter = new StaticSubPresenterFragmentAdapter(fragment.getFragmentManager(), items);
        this.viewPager.setOffscreenPageLimit(0);
        this.viewPager.setAdapter(this.adapter);

        // TabLayout
        for (final StaticSubPresenterFragmentAdapter.Item item : items) {
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
    }

    @Override
    protected boolean useAppCompat() {
        return true;
    }
    //endregion
}

