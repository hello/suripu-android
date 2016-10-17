package is.hello.sense.flows.expansions.ui.fragments;

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
import is.hello.sense.flows.expansions.interactors.ConfigurationsInteractor;
import is.hello.sense.flows.expansions.interactors.ExpansionDetailsInteractor;
import is.hello.sense.flows.expansions.ui.views.ExpansionDetailView;
import is.hello.sense.functional.Functions;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import static is.hello.sense.api.model.v2.expansions.Expansion.NO_ID;

public class ExpansionDetailFragment extends PresenterFragment<ExpansionDetailView>
implements OnBackPressedInterceptor{
    public static final int RESULT_CONFIGURE_PRESSED = 100;
    public static final int RESULT_ACTION_PRESSED = 101;

    @Inject
    Picasso picasso;

    @Inject
    ExpansionDetailsInteractor expansionDetailsInteractor;

    @Inject
    ConfigurationsInteractor configurationsInteractor;

    private static final String ARG_EXPANSION_ID = ExpansionDetailFragment.class + "ARG_EXPANSION_ID";
    private Subscription updateStateSubscription;

    public static ExpansionDetailFragment newInstance(final long expansionId) {
        final ExpansionDetailFragment fragment = new ExpansionDetailFragment();
        final Bundle args = new Bundle();
        args.putLong(ARG_EXPANSION_ID, expansionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new ExpansionDetailView(getActivity());
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateStateSubscription = Subscriptions.empty();
        final Bundle arguments = getArguments();
        if (arguments != null) {
            final long id = arguments.getLong(ARG_EXPANSION_ID, NO_ID);
            if (id == NO_ID) {
                cancelFlow();
                return;
            }
            expansionDetailsInteractor.setId(id);
            configurationsInteractor.setExpansionId(id);
        } else {
            cancelFlow();
            return;
        }
        expansionDetailsInteractor.expansionSubject.forget();
        configurationsInteractor.configSubject.forget();
        expansionDetailsInteractor.update();
        configurationsInteractor.update();

        bindAndSubscribe(expansionDetailsInteractor.expansionSubject,
                         this::bindExpansion,
                         this::presentError);

        bindAndSubscribe(configurationsInteractor.configSubject,
                         this::bindConfigurations,
                         this::presentError);


        presenterView.setRemoveAccessClickListener(ignore -> this.onRemoveAccessClicked());
    }

    @Override
    public void onRelease() {
        super.onRelease();
        if (updateStateSubscription != null) {
            updateStateSubscription.unsubscribe();
        }
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        finishFlow();
        return true;
    }

    public void bindConfigurations(@Nullable final List<Configuration> configurations) {
        if (configurations == null) {
            return;
        }
        Configuration selectedConfig = null;
        for (int i = 0; i < configurations.size(); i++) {
            final Configuration config = configurations.get(i);
            if (config.isSelected()) {
                selectedConfig = config;
                break;
            }
        }
        //todo pass along selected config and list to move work to interactor
        if (selectedConfig != null) {
            presenterView.enableConfigurations(selectedConfig.getName(),
                                               ignore -> this.onConfigurationSelectionClicked());
        }
    }

    public void bindExpansion(@Nullable final Expansion expansion) {
        if (expansion == null) {
            return; //todo handle better
        }
        //todo update expansion enabled switch based on state
        presenterView.setExpansionInfo(expansion, picasso);
        if (expansion.requiresAuthentication()) {
            presenterView.enableConnectButton(this::handleActionButtonClicked);
        } else if (expansion.requiresConfiguration()) {
            presenterView.enableConfigurations(getString(R.string.action_connect),
                                               ignore -> finishFlowWithResult(RESULT_CONFIGURE_PRESSED));
        } else {
            presenterView.enableSwitch(expansion.isConnected(),
                                       this::onEnableSwitchChanged,
                                       ignore -> this.onEnabledIconClicked());
        }


    }

    private void presentError(final Throwable throwable) {
        //todo show more generic error dialog
        showErrorDialog(ErrorDialogFragment.newInstance(throwable));
    }

    private void onEnableSwitchChanged(final CompoundButton ignore, final boolean isEnabled) {
        updateState(isEnabled ? State.CONNECTED_ON : State.CONNECTED_OFF, Functions.NO_OP);
    }

    private void onEnabledIconClicked() {
        WelcomeDialogFragment.show(getActivity(),
                                   R.xml.welcome_dialog_expansions,
                                   true);
    }

    private void onConfigurationSelectionClicked() {
        //todo redirect to config select frag if nothing selected else open bottom sheet to select config
    }

    private void onRemoveAccessClicked() {
        final SenseAlertDialog.SerializedRunnable finishRunnable = () ->
                this.updateState(State.REVOKED, ignore -> finishFlow());
        showAlertDialog(new SenseAlertDialog.Builder().setTitle(R.string.are_you_sure)
                                                      .setMessage(R.string.expansion_detail_remove_access_dialog_message)
                                                      .setNegativeButton(R.string.action_cancel, null)
                                                      .setButtonDestructive(SenseAlertDialog.BUTTON_POSITIVE, true)
                                                      .setPositiveButton(R.string.action_delete, finishRunnable));
    }

    public void updateState(@NonNull final State state, @NonNull final Action1<Object> onNext) {
        this.updateStateSubscription.unsubscribe();
        this.updateStateSubscription = bind(expansionDetailsInteractor.setState(state))
                .subscribe(onNext,
                           this::presentError);
    }

    private void handleActionButtonClicked(final View ignored) {
        //todo test when value is lost
        finishFlowWithResult(RESULT_ACTION_PRESSED);
    }

}
