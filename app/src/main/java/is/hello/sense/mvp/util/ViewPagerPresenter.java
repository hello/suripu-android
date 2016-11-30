package is.hello.sense.mvp.util;

import android.support.annotation.NonNull;

import is.hello.sense.ui.adapter.StaticFragmentAdapter;

public interface ViewPagerPresenter {
    @NonNull
    StaticFragmentAdapter.Item[] getViewPagerItems();
}
