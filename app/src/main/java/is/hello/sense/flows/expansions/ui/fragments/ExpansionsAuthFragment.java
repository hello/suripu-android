package is.hello.sense.flows.expansions.ui.fragments;

import android.app.ActionBar;
import android.content.pm.ActivityInfo;
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
import is.hello.sense.flows.expansions.ui.views.ExpansionsAuthView;
import is.hello.sense.mvp.presenters.PresenterFragment;
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

        lockOrientation(true);
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
                if(senseView != null){
                    senseView.reloadCurrentUrl();
                    senseView.showProgress(true);
                    return true;
                }
                default:
                    return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void initializeSenseView() {
        if (senseView == null) {
            this.client = new CustomWebViewClient(Expansion.INVALID_URL,
                                                  Expansion.INVALID_URL);
            client.setListener(this);
            senseView = new ExpansionsAuthView(
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
        senseView.showProgress(true);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Call during onDestroy because onRelease is called twice which prevents orientation from being unlocked
        lockOrientation(false);
    }

    //region CustomWebViewClient Listener

    @Override
    public void onInitialUrlLoaded() {
        senseView.showProgress(false);
    }

    @Override
    public void onCompletionUrlStarted() {
        senseView.showProgress(false);
        showBlockingActivity(R.string.expansions_completing);
    }

    @Override
    public void onCompletionUrlFinished() {
        finishFlow();
    }

    @Override
    public void onResourceError() {
        hideBlockingActivity(false, null);
    }

    @Override
    public void onOtherUrlLoaded() {
        senseView.showProgress(false);
    }

    //end region

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        return senseView != null && senseView.loadPreviousUrl();
    }

    public void setActionBarHomeAsUpIndicator(@DrawableRes final int drawableRes) {
        final ActionBar actionBar = this.getActivity().getActionBar();
        if(actionBar != null) {
            this.getActivity().getActionBar().setHomeAsUpIndicator(drawableRes);
        }
    }

    private void lockOrientation(final boolean lock) {
        if(getActivity() != null){
            getActivity().setRequestedOrientation(
                    lock ? ActivityInfo.SCREEN_ORIENTATION_LOCKED : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                                                 );
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
        senseView.loadInitialUrl(headers);
    }

    public void presentError(final Throwable e) {
        //todo handle better
        senseView.showProgress(false);
        hideBlockingActivity(false, null);
        showErrorDialog(new ErrorDialogFragment.PresenterBuilder(e));
    }
}
