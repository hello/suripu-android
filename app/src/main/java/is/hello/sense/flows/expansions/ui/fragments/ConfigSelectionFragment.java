package is.hello.sense.flows.expansions.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.flows.expansions.ui.views.ConfigSelectionView;
import is.hello.sense.interactors.ConfigurationsInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.ConfigurationAdapter;

import static android.app.Activity.RESULT_OK;

public class ConfigSelectionFragment extends PresenterFragment<ConfigSelectionView>
        implements ArrayRecyclerAdapter.OnItemClickedListener<Configuration> {

    @Inject
    ConfigurationsInteractor configurationsInteractor;

    public static final String EXTRA_REQUEST = ConfigSelectionFragment.class + "EXTRA_REQUEST";

    private static final String ARG_EXPANSION = ConfigSelectionFragment.class + "ARG_EXPANSION";

    private ConfigurationAdapter adapter;
    private Expansion expansion;

    public static ConfigSelectionFragment newInstance(@NonNull final Expansion expansion){
        final ConfigSelectionFragment fragment = new ConfigSelectionFragment();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_EXPANSION, expansion);
        fragment.setArguments(args);
        return fragment;
    }

    public static Intent newIntent(@NonNull final Expansion expansion) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_REQUEST,true);
        intent.putExtra(ARG_EXPANSION, expansion);
        return intent;
    }

    @Override
    public void initializePresenterView() {
        if(presenterView == null){
            this.adapter = new ConfigurationAdapter(new ArrayList<>(2));
            this.adapter.setOnItemClickedListener(this);
            presenterView = new ConfigSelectionView(getActivity(), adapter);
            presenterView.setDoneButtonClickListener(this::onDoneButtonClicked);
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handleArgs(getArguments());
        bindAndSubscribe(configurationsInteractor.configSubject,
                         this::bindConfigurations,
                         this::presentError);
    }

    @Override
    public void onResume() {
        super.onResume();
        configurationsInteractor.update();
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        if(this.adapter != null){
            adapter.setOnItemClickedListener(null);
            adapter.clear();
            adapter = null;
        }
    }

    @Override
    public void onItemClicked(final int position, @NonNull final Configuration item) {
        adapter.setSelectedItem(position);
    }

    public void bindConfigurations(@Nullable final List<Configuration> configurations){
        if(configurations == null) {
            this.adapter.clear();
        } else {
            if(configurations.isEmpty()){
                if(expansion != null) {
                    configurations.add(new Configuration.Empty(getString(R.string.expansions_configuration_selection_item_missing_title_format, expansion.getConfigurationType()),
                                                               getString(R.string.expansions_configuration_selection_item_missing_subtitle_format, expansion.getServiceName(), expansion.getConfigurationType()),
                                                               R.drawable.icon_warning));
                } else {
                    configurations.add(new Configuration.Empty(getString(R.string.expansions_configuration_selection_empty_title_default),
                                                               getString(R.string.expansions_configuration_selection_empty_subtitle_default),
                                                               R.drawable.icon_warning));
                }
            }
            this.adapter.setSelectedItemFromList(configurations);
            this.adapter.replaceAll(configurations);
        }
    }

    public void presentError(@NonNull final Throwable e){
        hideBlockingActivity(false, null);
        //todo
    }

    private void handleArgs(@Nullable final Bundle arguments) {
        if(arguments != null){
            this.expansion = (Expansion) arguments.getSerializable(ARG_EXPANSION);
            if(expansion != null) {
                presenterView.setTitle(getString(R.string.expansions_configuration_selection_title_format, expansion.getServiceName()));
                presenterView.setSubtitle(getString(R.string.expansions_configuration_selection_subtitle_format, expansion.getConfigurationType()));
                configurationsInteractor.setExpansionId(expansion.getId());
            }
        }
    }

    public void bindConfigurationPostResponse(@NonNull final Configuration configuration){
        hideBlockingActivity(R.string.expansions_configuration_selection_configured,
                             stateSafeExecutor.bind(() -> this.finishFlowWithResult(RESULT_OK, new Intent())));
    }

    private void onDoneButtonClicked(final View ignored) {
        final Configuration selectedConfig = adapter.getSelectedItem();
        if(selectedConfig != null){
            if(expansion != null) {
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
