package is.hello.sense.flows.expansions.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.expansions.routers.ExpansionSettingsRouter;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.view.expansions.ExpansionsAuthView;
import is.hello.sense.ui.widget.CustomWebViewClient;

public class ExpansionsAuthFragment extends PresenterFragment<ExpansionsAuthView>
implements CustomWebViewClient.Listener{

    @Inject
    ApiSessionManager sessionManager;

    public static final String ARG_EXPANSION_ID = ExpansionsAuthFragment.class.getName() + "ARG_EXPANSION_ID";
    public static final String ARG_INIT_URL = ExpansionsAuthFragment.class.getName() + "ARG_INIT_URL";
    public static final String ARG_COMPLETE_URL = ExpansionsAuthFragment.class.getName() + "ARG_COMPLETE_URL";
    private long expansionId;
    private String initUrl;
    private String completeUrl;

    public static ExpansionsAuthFragment newInstance(final long expansionId,
                                                     @NonNull final String initialUrl,
                                                     @NonNull final String completionUrl) {

        final Bundle args = new Bundle();
        args.putLong(ARG_EXPANSION_ID, expansionId);
        args.putString(ARG_INIT_URL, initialUrl);
        args.putString(ARG_COMPLETE_URL, completionUrl);
        final ExpansionsAuthFragment fragment = new ExpansionsAuthFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        if(arguments != null){
            this.expansionId = arguments.getLong(ARG_EXPANSION_ID, Expansion.NO_ID);
            this.initUrl = arguments.getString(ARG_INIT_URL); //todo what should default urls be?
            this.completeUrl = arguments.getString(ARG_COMPLETE_URL);
        }
    }

    @Override
    public void initializePresenterView() {
        if(presenterView == null){
            final CustomWebViewClient client = new CustomWebViewClient(initUrl,
                                                                       completeUrl);
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
        if(sessionManager.hasSession()) {
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
        ((ExpansionSettingsRouter) getActivity()).showConfigurationSelection(this.expansionId);
    }

    @Override
    public void onOtherUrlLoaded(){
        presenterView.showProgress(false);
    }
}
