package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.flows.home.ui.fragments.InsightsFragment;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.StateSafeExecutor;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

@SuppressLint("ViewConstructor")
public class HomeView extends PresenterView {
    private final StaticFragmentAdapter adapter;
    private final ExtendedViewPager pager;
    private final SelectorView subNavSelector;
    private final AnimatorContext animatorContext;
    private final StateSafeExecutor stateSafeExecutor;

    public HomeView(@NonNull final Activity activity,
                    @NonNull final FragmentManager fragmentManager,
                    @NonNull final AnimatorContext animatorContext,
                    @NonNull final StateSafeExecutor stateSafeExecutor) {
        super(activity);
        this.animatorContext = animatorContext;
        this.stateSafeExecutor = stateSafeExecutor;
        this.subNavSelector = (SelectorView) findViewById(R.id.fragment_home_sub_nav);
        this.pager = (ExtendedViewPager) findViewById(R.id.fragment_home_scrollview);
        this.pager.setScrollingEnabled(false);
        this.adapter = new StaticFragmentAdapter(fragmentManager,
                                                 new StaticFragmentAdapter.Item(InsightsFragment.class, getString(R.string.action_insights)),
                                                 new StaticFragmentAdapter.Item(InsightsFragment.class, getString(R.string.label_voice)));
        this.pager.setAdapter(adapter);
        this.pager.setFadePageTransformer(true);
        this.subNavSelector.setButtonLayoutParams(new SelectorView.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        this.subNavSelector.setBackground(new TabsBackgroundDrawable(this.context.getResources(),
                                                                     TabsBackgroundDrawable.Style.SUBNAV));
        this.subNavSelector.addOption(R.string.home_subnav_insights, false);
        this.subNavSelector.addOption(R.string.home_subnav_voice, false);
        this.subNavSelector.setTranslationY(0);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_home;
    }

    @Override
    public void resume() {
        super.resume();
        subNavSelector.setSelectedIndex(pager.getCurrentItem());
    }

    @Override
    public void releaseViews() {
    }

    public final boolean isShowingViews() {
        return pager != null && adapter != null && pager.getChildCount() == adapter.getCount();
    }

    public Fragment getCurrentFragment() {
        return adapter.getItem(pager.getCurrentItem());
    }

    public void showVoiceFragment(final boolean show) {
        if (show && subNavSelector.getVisibility() == View.GONE) {
            transitionInSubNavBar();
            adapter.setOverrideCount(-1);
            adapter.notifyDataSetChanged();
        } else if (!show && subNavSelector.getVisibility() == View.VISIBLE) {
            transitionOutSubNavBar();
            adapter.setOverrideCount(1);
            adapter.notifyDataSetChanged();
            subNavSelector.setSelectedIndex(0);
            pager.setCurrentItem(0);
            adapter.notifyDataSetChanged();
        }
    }


    private void transitionInSubNavBar() {
        if (subNavSelector == null) {
            return;
        }
        subNavSelector.setVisibility(View.INVISIBLE);
        Views.runWhenLaidOut(subNavSelector, stateSafeExecutor.bind(() -> {
            subNavSelector.setTranslationY(-subNavSelector.getMeasuredHeight());
            subNavSelector.setVisibility(View.VISIBLE);
            animatorFor(subNavSelector, animatorContext)
                    .translationY(0f)
                    .start();
        }));
    }

    private void transitionOutSubNavBar() {
        if (subNavSelector == null) {
            return;
        }
        animatorFor(subNavSelector, animatorContext)
                .translationY(-subNavSelector.getMeasuredHeight())
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        subNavSelector.setVisibility(View.GONE);
                    }
                })
                .start();
    }
}
