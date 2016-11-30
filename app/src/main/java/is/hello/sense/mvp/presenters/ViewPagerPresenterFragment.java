package is.hello.sense.mvp.presenters;


import is.hello.sense.mvp.util.ViewPagerPresenter;
import is.hello.sense.mvp.view.ViewPagerPresenterView;

/**
 * Any class Fragment that wants to host fragments should extend this.
 */
public abstract class ViewPagerPresenterFragment extends PresenterFragment<ViewPagerPresenterView>
        implements ViewPagerPresenter {

    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new ViewPagerPresenterView(this);
        }
    }
}
