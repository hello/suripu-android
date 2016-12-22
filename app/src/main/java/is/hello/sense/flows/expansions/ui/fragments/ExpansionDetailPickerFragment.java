package is.hello.sense.flows.expansions.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import java.util.ArrayList;

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
import is.hello.sense.ui.widget.util.Styles;
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
        setHasOptionsMenu(true);
        final Bundle arguments = getArguments();
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else if (arguments != null) {
            restoreState(arguments);
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.info_menu, menu);
        final MenuItem infoItem = menu.findItem(R.id.action_info);
        if(infoItem != null){
            Styles.tintMenuIcon(getActivity(), infoItem, R.color.white);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                this.getExpansionInfoDialogClickListener(expansionCategory).onClick(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle arguments = getArguments();
        if (arguments != null) {
            final long id = arguments.getLong(ARG_EXPANSION_ID, NO_ID);
            if (id == NO_ID) {
                cancelFlow();
                return;
            }
            expansionCategory = (Category) arguments.getSerializable(ARG_EXPANSION_CATEGORY);
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
        outState.putSerializable(ARG_EXPANSION_VALUE_RANGE, getCurrentExpansionValueRange());
    }

    public void restoreState(@NonNull final Bundle bundle) {
        isEnabled = bundle.getBoolean(ARG_EXPANSION_ENABLED_FOR_SMART_ALARM);
        initialValueRange = (ExpansionValueRange) bundle.getSerializable(ARG_EXPANSION_VALUE_RANGE);
    }

    public void bindConfigurations(@NonNull final ArrayList<Configuration> configs) {
        final Configuration selectedConfig = ConfigurationsInteractor.selectedConfiguration(configs);

        final String configName;
        if (selectedConfig == null) {
            presentConfigurationError(new IllegalStateException("no configurations available"));
            return;
        } else if (selectedConfig.isEmpty()) {
            configName = getString(R.string.expansions_select);
        } else {
            configName = selectedConfig.getName();
        }
        lastConfigurationsFetchFailed = false;
        presenterView.showConfigurationSuccess(configName, this::onConfigureClicked);
        presenterView.showExpansionRangePicker(expansionCategoryFormatter.getInitialValuePair(expansionCategory,
                                                                                              selectedConfig.getCapabilities(),
                                                                                              initialValueRange));

    }

    /**
     * currently assumes that the expansion is enabled, configured, and authenticated
     */
    public void bindExpansion(@Nullable final Expansion expansion) {
        if (expansion == null) {
            cancelFlow();
            return;
        }

        final UnitConverter unitConverter = expansionCategoryFormatter.getUnitConverter(expansionCategory);
        final ExpansionValueRange expansionValueRange = expansion.getValueRange();
        final int max = (int) Math.ceil(unitConverter.convert(expansionValueRange.max));
        final int min = (int) Math.ceil(unitConverter.convert(expansionValueRange.min));

        if(initialValueRange == null){
            initialValueRange = expansionCategoryFormatter.getIdealValueRange(expansionCategory,
                                                                              expansionValueRange);
        }

        presenterView.setConfigurationTypeText(expansion.getConfigurationType());
        presenterView.initExpansionRangePicker(min,
                                               max,
                                               expansionCategoryFormatter.getSuffix(expansion.getCategory())
                                              );

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

    /**
     * @return new {@link ExpansionValueRange} based on UI
     * or {@link ExpansionDetailPickerFragment#initialValueRange} if UI is gone.
     */
    public ExpansionValueRange getCurrentExpansionValueRange() {
        if (presenterView != null && expansionCategoryFormatter != null) {
            final UnitConverter unitConverter = expansionCategoryFormatter.getReverseUnitConverter(expansionCategory);
            final Pair<Integer, Integer> selectedValue = presenterView.getSelectedValuePair();
            final float convertedMinValue = unitConverter.convert((float) selectedValue.first);
            final float convertedMaxValue = unitConverter.convert((float) selectedValue.second);
            return new ExpansionValueRange(convertedMinValue, convertedMaxValue);
        } else {
            return initialValueRange;
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
        presenterView.hidePickerSpinner();
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

    /**
     * Because THERMOSTAT configurations have different capabilities they will differ in min max values
     * toggling from single picker to double picker with for example min = 15, max = 15
     * will break default 3 degrees difference if initialValueRange is not cleared.
     */
    private void onConfigureClicked(final View ignored) {
        if(expansionCategory == Category.LIGHT) {
            initialValueRange = getCurrentExpansionValueRange();
        } else {
            initialValueRange = null;
        }
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
        if (expansionDetailsInteractor.expansionSubject.hasValue()
                && configurationsInteractor.configSubject.hasValue()
                && !lastConfigurationsFetchFailed
                && initialValueRange != null) {
            final Intent intentWithExpansionAlarm = new Intent();
            final ExpansionAlarm expansionAlarm = new ExpansionAlarm(expansionDetailsInteractor.expansionSubject.getValue(),
                                                                     isEnabled);
            expansionAlarm.setExpansionRange(getCurrentExpansionValueRange());
            intentWithExpansionAlarm.putExtra(ExpansionValuePickerActivity.EXTRA_EXPANSION_ALARM, expansionAlarm);
            finishFlowWithResult(Activity.RESULT_OK, intentWithExpansionAlarm);
            return true;
        }
        return false;
    }
    //endregion
}
