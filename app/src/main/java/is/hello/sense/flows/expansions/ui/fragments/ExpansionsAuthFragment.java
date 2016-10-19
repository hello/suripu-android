package is.hello.sense.flows.expansions.ui.fragments;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
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
import is.hello.sense.flows.expansions.ui.views.ExpansionsAuthView;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.CustomWebViewClient;

public class ExpansionsAuthFragment extends PresenterFragment<ExpansionsAuthView>
        implements CustomWebViewClient.Listener,
        OnBackPressedInterceptor {

    @Inject
    ApiSessionManager sessionManager;
    @Inject
    ExpansionDetailsInteractor expansionDetailsInteractor;
    private CustomWebViewClient client;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        setActionBarHomeAsUpIndicator(R.drawable.app_style_ab_cancel);
        addInteractor(expansionDetailsInteractor);
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
                    presenterView.showProgress(true);
                    return true;
                }
                default:
                    return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            this.client = new CustomWebViewClient(Expansion.INVALID_URL,
                                                  Expansion.INVALID_URL);
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
        bindAndSubscribe(expansionDetailsInteractor.expansionSubject,
                         this::bindLatestExpansion,
                         this::presentError);
        presenterView.showProgress(true);
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        setActionBarHomeAsUpIndicator(R.drawable.app_style_ab_up);
        if(client != null){
            client.setListener(null);
            client = null;
        }
    }

    //region CustomWebViewClient Listener

    @Override
    public void onInitialUrlLoaded() {
        presenterView.showProgress(false);
    }

    @Override
    public void onCompletionUrlStarted() {
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
        return presenterView != null && presenterView.loadPreviousUrl();
    }

    public void setActionBarHomeAsUpIndicator(@DrawableRes final int drawableRes) {
        final ActionBar actionBar = this.getActivity().getActionBar();
        if(actionBar != null) {
            this.getActivity().getActionBar().setHomeAsUpIndicator(drawableRes);
        }
    }

    @VisibleForTesting
    public void bindLatestExpansion(@Nullable final Expansion expansion) {
        if(expansion == null){
            cancelFlow();
            return;
        }
        client.setInitialUrl(expansion.getAuthUri());
        client.setCompletionUrl(expansion.getCompletionUri());
        final Map<String, String> headers = new HashMap<>(1);
        if (sessionManager.hasSession()) {
            headers.put("Authorization", "Bearer " + sessionManager.getAccessToken());
        }
        presenterView.loadInitialUrl(headers);
    }

    public void presentError(final Throwable e) {
        //todo handle better
        presenterView.showProgress(false);
        showErrorDialog(new ErrorDialogFragment.PresenterBuilder(e));
    }
}
