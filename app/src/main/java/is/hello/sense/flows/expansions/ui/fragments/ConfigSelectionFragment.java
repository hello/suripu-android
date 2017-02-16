package is.hello.sense.flows.expansions.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.flows.expansions.interactors.ConfigurationsInteractor;
import is.hello.sense.flows.expansions.interactors.ExpansionDetailsInteractor;
import is.hello.sense.flows.expansions.ui.activities.ExpansionSettingsActivity;
import is.hello.sense.flows.expansions.ui.views.ConfigSelectionView;
import is.hello.sense.mvp.fragments.PresenterFragment;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.ConfigurationAdapter;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.util.Logger;

public class ConfigSelectionFragment extends PresenterFragment<ConfigSelectionView>
        implements ArrayRecyclerAdapter.OnItemClickedListener<Configuration>,
        OnBackPressedInterceptor {
    public static final String EXPANSION_ID_KEY = ConfigSelectionFragment.class.getSimpleName() + ".expansion_id_key";
    public static final String EXPANSION_CATEGORY = ConfigSelectionFragment.class.getSimpleName() + ".expansion_category";
    public static final int RESULT_CONFIG_SELECTED = 110;

    @Inject
    ConfigurationsInteractor configurationsInteractor;

    @Inject
    ExpansionDetailsInteractor expansionDetailsInteractor;

    private ConfigurationAdapter adapter;
    private Expansion expansion;

    @Override
    public void onAttach(@NonNull final Activity activity) {
        super.onAttach(activity);
        showLockedBlockingActivity(R.string.expansions_configuration_selection_loading);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(expansionDetailsInteractor);
        addInteractor(configurationsInteractor);
    }

    @Override
    public void initializeSenseView() {
        if (senseView == null) {
            this.adapter = new ConfigurationAdapter(new ArrayList<>(2));
            this.adapter.setOnItemClickedListener(this);
            senseView = new ConfigSelectionView(getActivity(), adapter);
            senseView.setDoneButtonClickListener(this::onDoneButtonClicked);
            senseView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        senseView.postDelayed(stateSafeExecutor.bind(() -> {
            senseView.setVisibility(View.VISIBLE);
            bindAndSubscribe(expansionDetailsInteractor.expansionSubject,
                             this::bindExpansion,
                             this::presentError);
            configurationsInteractor.configSubject.forget();
            bindAndSubscribe(configurationsInteractor.configSubject,
                             this::bindConfigurations,
                             this::presentError);

            if(!expansionDetailsInteractor.expansionSubject.hasValue()) {
                Logger.error(ConfigSelectionFragment.class.getName(), "expansion detail interactor had no value");
                bindConfigurations(null);
            }
        }), 2000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stateSafeExecutor.clearPending();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        if (this.adapter != null) {
            adapter.setOnItemClickedListener(null);
            adapter.clear();
            adapter = null;
        }
    }

    //region OnItemClickedListener

    @Override
    public void onItemClicked(final int position, @NonNull final Configuration item) {
        adapter.setSelectedItem(position);
    }

    //endregion

    //region OnBackPressedInterceptor

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        finishFlowWithExpansionDetailIntent(false);
        return true;
    }

    //endregion

    @VisibleForTesting
    public void bindExpansion(@Nullable final Expansion expansion) {
        if (expansion != null) {
            this.expansion = expansion;
            senseView.setTitle(getString(R.string.expansions_configuration_selection_title_format, expansion.getCompanyName()));
            senseView.setSubtitle(getString(R.string.expansions_configuration_selection_subtitle_format, expansion.getConfigurationType()));
            configurationsInteractor.setExpansionId(expansion.getId());

            if (expansion.requiresConfiguration() || expansion.isConnected()) {
                configurationsInteractor.update();
            } else {
                bindConfigurations(null); //show empty configuration
            }
        }else {
            cancelFlow();
        }
    }

    @VisibleForTesting
    public void bindConfigurations(@Nullable final List<Configuration> configurations) {
        if (configurations == null) {
            this.adapter.replaceAll(Collections.singletonList(getEmptyConfiguration(expansion)));
        } else {
            if (configurations.isEmpty()) {
                configurations.add(getEmptyConfiguration(expansion));
            }
            this.adapter.setSelectedItemFromList(configurations);
            this.adapter.replaceAll(configurations);
        }
        hideBlockingActivity(false, null);
    }

    private Configuration.Empty getEmptyConfiguration(@Nullable final Expansion expansion) {
        if (expansion != null) {
            return new Configuration.Empty(getString(R.string.expansions_configuration_selection_item_missing_title_format, expansion.getConfigurationType()),
                                           getString(R.string.expansions_configuration_selection_item_missing_subtitle_format, expansion.getCompanyName(), expansion.getConfigurationType()),
                                           R.drawable.icon_warning);
        } else {
            return new Configuration.Empty(getString(R.string.expansions_configuration_selection_empty_title_default),
                                           getString(R.string.expansions_configuration_selection_empty_subtitle_default),
                                           R.drawable.icon_warning);
        }
    }

    @VisibleForTesting
    public void bindConfigurationPostResponse(@NonNull final Configuration configuration) {
        hideBlockingActivity(R.string.expansions_configuration_selection_configured,
                             stateSafeExecutor.bind(() -> this.finishFlowWithExpansionDetailIntent(true)));

    }

    @VisibleForTesting
    public void presentError(@NonNull final Throwable e) {
        hideBlockingActivity(false, null);
        //todo
    }

    private void onDoneButtonClicked(final View ignored) {
        final Configuration selectedConfig = adapter.getSelectedItem();
        if (selectedConfig != null) {
            if (expansion != null) {
                showLockedBlockingActivity(getString(R.string.expansions_configuration_selection_setting_progress_format, expansion.getConfigurationType()));
            } else {
                showLockedBlockingActivity(R.string.expansions_configuration_selection_setting_progress_default);
            }
            bindAndSubscribe(configurationsInteractor.setConfiguration(selectedConfig),
                             this::bindConfigurationPostResponse,
                             this::presentError);
        } else {
            finishFlowWithExpansionDetailIntent(false);
        }
    }

    private void finishFlowWithExpansionDetailIntent(final boolean configSelected) {
        if (expansion != null) {
            finishFlowWithResult(configSelected ? RESULT_CONFIG_SELECTED : Activity.RESULT_OK,
                                 new Intent().putExtra(EXPANSION_ID_KEY, expansion.getId())
                                             .putExtra(EXPANSION_CATEGORY, expansion.getCategory())
                                             .putExtra(ExpansionSettingsActivity.EXTRA_EXPANSION, expansion)
                                );
        } else {
            finishFlow();
        }
    }
}
