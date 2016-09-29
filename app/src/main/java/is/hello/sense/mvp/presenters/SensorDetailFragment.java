package is.hello.sense.mvp.presenters;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.sensors.QueryScope;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorDataRequest;
import is.hello.sense.api.model.v2.sensors.SensorResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.mvp.view.SensorDetailView;
import is.hello.sense.ui.widget.SelectorView;

public final class SensorDetailFragment extends PresenterFragment<SensorDetailView>
        implements SelectorView.OnSelectionChangedListener {
    private static final String ARG_SENSOR = SensorDetailFragment.class.getName() + ".ARG_SENSOR";

    public static SensorDetailFragment createFragment(@NonNull final Sensor sensor) {
        final SensorDetailFragment sensorDetailFragment = new SensorDetailFragment();
        final Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_SENSOR, sensor);
        sensorDetailFragment.setArguments(bundle);
        return sensorDetailFragment;
    }

    @Inject
    ApiService apiService;
    @Inject
    PreferencesInteractor preferences;

    private Sensor sensor;


    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            this.presenterView = new SensorDetailView(getActivity(),
                                                      this,
                                                      sensor);
            this.presenterView.set24HourTime(preferences.getUse24Time());
        }
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(preferences);
        final Bundle args = getArguments();
        if (args == null) {
            finishWithResult(Activity.RESULT_CANCELED, null);
            return;
        }
        if (!args.containsKey(ARG_SENSOR)) {
            finishWithResult(Activity.RESULT_CANCELED, null);
            return;
        }
        this.sensor = (Sensor) args.getSerializable(ARG_SENSOR);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(preferences.observableUse24Time(),
                         aBoolean -> presenterView.set24HourTime(aBoolean != null && aBoolean),
                         Functions.LOG_ERROR);
    }

    @Override
    public synchronized void onSelectionChanged(final int newSelectionIndex) {
        switch (newSelectionIndex) {
            case 0:
                updateSensors(QueryScope.DAY_5_MINUTE);
                break;
            case 1:
                updateSensors(QueryScope.WEEK_1_HOUR);
                break;
            default:
                throw new IllegalArgumentException(newSelectionIndex + " is not an option");
        }
    }

    //todo consider creating a hashmap to hold these in. Limit requests to time.
    private synchronized void updateSensors(@NonNull final QueryScope queryScope) {
        stateSafeExecutor.execute(() -> {
            sensor.setLabels(queryScope == QueryScope.DAY_5_MINUTE ? getDayLabels() : getWeekLabels());
            final ArrayList<Sensor> sensors = new ArrayList<>();
            sensors.add(sensor);
            apiService.postSensors(new SensorDataRequest(queryScope, sensors))
                      .subscribe(this.presenterView::bindServerDataResponse,
                                 this.presenterView::bindError);
        });
    }

    private String[] getWeekLabels() {
        final String[] labels = new String[7];
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        for (int i = 0; i < labels.length; i++) {
            calendar.add(Calendar.DATE, 1);
            final String day = dateFormat.format(calendar.getTime());
            labels[i] = day;
        }
        return labels;
    }

    private String[] getDayLabels() {
        final String[] labels = new String[7];
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateFormat;
        final int minuteDiff;
        if (preferences.getUse24Time()) {
            dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            final int unRoundedMins = calendar.get(Calendar.MINUTE) % 15;
            calendar.add(Calendar.MINUTE, unRoundedMins < 8 ? -unRoundedMins : (15 - unRoundedMins));
            calendar.add(Calendar.MINUTE, 15);
            minuteDiff = -25;
        } else {
            dateFormat = new SimpleDateFormat("ha", Locale.getDefault());
            final int unRoundedMins = calendar.get(Calendar.MINUTE) % 30;
            calendar.add(Calendar.MINUTE, 30 - unRoundedMins);
            minuteDiff = -30;
        }
        calendar.add(Calendar.HOUR, -2);

        for (int i = 6; i >= 0; i--) {
            final String day = dateFormat.format(calendar.getTime());
            labels[i] = day;
            calendar.add(Calendar.HOUR, -3);
            calendar.add(Calendar.MINUTE, minuteDiff);
        }
        return labels;
    }

}
