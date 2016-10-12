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

import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.flows.expansions.interactors.ExpansionDetailsInteractor;
import is.hello.sense.flows.expansions.ui.views.ExpansionDetailView;
import is.hello.sense.interactors.ConfigurationsInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
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
        presenterView.setConfigurationSelectionClickListener(ignore -> this.onConfigurationSelectionClicked());
        presenterView.setEnabledIconClickListener(ignore -> this.onEnabledIconClicked());
        presenterView.setEnabledSwitchClickListener(this::onEnableSwitchChanged);
    }

    @Override
    public void onRelease() {
        super.onRelease();
        if(updateStateSubscription != null){
            updateStateSubscription.unsubscribe();
        }
    }

    public void bindConfigurations(@NonNull final List<Configuration> configurations) {
        //todo
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
        presenterView.setActionButtonClickListener(this::handleActionButtonClicked);
    }

    private void presentError(final Throwable throwable) {
        //todo
    }

    private void onEnableSwitchChanged(final CompoundButton ignore, final boolean isEnabled) {
        updateState(isEnabled ? State.CONNECTED_ON : State.CONNECTED_OFF);
    }

    private void onEnabledIconClicked() {
        //todo
    }

    private void onConfigurationSelectionClicked() {
        //todo
    }

    public void updateState(@NonNull final State state){
        this.updateStateSubscription.unsubscribe();
        this.updateStateSubscription = bind(expansionDetailsInteractor.setState(state))
                .subscribe();
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
