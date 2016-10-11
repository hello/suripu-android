package is.hello.sense.flows.expansions.ui.fragments;

import is.hello.sense.flows.expansions.ui.views.ExpansionDetailView;
import is.hello.sense.mvp.presenters.PresenterFragment;

public class ExpansionDetailFragment extends PresenterFragment<ExpansionDetailView> {
    @Override
    public void initializePresenterView() {
        if(presenterView == null){
            presenterView = new ExpansionDetailView(getActivity());
        }
    }
}
