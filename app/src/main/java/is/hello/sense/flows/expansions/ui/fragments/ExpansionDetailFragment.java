package is.hello.sense.flows.expansions.ui.fragments;

import android.content.Intent;
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
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.model.v2.expansions.ExpansionValueRange;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.flows.expansions.interactors.ConfigurationsInteractor;
import is.hello.sense.flows.expansions.interactors.ExpansionDetailsInteractor;
import is.hello.sense.flows.expansions.ui.views.ExpansionDetailView;
import is.hello.sense.flows.expansions.utils.ExpansionCategoryFormatter;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import static is.hello.sense.api.model.v2.expansions.Expansion.NO_ID;

public class ExpansionDetailFragment extends PresenterFragment<ExpansionDetailView>
        implements CompoundButton.OnCheckedChangeListener {
    public static final int RESULT_CONFIGURE_PRESSED = 100;
    public static final int RESULT_ACTION_PRESSED = 101;
    private static final int REQUEST_CODE_UPDATE_STATE_ERROR = 102;
    private static final int RESULT_HELP_PRESSED = 103;

    @Inject
    Picasso picasso;

    @Inject
    ExpansionDetailsInteractor expansionDetailsInteractor;

    @Inject
    ConfigurationsInteractor configurationsInteractor; // todo remove when selected config end point is ready.

    @Inject
    ExpansionCategoryFormatter expansionCategoryFormatter;

    private static final String ARG_EXPANSION_ID = ExpansionDetailFragment.class + "ARG_EXPANSION_ID";
    private static final String ARG_VALUE_PICKER = ExpansionDetailFragment.class + "ARG_VALUE_PICKER";
    private Subscription updateStateSubscription;
    /**
     * Tracks when fetching configurations fails to show the dialog at the right time.
     */
    private boolean lastConfigurationsFetchFailed = false;
    private boolean wantsValuePicker = false;

    public static ExpansionDetailFragment newInstance(final long expansionId) {
        final ExpansionDetailFragment fragment = new ExpansionDetailFragment();
        final Bundle args = new Bundle();
        args.putLong(ARG_EXPANSION_ID, expansionId);
        fragment.setArguments(args);
        return fragment;
    }

    public static ExpansionDetailFragment newValuePickerInstance(final long expansionId) {
        final ExpansionDetailFragment fragment = new ExpansionDetailFragment();
        final Bundle args = new Bundle();
        args.putLong(ARG_EXPANSION_ID, expansionId);
        args.putBoolean(ARG_VALUE_PICKER, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new ExpansionDetailView(getActivity(),
                                                    this::onEnabledIconClicked,
                                                    this::onRemoveAccessClicked);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        if (arguments != null) {
            wantsValuePicker = arguments.getBoolean(ARG_VALUE_PICKER, false);
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
                         this::presentExpansionError);

        bindAndSubscribe(configurationsInteractor.configSubject,
                         this::bindConfigurations,
                         this::presentConfigurationError);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_UPDATE_STATE_ERROR && resultCode == RESULT_HELP_PRESSED) {
            UserSupport.showUserGuide(getActivity());
        }
    }

    @Override
    public void onRelease() {
        super.onRelease();
        if (updateStateSubscription != null) {
            updateStateSubscription.unsubscribe();
        }
    }

    public void updateState(@NonNull final State state, @NonNull final Action1<Object> onNext) {
        this.updateStateSubscription.unsubscribe();
        this.updateStateSubscription = bind(expansionDetailsInteractor.setState(state))
                .subscribe(onNext,
                           this::presentUpdateStateError);
    }

    public void bindConfigurations(@Nullable final List<Configuration> configurations) {
        lastConfigurationsFetchFailed = false;
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
            presenterView.showConfigurationSuccess(selectedConfig.getName(), this::onConfigureClicked);
        }
    }

    public void bindExpansion(@Nullable final Expansion expansion) {
        if (expansion == null) {
            cancelFlow();
            return;
        }
        if(wantsValuePicker) {
            final ExpansionValueRange expansionValueRange = expansion.getValueRange();
            presenterView.showExpansionValuePicker(expansionValueRange,
                                                   expansionValueRange.max - expansionValueRange.min, //initial value is midpoint
                                                   expansionCategoryFormatter.getSuffix(expansion.getCategory()));
        } else {
            presenterView.showExpansionInfo(expansion, picasso);
        }
        if (expansion.requiresAuthentication()) {
            presenterView.showConnectButton(this::onConnectClicked);
        } else if (expansion.requiresConfiguration()) {
            presenterView.showConfigurationSuccess(getString(R.string.action_connect), this::onConfigureClicked);
        } else {
            configurationsInteractor.update(); //todo remove after selected config end point is ready
            presenterView.showEnableSwitch(expansion.isConnected(), this);
        }

    }

    //region errors

    /**
     * Error shown when fetching expansions fails. Will close fragment since there is nothing to
     * show and we don't have a retry option.
     *
     * @param throwable -
     */
    private void presentExpansionError(final Throwable throwable) {
        if (ApiException.isNetworkError(throwable)) {
            showErrorDialog(new ErrorDialogFragment.PresenterBuilder(throwable));
        } else {
            showErrorDialog(ErrorDialogFragment.newInstance(throwable));
        }
        cancelFlow();
    }

    /**
     * Error shown when fetching selected configuration. Will tell the view to display the error
     * image which on press will allow retry.
     *
     * @param throwable -
     */
    private void presentConfigurationError(final Throwable throwable) {
        presenterView.showConfigurationsError(this::onConfigurationErrorImageViewClicked);
        if (ApiException.isNetworkError(throwable)) {
            showErrorDialog(new ErrorDialogFragment.PresenterBuilder(throwable));
        } else {
            if (lastConfigurationsFetchFailed) {
                final ErrorDialogFragment.PresenterBuilder builder = new ErrorDialogFragment.PresenterBuilder(null);
                if (expansionDetailsInteractor.expansionSubject.hasValue()) {
                    final Expansion expansion = expansionDetailsInteractor.expansionSubject.getValue();
                    builder.withTitle(StringRef.from(getString(R.string.error_configurations_unavailable_title, expansion.getCategory().displayString)));
                    builder.withMessage(StringRef.from(getString(R.string.error_configurations_unavailable_message, expansion.getCategory().displayString, expansion.getServiceName())));
                } else {
                    builder.withTitle(R.string.error_configurations_unavailable_title_no_expansion);
                    builder.withMessage(StringRef.from(getString(R.string.error_configurations_unavailable_message, Category.fromString(null).displayString, null)));
                }
                builder.withAction(RESULT_HELP_PRESSED, R.string.label_having_trouble);
                showErrorDialog(builder, REQUEST_CODE_UPDATE_STATE_ERROR);
            }
        }
        lastConfigurationsFetchFailed = true;
    }

    /**
     * Error shown when turning the expansion on/off. Just show a dialog and revert selection.
     *
     * @param throwable -
     */
    private void presentUpdateStateError(final Throwable throwable) {
        hideBlockingActivity(false, () -> {
            this.presenterView.showUpdateSwitchError(this);
            final ErrorDialogFragment.PresenterBuilder builder = ErrorDialogFragment.newInstance(throwable);
            builder.withTitle(R.string.expansion_detail_error_dialog_title)
                   .withMessage(StringRef.from(R.string.expansion_detail_error_dialog_message))
                   .withAction(RESULT_HELP_PRESSED, R.string.label_having_trouble);
            showErrorDialog(builder, REQUEST_CODE_UPDATE_STATE_ERROR);
        });

    }
    //endregion

    // region listeners
    private void onRemoveAccessClicked(final View ignored) {
        final SenseAlertDialog.SerializedRunnable finishRunnable = () ->
                ExpansionDetailFragment.this.updateState(State.REVOKED, (ignored2) ->
                        hideBlockingActivity(true, this::finishFlow));
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

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        showBlockingActivity(isChecked ? R.string.enabling_expansion : R.string.disabling_expansion);
        updateState(isChecked ? State.CONNECTED_ON : State.CONNECTED_OFF, (ignored) ->
                hideBlockingActivity(true, ExpansionDetailFragment.this.presenterView::showUpdateSwitchSuccess));

    }
    //endregion
}
