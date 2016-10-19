package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.fragments.InsightsFragment;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.StaticFragmentAdapter; ;
import is.hello.sense.ui.widget.ExtendedViewPager;

@SuppressLint("ViewConstructor")
public class HomeView extends PresenterView {
    private final StaticFragmentAdapter adapter;
    private final ExtendedViewPager pager;

    public HomeView(@NonNull final Activity activity,
                    @NonNull final FragmentManager fragmentManager) {
        super(activity);
        this.pager = (ExtendedViewPager) findViewById(R.id.fragment_home_scrollview);
        pager.setScrollingEnabled(false);
        this.adapter = new StaticFragmentAdapter(fragmentManager,
                                                 new StaticFragmentAdapter.Item(InsightsFragment.class, getString(R.string.action_insights)));
        pager.setAdapter(adapter);
        pager.setFadePageTransformer(true);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_home;
    }

    @Override
    public void releaseViews() {

    }
}
