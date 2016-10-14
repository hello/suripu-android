package is.hello.sense.flows.expansions.ui.fragments;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.expansions.interactors.ExpansionDetailsInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.view.expansions.ExpansionsAuthView;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.widget.CustomWebViewClient;

public class ExpansionsAuthFragment extends PresenterFragment<ExpansionsAuthView>
        implements CustomWebViewClient.Listener,
        OnBackPressedInterceptor {

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
            return;
        }

        this.setHasOptionsMenu(true);
        setActionBarHomeAsUpIndicator(R.drawable.ic_close_white);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.expansion_auth_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                cancelFlow();
                return true;
            case R.id.expansions_auth_menu_item_refresh:
                if(presenterView != null){
                    presenterView.reloadCurrentUrl();
                    return true;
                }
                default:
                    return super.onOptionsItemSelected(item);
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
    protected void onRelease() {
        super.onRelease();
        setActionBarHomeAsUpIndicator(R.drawable.app_style_ab_up);
    }

    //region CustomWebViewClient Listener

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

    //end region

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        if(presenterView != null && presenterView.loadPreviousUrl()){
            return true;
        } else {
            defaultBehavior.run();
            return true;
        }
    }

    public void setActionBarHomeAsUpIndicator(@DrawableRes final int drawableRes) {
        final ActionBar actionBar = this.getActivity().getActionBar();
        if(actionBar != null) {
            this.getActivity().getActionBar().setHomeAsUpIndicator(drawableRes);
        }
    }
}
