package is.hello.sense.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.activities.SettingsActivity;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.adapter.ViewPagerAdapter;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.InsightDialogFragment;
import is.hello.sense.ui.widget.SensorStateView;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;

public class HomeUndersideFragment extends InjectionFragment implements ViewPagerAdapter.OnItemViewClickedListener {
    @Inject InsightsPresenter insightsPresenter;
    @Inject CurrentConditionsPresenter currentConditionsPresenter;
    @Inject Markdown markdown;
    @Inject BuildValues buildValues;

    private InsightsAdapter insightsAdapter;

    private SensorStateView temperatureState;
    private SensorStateView humidityState;
    private SensorStateView particulatesState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(insightsPresenter);
        addPresenter(currentConditionsPresenter);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_underside, container, false);

        ViewPager insightsPager = (ViewPager) view.findViewById(R.id.fragment_underside_insights);
        this.insightsAdapter = new InsightsAdapter(getActivity(), markdown, view.findViewById(R.id.fragment_underside_insights_loading));
        insightsAdapter.onItemViewClickedListener = this;
        insightsPager.setClipToPadding(false);
        int padding = getResources().getDimensionPixelSize(R.dimen.gap_small) * 2;
        insightsPager.setPadding(padding, 0, padding, 0);
        insightsPager.setOffscreenPageLimit(3);
        insightsPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.gap_small));
        insightsPager.setAdapter(insightsAdapter);
        Animation.Properties.DEFAULT.apply(insightsPager.getLayoutTransition(), false);

        this.temperatureState = (SensorStateView) view.findViewById(R.id.fragment_underside_temperature);
        temperatureState.setOnClickListener(ignored -> showSensorHistory(SensorHistory.SENSOR_NAME_TEMPERATURE));

        this.humidityState = (SensorStateView) view.findViewById(R.id.fragment_underside_humidity);
        humidityState.setOnClickListener(ignored -> showSensorHistory(SensorHistory.SENSOR_NAME_HUMIDITY));

        this.particulatesState = (SensorStateView) view.findViewById(R.id.fragment_underside_particulates);
        particulatesState.setOnClickListener(ignored -> showSensorHistory(SensorHistory.SENSOR_NAME_PARTICULATES));

        ImageButton settings = (ImageButton) view.findViewById(R.id.fragment_underside_settings);
        settings.setOnClickListener(ignored -> startActivity(new Intent(getActivity(), SettingsActivity.class)));

        SensorStateView debug = (SensorStateView) view.findViewById(R.id.fragment_underside_debug);
        if (buildValues.debugScreenEnabled) {
            debug.setOnClickListener(ignored -> startActivity(new Intent(getActivity(), DebugActivity.class)));
        } else {
            view.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(currentConditionsPresenter.currentConditions, this::bindConditions, this::conditionsUnavailable);
        bindAndSubscribe(insightsPresenter.insights, insightsAdapter::bindInsights, insightsAdapter::insightsUnavailable);
    }

    @Override
    public void onStart() {
        super.onStart();

        insightsPresenter.update();
        currentConditionsPresenter.update();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= Presenter.BASE_TRIM_LEVEL) {
            insightsAdapter.clear();
        }
    }

    //region Displaying Data

    public void bindConditions(@Nullable CurrentConditionsPresenter.Result result) {
        if (result == null) {
            temperatureState.displayCondition(Condition.UNKNOWN);
            temperatureState.setReading(getString(R.string.missing_data_placeholder));

            humidityState.displayCondition(Condition.UNKNOWN);
            humidityState.setReading(getString(R.string.missing_data_placeholder));

            particulatesState.displayCondition(Condition.UNKNOWN);
            particulatesState.setReading(getString(R.string.missing_data_placeholder));
        } else {
            temperatureState.displayReading(result.conditions.getTemperature(), result.units::formatTemperature);
            humidityState.displayReading(result.conditions.getHumidity(), null);
            particulatesState.displayReading(result.conditions.getParticulates(), result.units::formatParticulates);
        }
    }

    public void conditionsUnavailable(@NonNull Throwable e) {
        Logger.error(HomeUndersideFragment.class.getSimpleName(), "Could not load conditions", e);

        temperatureState.displayCondition(Condition.UNKNOWN);
        temperatureState.setReading(getString(R.string.missing_data_placeholder));

        humidityState.displayCondition(Condition.UNKNOWN);
        humidityState.setReading(getString(R.string.missing_data_placeholder));

        particulatesState.displayCondition(Condition.UNKNOWN);
        particulatesState.setReading(getString(R.string.missing_data_placeholder));
    }

    //endregion


    public void showSensorHistory(@NonNull String sensor) {
        Intent intent = new Intent(getActivity(), SensorHistoryActivity.class);
        intent.putExtra(SensorHistoryActivity.EXTRA_SENSOR, sensor);
        startActivity(intent);
    }

    @Override
    public void onItemViewClicked(@NonNull View view, int position) {
        InsightDialogFragment dialogFragment = InsightDialogFragment.newInstance(insightsAdapter.getItem(position));
        dialogFragment.show(getFragmentManager(), InsightDialogFragment.TAG);
    }
}
