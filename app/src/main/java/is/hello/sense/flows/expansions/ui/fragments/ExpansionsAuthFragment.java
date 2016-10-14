package is.hello.sense.flows.expansions.ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.expansions.interactors.ExpansionDetailsInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.view.expansions.ExpansionsAuthView;
import is.hello.sense.ui.widget.CustomWebViewClient;

public class ExpansionsAuthFragment extends PresenterFragment<ExpansionsAuthView>
        implements CustomWebViewClient.Listener {

    @Inject
    ApiSessionManager sessionManager;
    @Inject
    ExpansionDetailsInteractor expansionDetailsInteractor;
    private Expansion expansion;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (expansionDetailsInteractor.expansionSubject.hasValue()) {
            this.expansion = expansionDetailsInteractor.expansionSubject.getValue();
        } else {
            cancelFlow();
        }
    }

    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            final CustomWebViewClient client = new CustomWebViewClient(expansion.getAuthUri(),
                                                                       expansion.getCompletionUri());
            client.setListener(this);
            presenterView = new ExpansionsAuthView(
                    getActivity(),
                    client
            );
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Map<String, String> headers = new HashMap<>(1);
        if (sessionManager.hasSession()) {
            headers.put("Authorization", "Bearer " + sessionManager.getAccessToken());
        }
        presenterView.loadlInitialUrl(headers);
        presenterView.showProgress(true);
    }

    @Override
    public void onInitialUrlLoaded() {
        presenterView.showProgress(false);
    }

    @Override
    public void onCompletionUrlLoaded() {
        presenterView.showProgress(false);
        finishFlow();
    }

    @Override
    public void onOtherUrlLoaded() {
        presenterView.showProgress(false);
    }
}
