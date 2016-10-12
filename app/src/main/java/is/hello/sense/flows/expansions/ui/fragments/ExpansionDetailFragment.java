package is.hello.sense.flows.expansions.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CompoundButton;

import com.squareup.picasso.Picasso;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.flows.expansions.interactors.ExpansionDetailsInteractor;
import is.hello.sense.flows.expansions.ui.views.ExpansionDetailView;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.ConfigurationsInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class ExpansionDetailFragment extends PresenterFragment<ExpansionDetailView> {

    @Inject
    Picasso picasso;

    @Inject
    ExpansionDetailsInteractor expansionDetailsInteractor;

    @Inject
    ConfigurationsInteractor configurationsInteractor;

    public static String EXTRA_EXPANSION = ExpansionDetailFragment.class + "EXTRA_EXPANSION";
    private static final String ARG_EXPANSION = ExpansionDetailFragment.class + "ARG_EXPANSION";
    private Subscription updateStateSubscription;

    public static ExpansionDetailFragment newInstance(@NonNull final Expansion expansion) {
        final ExpansionDetailFragment fragment = new ExpansionDetailFragment();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_EXPANSION, expansion);
        fragment.setArguments(args);
        return fragment;
    }

    public static Intent newIntent(@NonNull final Expansion expansion) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_EXPANSION, expansion);
        return intent;
    }

    @Override
    public void initializePresenterView() {
        if(presenterView == null){
            presenterView = new ExpansionDetailView(getActivity());
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateStateSubscription = Subscriptions.empty();
        handleArgs(getArguments());
        bindAndSubscribe(expansionDetailsInteractor.expansionSubject,
                         this::bindExpansion,
                         this::presentError);
        bindAndSubscribe(configurationsInteractor.configSubject,
                         this::bindConfigurations,
                         this::presentError);
        expansionDetailsInteractor.update();
        configurationsInteractor.update();

        presenterView.setRemoveAccessClickListener(ignore -> this.updateState(State.REVOKED));
    }

    @Override
    public void onRelease() {
        super.onRelease();
        if(updateStateSubscription != null){
            updateStateSubscription.unsubscribe();
        }
    }

    public void bindConfigurations(@Nullable final List<Configuration> configurations) {
        if(configurations == null){
            return;
        }
        Configuration selectedConfig = null;
        for (int i = 0; i < configurations.size(); i++) {
            final Configuration config = configurations.get(i);
            if(config.isSelected()){
                presenterView.setConnectedContainerVisibility(true);
                selectedConfig = config;
                break;
            }
        }
        //todo pass along selected config and list to move work to interactor
        if(selectedConfig != null) {
            presenterView.setConfigurationSelectionText(selectedConfig.getName());
            presenterView.setConfigurationSelectionClickListener(ignore -> this.onConfigurationSelectionClicked());
        }
    }

    public void bindExpansion(@Nullable final Expansion expansion) {
        if(expansion == null){
            return; //todo handle better
        }
        //todo update expansion enabled switch based on state
        presenterView.setTitle(expansion.getDeviceName());
        presenterView.setSubtitle(expansion.getServiceName());
        presenterView.loadExpansionIcon(picasso, expansion.getIcon()
                                                          .getUrl(getResources()));
        presenterView.setDescription(expansion.getDescription());


        presenterView.setConfigurationType(expansion.getConfigurationType());
        if(expansion.requiresAuthentication()){
            presenterView.setConnectButtonClickListener(this::handleActionButtonClicked);
            presenterView.setConnectButtonVisibility(true);
        } else {
            if(expansion.requiresConfiguration()){
                presenterView.setConfigurationSelectionText(getString(R.string.action_connect));
                presenterView.setConfigurationSelectionClickListener(ignore -> this.redirectToConfigSelection());
            }
            presenterView.setEnabledContainerVisibility(true);
            presenterView.setEnabledSwitchOn(expansion.isConnected());
            presenterView.setEnabledSwitchClickListener(this::onEnableSwitchChanged);
            presenterView.setEnabledIconClickListener(ignore -> this.onEnabledIconClicked());
        }


    }

    private void presentError(final Throwable throwable) {
        //todo show more generic error dialog
        showErrorDialog(ErrorDialogFragment.newInstance(throwable));
    }

    private void onEnableSwitchChanged(final CompoundButton ignore, final boolean isEnabled) {
        updateState(isEnabled ? State.CONNECTED_ON : State.CONNECTED_OFF);
    }

    private void onEnabledIconClicked() {
        //todo show info dialog
    }

    private void redirectToConfigSelection() {
        //todo check nullable
        finishFlowWithResult(Activity.RESULT_OK,
                             ConfigSelectionFragment.newIntent(expansionDetailsInteractor.expansionSubject.getValue()));
    }

    private void onConfigurationSelectionClicked() {
        //todo redirect to config select frag if nothing selected else open bottom sheet to select config
    }

    public void updateState(@NonNull final State state){
        this.updateStateSubscription.unsubscribe();
        this.updateStateSubscription = bind(expansionDetailsInteractor.setState(state))
                .subscribe(Functions.NO_OP,
                           Functions.LOG_ERROR);
    }

    private void handleActionButtonClicked(final View ignored) {
        final Expansion expansion = expansionDetailsInteractor.expansionSubject.getValue();
        finishFlowWithResult(Activity.RESULT_OK, ExpansionsAuthFragment.newIntent(expansion.getAuthUri(),
                                                                                  expansion.getCompletionUri()));
    }

    private void handleArgs(@Nullable final Bundle arguments) {
        if(arguments != null){
            final Expansion expansion = (Expansion) arguments.getSerializable(ARG_EXPANSION);
            if(expansion != null) {
                expansionDetailsInteractor.setId(expansion.getId());
                configurationsInteractor.setExpansionId(expansion.getId());
            }
        }
    }
}
