package is.hello.sense.ui.fragments.expansions;

import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.view.expansions.ExpansionListView;

public class ExpansionListFragment extends PresenterFragment<ExpansionListView> {
    @Override
    public void initializePresenterView() {
        if(presenterView == null){
            presenterView = new ExpansionListView(getActivity());
        }
    }
}
