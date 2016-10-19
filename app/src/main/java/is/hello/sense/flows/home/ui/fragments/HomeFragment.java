package is.hello.sense.flows.home.ui.fragments;

import is.hello.sense.flows.home.ui.views.HomeView;

public class HomeFragment extends BacksideTabFragment<HomeView> {
    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new HomeView(getActivity(),
                                         getFragmentManager());
        }
    }

    @Override
    protected void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {

    }
}
