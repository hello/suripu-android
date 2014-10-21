package is.hello.sense.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.activities.SettingsActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SensorStateView;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Logger;

public class HomeUndersideFragment extends InjectionFragment {
    @Inject CurrentConditionsPresenter currentConditionsPresenter;
    @Inject BuildValues buildValues;

    private SensorStateView temperatureState;
    private SensorStateView humidityState;
    private SensorStateView particulatesState;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_underside, container, false);

        this.temperatureState = (SensorStateView) view.findViewById(R.id.fragment_underside_temperature);
        temperatureState.setOnClickListener(ignored -> showSensorHistory(SensorHistory.SENSOR_NAME_TEMPERATURE));

        this.humidityState = (SensorStateView) view.findViewById(R.id.fragment_underside_humidity);
        humidityState.setOnClickListener(ignored -> showSensorHistory(SensorHistory.SENSOR_NAME_HUMIDITY));

        this.particulatesState = (SensorStateView) view.findViewById(R.id.fragment_underside_particulates);
        particulatesState.setOnClickListener(ignored -> showSensorHistory(SensorHistory.SENSOR_NAME_PARTICULATES));

        SensorStateView settings = (SensorStateView) view.findViewById(R.id.fragment_underside_settings);
        settings.setOnClickListener(ignored -> startActivity(new Intent(getActivity(), SettingsActivity.class)));

        SensorStateView debug = (SensorStateView) view.findViewById(R.id.fragment_underside_debug);
        if (buildValues.debugEnabled) {
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
    }

    @Override
    public void onStart() {
        super.onStart();

        currentConditionsPresenter.update();
    }


    //region Displaying Data

    public void bindConditions(@NonNull CurrentConditionsPresenter.Result result) {
        temperatureState.displayReading(result.conditions.getTemperature(), result.units::formatTemperature);
        humidityState.displayReading(result.conditions.getHumidity(), null);
        particulatesState.displayReading(result.conditions.getParticulates(), result.units::formatParticulates);
    }

    public void conditionsUnavailable(@NonNull Throwable e) {
        if (buildValues.isDebugBuild()) {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        } else {
            Logger.error(HomeUndersideFragment.class.getSimpleName(), "Could not load conditions", e);
            temperatureState.displayCondition(Condition.UNKNOWN);
            temperatureState.setReading(getString(R.string.missing_data_placeholder));
        }
    }

    //endregion


    public void showSensorHistory(@NonNull String sensor) {
        Intent intent = new Intent(getActivity(), SensorHistoryActivity.class);
        intent.putExtra(SensorHistoryActivity.EXTRA_SENSOR, sensor);
        startActivity(intent);
    }
}
