package is.hello.sense.flows.home.util;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.fragments.InsightsFragment;
import is.hello.sense.flows.home.ui.fragments.VoiceFragment;
import is.hello.sense.mvp.adapters.StaticSubPresenterFragmentAdapter;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

public class HomeViewPagerPresenterDelegate extends BaseViewPagerPresenterDelegate {
    private final Resources resources;

    public HomeViewPagerPresenterDelegate(@NonNull final Resources resources) {
        this.resources = resources;
    }

    @NonNull
    @Override
    public StaticSubPresenterFragmentAdapter.Item[] getViewPagerItems() {
        return new StaticSubPresenterFragmentAdapter.Item[]{
                new StaticSubPresenterFragmentAdapter.Item(InsightsFragment.class, resources.getString(R.string.action_insights)),
                new StaticSubPresenterFragmentAdapter.Item(VoiceFragment.class, resources.getString(R.string.label_voice))
        };
    }
}
