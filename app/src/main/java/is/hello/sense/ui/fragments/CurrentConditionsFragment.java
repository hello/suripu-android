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
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.SensorStateView;
import is.hello.sense.util.Logger;

public class CurrentConditionsFragment extends InjectionFragment {
    @Inject CurrentConditionsPresenter presenter;

    private SensorStateView temperatureState;
    private SensorStateView humidityState;
    private SensorStateView particulatesState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_conditions, container, false);

        this.temperatureState = (SensorStateView) view.findViewById(R.id.fragment_current_conditions_temperature);
        temperatureState.setOnClickListener(ignored -> showSensorHistory(SensorHistory.SENSOR_NAME_TEMPERATURE));

        this.humidityState = (SensorStateView) view.findViewById(R.id.fragment_current_conditions_humidity);
        humidityState.setOnClickListener(ignored -> showSensorHistory(SensorHistory.SENSOR_NAME_HUMIDITY));

        this.particulatesState = (SensorStateView) view.findViewById(R.id.fragment_current_conditions_particulates);
        particulatesState.setOnClickListener(ignored -> showSensorHistory(SensorHistory.SENSOR_NAME_PARTICULATES));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.currentConditions, this::bindConditions, this::conditionsUnavailable);
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter.update();
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
}
