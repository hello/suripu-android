package is.hello.sense.flows.expansions.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CompoundButton;

import java.util.List;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.model.v2.expansions.ExpansionAlarm;
import is.hello.sense.api.model.v2.expansions.ExpansionValueRange;
import is.hello.sense.flows.expansions.interactors.ConfigurationsInteractor;
import is.hello.sense.flows.expansions.interactors.ExpansionDetailsInteractor;
import is.hello.sense.flows.expansions.ui.activities.ExpansionValuePickerActivity;
import is.hello.sense.flows.expansions.ui.views.ExpansionDetailPickerView;
import is.hello.sense.flows.expansions.utils.ExpansionCategoryFormatter;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.units.UnitConverter;
import is.hello.sense.util.Logger;

import static is.hello.sense.api.model.v2.expansions.Expansion.NO_ID;

public class ExpansionDetailPickerFragment extends PresenterFragment<ExpansionDetailPickerView>
        implements CompoundButton.OnCheckedChangeListener,
        OnBackPressedInterceptor {
    public static final int RESULT_CONFIGURE_PRESSED = 100;
    private static final int REQUEST_CODE_UPDATE_STATE_ERROR = 102;
    private static final int RESULT_HELP_PRESSED = 103;

    @Inject
    ExpansionDetailsInteractor expansionDetailsInteractor;

    @Inject
    ConfigurationsInteractor configurationsInteractor; // todo remove when selected config end point is ready.

    @Inject
    ExpansionCategoryFormatter expansionCategoryFormatter;

    private static final String ARG_EXPANSION_ID = ExpansionDetailPickerFragment.class.getSimpleName() + "ARG_EXPANSION_ID";
    private static final String ARG_EXPANSION_CATEGORY = ExpansionDetailPickerFragment.class.getSimpleName() + "ARG_EXPANSION_CATEGORY";
    private static final String ARG_EXPANSION_VALUE_RANGE = ExpansionDetailPickerFragment.class.getSimpleName() + "ARG_EXPANSION_VALUE_RANGE";
    private static final String ARG_EXPANSION_ENABLED_FOR_SMART_ALARM = ExpansionDetailPickerFragment.class.getSimpleName() + "ARG_EXPANSION_ENABLED_FOR_SMART_ALARM";
    /**
     * Tracks when fetching configurations fails to show the dialog at the right time.
     */
    private boolean lastConfigurationsFetchFailed = false;
    private boolean isEnabled;
    //used by value picker for inflating # pickers and setting up
    private ExpansionValueRange initialValueRange;
    private Category expansionCategory;

    public static ExpansionDetailPickerFragment newInstance(final long expansionId,
                                                            @NonNull final Category category,
                                                            @Nullable final ExpansionValueRange valueRange,
                                                            final boolean enabledForSmartAlarm) {
        final ExpansionDetailPickerFragment fragment = new ExpansionDetailPickerFragment();
        final Bundle args = new Bundle();
        args.putLong(ARG_EXPANSION_ID, expansionId);
        args.putSerializable(ARG_EXPANSION_CATEGORY, category);
        args.putSerializable(ARG_EXPANSION_VALUE_RANGE, valueRange);
        args.putBoolean(ARG_EXPANSION_ENABLED_FOR_SMART_ALARM, enabledForSmartAlarm);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new ExpansionDetailPickerView(getActivity());
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        if (arguments != null) {
            isEnabled = arguments.getBoolean(ARG_EXPANSION_ENABLED_FOR_SMART_ALARM, false);
            expansionCategory = (Category) arguments.getSerializable(ARG_EXPANSION_CATEGORY);
            initialValueRange = (ExpansionValueRange) arguments.getSerializable(ARG_EXPANSION_VALUE_RANGE);
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            isEnabled = savedInstanceState.getBoolean(ARG_EXPANSION_ENABLED_FOR_SMART_ALARM);
        }
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

        bindAndSubscribe(expansionDetailsInteractor.expansionSubject,
                         this::bindExpansion,
                         this::presentExpansionError);

        bindAndSubscribe(configurationsInteractor.configSubject,
                         this::bindConfigurations,
                         this::presentConfigurationError);

        // never call update for observable before subscribing
        expansionDetailsInteractor.update();
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
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_EXPANSION_ENABLED_FOR_SMART_ALARM, isEnabled);
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
        final String configName;
        if (selectedConfig == null) {
            configName = getString(R.string.expansions_select);
        } else {
            configName = selectedConfig.getName();
        }
        presenterView.showConfigurationSuccess(configName, this::onConfigureClicked);
    }

    public void bindExpansion(@Nullable final Expansion expansion) {
        if (expansion == null) {
            cancelFlow();
            return;
        }

        //todo currently assumes that the expansion is enabled, configured, and authenticated

        //todo need to wait for configurations to indicate what capabilities are available to determine number of pickers
        final UnitConverter unitConverter = expansionCategoryFormatter.getUnitConverter(expansionCategory);
        final int max = (int) Math.ceil(unitConverter.convert(expansion.getValueRange().max));
        final int min = (int) Math.ceil(unitConverter.convert(expansion.getValueRange().min));
        final float defaultValue = expansion.getValueRange().max - expansion.getValueRange().min;
        final int initialValues = unitConverter.convert((initialValueRange != null ? initialValueRange.max : defaultValue))
                                               .intValue();
        presenterView.showExpansionRangePicker(min,
                                               max,
                                               initialValues,
                                               expansionCategoryFormatter.getSuffix(expansion.getCategory()),
                                               expansion.getConfigurationType());


        presenterView.setExpansionEnabledTextViewClickListener(this.getExpansionInfoDialogClickListener(expansion.getCategory()));


        presenterView.showConnectedContainer(!expansion.requiresAuthentication());

        if (expansion.requiresAuthentication()) {
            //todo handle
            Logger.debug(ExpansionDetailPickerFragment.class.getName(), "expansion that requires auth returned in picker view");
        } else if (expansion.requiresConfiguration()) {
            presenterView.showConfigurationSuccess(getString(R.string.expansions_select), this::onConfigureClicked);
        } else {
            configurationsInteractor.update();
            presenterView.showEnableSwitch(isEnabled, this);
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
                    builder.withMessage(StringRef.from(getString(R.string.error_configurations_unavailable_message, expansion.getCategory().displayString, expansion.getCompanyName())));
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

    //endregion

    // region listeners

    private void onConfigureClicked(final View ignored) {
        finishFlowWithResult(RESULT_CONFIGURE_PRESSED);
    }

    private void onConfigurationErrorImageViewClicked(final View ignored) {
        presenterView.showConfigurationSpinner();
        configurationsInteractor.update();

    }

    private View.OnClickListener getExpansionInfoDialogClickListener(@NonNull final Category category) {
        final int xmlResId = expansionCategoryFormatter.getExpansionAlarmInfoDialogXmlRes(category);
        return ignoredView -> WelcomeDialogFragment.show(getActivity(),
                                                         xmlResId,
                                                         true);
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        isEnabled = isChecked;
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        if (expansionDetailsInteractor.expansionSubject.hasValue()) {
            final Intent intentWithExpansionAlarm = new Intent();
            final ExpansionAlarm expansionAlarm = new ExpansionAlarm(expansionDetailsInteractor.expansionSubject.getValue(),
                                                                     isEnabled);
            final UnitConverter unitConverter = expansionCategoryFormatter.getReverseUnitConverter(expansionCategory);
            final int selectedValue = presenterView.getSelectedValue();
            final float convertedValue = unitConverter.convert((float) selectedValue);
            expansionAlarm.setExpansionRange(convertedValue);
            intentWithExpansionAlarm.putExtra(ExpansionValuePickerActivity.EXTRA_EXPANSION_ALARM, expansionAlarm);
            finishFlowWithResult(Activity.RESULT_OK, intentWithExpansionAlarm);
            return true;
        }
        return false;
    }
    //endregion
}
