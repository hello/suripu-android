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
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.ui.activities.SensorHistoryActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SensorStateView;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class HomeUndersideFragment extends InjectionFragment {
    @Inject CurrentConditionsPresenter currentConditionsPresenter;

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

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<RoomConditions> currentConditions = bindFragment(this, currentConditionsPresenter.currentConditions);
        track(currentConditions.subscribe(this::bindConditions, this::presentError));
    }

    @Override
    public void onStart() {
        super.onStart();

        currentConditionsPresenter.update();
    }


    //region Displaying Data

    private void displayCondition(@Nullable SensorState condition,
                                  @NonNull SensorStateView view,
                                  @DrawableRes int iconRes,
                                  @StringRes int titleRes) {
        view.setIconDrawable(getResources().getDrawable(iconRes));
        view.setTitle(getString(titleRes));

        if (condition == null) {
            view.setReading(getString(R.string.missing_data_placeholder));
            view.displayCondition(Condition.UNKNOWN);
        } else {
            view.setReading(Integer.toString(condition.getValue()));
            view.displayCondition(condition.getCondition());
        }
    }

    public void bindConditions(@NonNull RoomConditions conditions) {
        displayCondition(conditions.getTemperature(), temperatureState, R.drawable.icon_sensor_temperature, R.string.condition_temperature);
        displayCondition(conditions.getHumidity(), humidityState, R.drawable.icon_sensor_humidity, R.string.condition_humidity);
        displayCondition(conditions.getParticulates(), particulatesState, R.drawable.icon_sensor_particle, R.string.condition_particulates);
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
