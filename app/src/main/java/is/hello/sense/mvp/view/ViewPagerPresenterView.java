package is.hello.sense.mvp.view;


import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TableLayout;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.mvp.presenters.ViewPagerPresenterFragment;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.util.Views;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

@SuppressLint("ViewConstructor")
public class ViewPagerPresenterView extends PresenterView
        implements SelectorView.OnSelectionChangedListener {

    private final ProgressBar progressBar;
    //private final SelectorView selectorView;
    private final ViewPager viewPager;
    private final SwipeRefreshLayout swipeRefreshLayout;
    private final StaticFragmentAdapter adapter;
    private final AnimatorContext animatorContext;
    private final TabLayout tableLayout;

    /**
     * @param fragment - Fragment providing initialization settings and callbacks.
     *                 Don't keep a reference to this.
     */
    public ViewPagerPresenterView(@NonNull final ViewPagerPresenterFragment fragment) {
        super(fragment.getActivity());
        this.progressBar = (ProgressBar) findViewById(R.id.view_view_pager_progress_bar);
        //this.selectorView = (SelectorView) findViewById(R.id.view_view_pager_selector_view);
        this.viewPager = (ViewPager) findViewById(R.id.view_view_pager_extended_view_pager);
        this.swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.view_view_pager_refresh_container);
        this.tableLayout = (TabLayout) findViewById(R.id.view_view_pager_tab_layout);
        //  Selector View
       /* this.selectorView.setButtonLayoutParams(new SelectorView.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        this.selectorView.setBackground(fragment.getSelectorViewBackground());
        this.selectorView.setOnSelectionChangedListener(this);
        this.selectorView.setTranslationY(0);
        for (final SelectorView.Option option : fragment.getSelectorViewOptions()) {
            selectorView.addOption(option);
        }*/

        // ViewPager
        this.adapter = new StaticFragmentAdapter(fragment.getFragmentManager(), fragment.getViewPagerItems());
        this.viewPager.setAdapter(this.adapter);


        //SwipeRefreshLayout
        this.swipeRefreshLayout.setOnRefreshListener(fragment::onRefresh);
        animatorContext = fragment.getAnimatorContext();
        progressBar.setVisibility(GONE);

        // ActionBar
        tableLayout.addTab(tableLayout.newTab().setText("ALARM"), true);
        tableLayout.addTab(tableLayout.newTab().setText("SOUNDS"));
        tableLayout.setupWithViewPager(viewPager);
    }

    //region PresenterView
    @Override
    protected int getLayoutRes() {
        return R.layout.view_view_pager_view;
    }

    @Override
    public void releaseViews() {
        // this.selectorView.setOnSelectionChangedListener(null);
        // this.selectorView.removeAllButtons();
        this.swipeRefreshLayout.setOnRefreshListener(null);
    }
    //endregion

    //region selectionChanged
    @Override
    public void onSelectionChanged(final int newSelectionIndex) {
        this.viewPager.setCurrentItem(newSelectionIndex);
    }
    //endregion
    //region methods
/*
    public void refreshView(final boolean show) {
        if (show) {
            transitionDown();
        } else {
            transitionUp();
        }
    }

    private void transitionDown() {
        selectorView.setVisibility(View.INVISIBLE);
        Views.runWhenLaidOut(selectorView, () -> post(() -> {
            selectorView.setTranslationY(-selectorView.getMeasuredHeight());
            selectorView.setVisibility(View.VISIBLE);
            animatorFor(selectorView, animatorContext)
                    .translationY(0f)
                    .start();
        }));
    }

    private void transitionUp() {
        animatorFor(selectorView, animatorContext)
                .translationY(-selectorView.getMeasuredHeight())
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        selectorView.setVisibility(View.GONE);
                    }
                })
                .start();
    }*/
    //endregion

}
