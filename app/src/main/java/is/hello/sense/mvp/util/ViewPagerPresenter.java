package is.hello.sense.mvp.util;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.widget.SelectorView;

public interface ViewPagerPresenter {
    @NonNull
    StaticFragmentAdapter.Item[] getViewPagerItems();

    @NonNull
    SelectorView.Option[] getSelectorViewOptions();

    @NonNull
    Drawable getSelectorViewBackground();
}
