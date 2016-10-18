package is.hello.sense.flows.expansions.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CompoundButton;

import com.squareup.picasso.Picasso;

import java.util.List;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.flows.expansions.interactors.ConfigurationsInteractor;
import is.hello.sense.flows.expansions.interactors.ExpansionDetailsInteractor;
import is.hello.sense.flows.expansions.ui.views.ExpansionDetailView;
import is.hello.sense.functional.Functions;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import static is.hello.sense.api.model.v2.expansions.Expansion.NO_ID;

public class ExpansionDetailFragment extends PresenterFragment<ExpansionDetailView> {
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
            presenterView = new ExpansionDetailView(getActivity(),
                                                    this::onConnectClicked,
                                                    this::onEnabledIconClicked,
                                                    this::onRemoveAccessClicked,
                                                    this::onConfigureClicked,
                                                    this::onConfigurationErrorImageViewClicked,
                                                    this::onEnableSwitchChanged);
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

        bindAndSubscribe(expansionDetailsInteractor.expansionSubject,
                         this::bindExpansion,
                         this::presentError);

        bindAndSubscribe(configurationsInteractor.configSubject,
                         this::bindConfigurations,
                         this::presentConfigurationError);


    }

    @Override
    public void onRelease() {
        super.onRelease();
        if (updateStateSubscription != null) {
            updateStateSubscription.unsubscribe();
        }
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
            presenterView.showConfigurationSuccess(selectedConfig.getName());
        }
    }

    public void bindExpansion(@Nullable final Expansion expansion) {
        if (expansion == null) {
            return; //todo handle better
        }
        //todo update expansion enabled switch based on state
        presenterView.setExpansionInfo(expansion, picasso);
        if (expansion.requiresAuthentication()) {
            presenterView.showConnectButton();
            configurationsInteractor.update();//todo remove after selected config end point is ready
        } else if (expansion.requiresConfiguration()) {
            presenterView.showConfigurationSuccess(getString(R.string.action_connect));
        } else {
            configurationsInteractor.update(); //todo remove after selected config end point is ready
            presenterView.showEnabledSwitch(expansion.isConnected());
        }

    }

    private void presentConfigurationError(final Throwable throwable) {
        presenterView.showConfigurationsError();
        final ErrorDialogFragment.PresenterBuilder builder = new ErrorDialogFragment.PresenterBuilder(null);
        if (expansionDetailsInteractor.expansionSubject.hasValue()) {
            final Expansion expansion = expansionDetailsInteractor.expansionSubject.getValue();
            builder.withTitle(StringRef.from(getString(R.string.error_configurations_unavailable_title, expansion.getCategory().displayString)));
            builder.withMessage(StringRef.from(getString(R.string.error_configurations_unavailable_message, expansion.getCategory().displayString, expansion.getServiceName())));
        } else {
            builder.withTitle(R.string.error_configurations_unavailable_title_no_expansion);
            builder.withMessage(StringRef.from(getString(R.string.error_configurations_unavailable_message, Category.fromString(null).displayString, null)));
        }
        builder.withAction(200,R.string.label_having_trouble); //todo change result code and handle
        showErrorDialog(builder);

    }

    private void presentError(final Throwable throwable) {
        //todo show more generic error dialog
        showErrorDialog(ErrorDialogFragment.newInstance(throwable));
    }


    public void updateState(@NonNull final State state, @NonNull final Action1<Object> onNext) {
        this.updateStateSubscription.unsubscribe();
        this.updateStateSubscription = bind(expansionDetailsInteractor.setState(state))
                .subscribe(onNext,
                           this::presentError);
    }


    // region listeners
    private void onEnableSwitchChanged(final CompoundButton ignore, final boolean isEnabled) {
        updateState(isEnabled ? State.CONNECTED_ON : State.CONNECTED_OFF, Functions.NO_OP);
    }

    private void onRemoveAccessClicked(final View ignored) {
        final SenseAlertDialog.SerializedRunnable finishRunnable = () ->
                ExpansionDetailFragment.this.updateState(State.REVOKED, ignore -> finishFlow());
        showAlertDialog(new SenseAlertDialog.Builder().setTitle(R.string.are_you_sure)
                                                      .setMessage(R.string.expansion_detail_remove_access_dialog_message)
                                                      .setNegativeButton(R.string.action_cancel, null)
                                                      .setButtonDestructive(SenseAlertDialog.BUTTON_POSITIVE, true)
                                                      .setPositiveButton(R.string.action_delete, finishRunnable));
    }

    private void onConnectClicked(final View ignored) {
        //todo test when value is lost
        finishFlowWithResult(RESULT_ACTION_PRESSED);
    }

    private void onConfigureClicked(final View ignored) {
        finishFlowWithResult(RESULT_CONFIGURE_PRESSED);
    }

    private void onConfigurationErrorImageViewClicked(final View ignored) {
        presenterView.showConfigurationSpinner();
        configurationsInteractor.update();

    }

    private void onEnabledIconClicked(final View ignored) {
        WelcomeDialogFragment.show(getActivity(),
                                   R.xml.welcome_dialog_expansions,
                                   true);
    }
    //endregion
}
