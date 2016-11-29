package is.hello.sense.mvp.presenters;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import is.hello.sense.mvp.util.ViewPagerPresenter;
import is.hello.sense.mvp.view.ViewPagerPresenterView;

public abstract class ViewPagerPresenterFragment extends PresenterFragment<ViewPagerPresenterView>
        implements SwipeRefreshLayout.OnRefreshListener,
        ViewPagerPresenter {

    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new ViewPagerPresenterView(this);
        }
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //presenterView.refreshView(true);
    }
}
