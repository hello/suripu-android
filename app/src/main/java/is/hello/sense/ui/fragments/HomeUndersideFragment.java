package is.hello.sense.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.SensorState;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.activities.SettingsActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SensorStateView;
import is.hello.sense.units.UnitFormatter;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class HomeUndersideFragment extends InjectionFragment {
    @Inject CurrentConditionsPresenter currentConditionsPresenter;
    @Inject UnitFormatter unitsFormatter;

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
        debug.setOnClickListener(ignored -> startActivity(new Intent(getActivity(), DebugActivity.class)));

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<SensorState> temperature = bindFragment(this, currentConditionsPresenter.temperature);
        track(temperature.subscribe(this::bindTemperature, this::presentError));

        Observable<SensorState> humidity = bindFragment(this, currentConditionsPresenter.humidity);
        track(humidity.subscribe(this::bindHumidity, Functions::ignoreError));

        Observable<SensorState> particulates = bindFragment(this, currentConditionsPresenter.particulates);
        track(particulates.subscribe(this::bindParticulates, Functions::ignoreError));
    }

    @Override
    public void onStart() {
        super.onStart();

        currentConditionsPresenter.update();
    }


    //region Displaying Data

    private void displayCondition(@Nullable SensorState condition,
                                  @NonNull SensorStateView view,
                                  @NonNull UnitFormatter.Formatter formatter) {
        if (condition == null || condition.getValue() == null) {
            view.setReading(getString(R.string.missing_data_placeholder));
            view.displayCondition(Condition.UNKNOWN);
        } else {
            view.setReading(formatter.format(condition.getValue()));
            view.displayCondition(condition.getCondition());
        }
    }

    public void bindTemperature(@Nullable SensorState condition) {
        displayCondition(condition, temperatureState, unitsFormatter::formatTemperature);
    }

    public void bindHumidity(@Nullable SensorState condition) {
        displayCondition(condition, temperatureState, unitsFormatter::formatPercentage);
    }

    public void bindParticulates(@Nullable SensorState condition) {
        displayCondition(condition, temperatureState, unitsFormatter::formatRaw);
    }

    public void presentError(@NonNull Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }

    //endregion


    public void showSensorHistory(@NonNull String sensor) {
        Intent intent = new Intent(getActivity(), SensorHistoryActivity.class);
        intent.putExtra(SensorHistoryActivity.EXTRA_SENSOR, sensor);
        startActivity(intent);
    }
}
