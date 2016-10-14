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
import is.hello.sense.flows.expansions.ui.views.ConfigSelectionView;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.ConfigurationAdapter;

public class ConfigSelectionFragment extends PresenterFragment<ConfigSelectionView>
        implements ArrayRecyclerAdapter.OnItemClickedListener<Configuration> {
    public static final String EXPANSION_ID_KEY = ConfigSelectionFragment.class.getSimpleName() + ".expansion_id_key";

    @Inject
    ConfigurationsInteractor configurationsInteractor;

    @Inject
    ExpansionDetailsInteractor expansionDetailsInteractor;

    private ConfigurationAdapter adapter;
    private Expansion expansion;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(expansionDetailsInteractor);
        addInteractor(configurationsInteractor);
    }

    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            this.adapter = new ConfigurationAdapter(new ArrayList<>(2));
            this.adapter.setOnItemClickedListener(this);
            presenterView = new ConfigSelectionView(getActivity(), adapter);
            presenterView.setDoneButtonClickListener(this::onDoneButtonClicked);
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(expansionDetailsInteractor.expansionSubject,
                         this::bindExpansion,
                         this::presentError);

        bindAndSubscribe(configurationsInteractor.configSubject,
                         this::bindConfigurations,
                         this::presentError);
    }

    @Override
    public void onResume() {
        super.onResume();
        hideBlockingActivity(false, null);
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

    @Override
    public void onItemClicked(final int position, @NonNull final Configuration item) {
        adapter.setSelectedItem(position);
    }

    @VisibleForTesting
    public void bindExpansion(@Nullable final Expansion expansion) {
        if (expansion != null) {
            this.expansion = expansion;
            presenterView.setTitle(getString(R.string.expansions_configuration_selection_title_format, expansion.getServiceName()));
            presenterView.setSubtitle(getString(R.string.expansions_configuration_selection_subtitle_format, expansion.getConfigurationType()));
            configurationsInteractor.setExpansionId(expansion.getId());
            configurationsInteractor.update();
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
    }

    private Configuration.Empty getEmptyConfiguration(@Nullable final Expansion expansion) {
        if (expansion != null) {
            return new Configuration.Empty(getString(R.string.expansions_configuration_selection_item_missing_title_format, expansion.getConfigurationType()),
                                                       getString(R.string.expansions_configuration_selection_item_missing_subtitle_format, expansion.getServiceName(), expansion.getConfigurationType()),
                                                       R.drawable.icon_warning);
        } else {
            return new Configuration.Empty(getString(R.string.expansions_configuration_selection_empty_title_default),
                                                       getString(R.string.expansions_configuration_selection_empty_subtitle_default),
                                                       R.drawable.icon_warning);
        }
    }

    @VisibleForTesting
    public void bindConfigurationPostResponse(@NonNull final Configuration configuration) {
        final Intent intent = new Intent();
        intent.putExtra(EXPANSION_ID_KEY, expansion.getId());
        hideBlockingActivity(R.string.expansions_configuration_selection_configured,
                             stateSafeExecutor.bind(() -> finishFlowWithResult(Activity.RESULT_OK, intent)));

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
                showBlockingActivity(getString(R.string.expansions_configuration_selection_setting_progress_format, expansion.getCategory()));
            } else {
                showBlockingActivity(R.string.expansions_configuration_selection_setting_progress_default);
            }
            bindAndSubscribe(configurationsInteractor.setConfiguration(selectedConfig),
                             this::bindConfigurationPostResponse,
                             this::presentError);
        } else {
            finishFlow();
        }
    }
}
