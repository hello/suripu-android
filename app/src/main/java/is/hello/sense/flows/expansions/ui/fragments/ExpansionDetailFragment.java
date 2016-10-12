package is.hello.sense.flows.expansions.ui.fragments;

import android.os.Bundle;
import android.view.View;

import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import is.hello.sense.flows.expansions.ui.views.ExpansionDetailView;
import is.hello.sense.mvp.presenters.PresenterFragment;

public class ExpansionDetailFragment extends PresenterFragment<ExpansionDetailView> {

    @Inject
    Picasso picasso;

    @Override
    public void initializePresenterView() {
        if(presenterView == null){
            presenterView = new ExpansionDetailView(getActivity());
            presenterView.setActionButtonClickListener(this::handleActionButtonClicked);
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(savedInstanceState != null) {
            presenterView.loadExpansionIcon(picasso, "");
        }
    }

    private void handleActionButtonClicked(final View ignored) {

    }
}
