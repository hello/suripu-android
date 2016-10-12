package is.hello.sense.mvp.presenters;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.segment.analytics.Properties;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.sensors.QueryScope;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorDataRequest;
import is.hello.sense.api.model.v2.sensors.X;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SensorLabelInteractor;
import is.hello.sense.interactors.SensorResponseInteractor;
import is.hello.sense.mvp.view.SensorDetailView;
import is.hello.sense.ui.activities.SensorDetailActivity;
import is.hello.sense.ui.common.UpdateTimer;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphDrawable;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphView;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;

public final class SensorDetailFragment extends PresenterFragment<SensorDetailView>
        implements SelectorView.OnSelectionChangedListener,
        SensorGraphDrawable.ScrubberCallback {
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
    @Inject
    SensorResponseInteractor sensorResponseInteractor;
    @Inject
    UnitFormatter unitFormatter;
    @Inject
    SensorLabelInteractor sensorLabelInteractor;

    private Sensor sensor;
    private UpdateTimer updateTimer;
    private DateFormatter dateFormatter;
    private TimestampQuery timestampQuery = new TimestampQuery(QueryScope.DAY_5_MINUTE);


    @Override
    public final void initializePresenterView() {
        if (this.presenterView == null) {
            this.presenterView = new SensorDetailView(getActivity(),
                                                      this.unitFormatter,
                                                      this,
                                                      this);
            this.presenterView.updateSensor(this.sensor);
        }
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(this.preferences);
        addInteractor(this.sensorResponseInteractor);
        addInteractor(this.unitFormatter);
        addInteractor(this.sensorLabelInteractor);
        this.dateFormatter = new DateFormatter(getActivity());
        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_SENSOR)) {
            this.sensor = (Sensor) savedInstanceState.getSerializable(ARG_SENSOR);
        } else {
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
            sendAnalyticsEvent(sensor);
        }
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.updateTimer = new UpdateTimer(2, TimeUnit.MINUTES);
        bindAndSubscribe(this.sensorResponseInteractor.sensors,
                         sensorResponse -> {
                             for (final Sensor sensor : sensorResponse.getSensors()) {
                                 if (sensor.getType() == this.sensor.getType()) {
                                     this.sensor = sensor;
                                     updateSensors(this.timestampQuery.queryScope);
                                 }
                             }
                         },
                         this::handleError);
        this.updateTimer.setOnUpdate(this.sensorResponseInteractor::update);
    }

    @Override
    public final void onResume() {
        super.onResume();
        this.updateTimer.schedule();
    }

    @Override
    public final void onPause() {
        super.onPause();
        if (this.updateTimer != null) {
            this.updateTimer.unschedule();
        }
    }

    @Override
    protected final void onRelease() {
        super.onRelease();
        this.updateTimer = null;
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

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        if (this.sensor != null) {
            outState.putSerializable(ARG_SENSOR, this.sensor);
        }
        super.onSaveInstanceState(outState);
    }

    private void sendAnalyticsEvent(@Nullable final Sensor sensor) {
        if (sensor == null) {
            return;
        }
        final Properties properties =
                Analytics.createProperties(Analytics.Backside.PROP_SENSOR_NAME, sensor.getType());
        Analytics.trackEvent(Analytics.Backside.EVENT_SENSOR_HISTORY, properties);
    }

    // consider creating a hashmap/cache to hold these in. Limit requests to time.
    private synchronized void updateSensors(@NonNull final QueryScope queryScope) {
        this.timestampQuery = new TimestampQuery(queryScope);
        this.stateSafeExecutor.execute(() -> {
            final ArrayList<Sensor> sensors = new ArrayList<>();
            sensors.add(this.sensor);
            bind(this.apiService.postSensors(new SensorDataRequest(queryScope, sensors)))
                    .subscribe(sensorsDataResponse -> {
                                   changeActionBarColor(this.sensor.getColor());
                                   this.timestampQuery.setTimestamps(sensorsDataResponse.getTimestamps());
                                   this.sensor.setSensorValues(sensorsDataResponse);
                                   this.presenterView.updateSensor(this.sensor);
                                   this.presenterView.setGraph(this.sensor,
                                                               SensorGraphView.StartDelay.SHORT,
                                                               queryScope == QueryScope.DAY_5_MINUTE ? sensorLabelInteractor.getDayLabels() : sensorLabelInteractor.getWeekLabels());
                               },
                               this::handleError);
        });
    }

    private void handleError(@NonNull final Throwable throwable) {
        changeActionBarColor(R.color.dim);
        this.presenterView.bindError();
    }

    private void changeActionBarColor(@ColorRes final int colorRes) {
        final Activity activity = getActivity();
        if (activity instanceof SensorDetailActivity) {
            // Some bug occurs when you try to change the actionbar from the main thread causing everything to freeze but never crash.
            this.presenterView.post(() -> ((SensorDetailActivity) activity).setActionbarColor(colorRes));
        }

    }

    @Override
    public void onPositionScrubbed(final int position) {
        final CharSequence value;
        final String message;
        if (this.timestampQuery.timestamps.size() < position) {
            message = null;
        } else {
            final long timestamp = this.timestampQuery.timestamps.get(position).getTimestamp();
            if (timestamp != -1) {
                if (this.timestampQuery.queryScope == QueryScope.DAY_5_MINUTE) {
                    message = this.dateFormatter.formatAsTime(new DateTime(timestamp),
                                                              this.preferences.getUse24Time());
                } else {
                    message = this.dateFormatter.formatAsDayAndTime(new DateTime(timestamp),
                                                                    this.preferences.getUse24Time());
                }
            } else {
                message = null;
            }
        }
        if (this.sensor.getSensorValues().length > position) {
            value = unitFormatter.createUnitBuilder(sensor.getType(), this.sensor.getSensorValues()[position])
                                 .buildWithStyle();
        } else {
            value = getString(R.string.missing_data_placeholder);
        }
        this.presenterView.setValueAndMessage(value,
                                              message);

    }

    @Override
    public void onScrubberReleased() {
        this.presenterView.setValueAndMessage(unitFormatter.createUnitBuilder(sensor)
                                                           .buildWithStyle(),
                                              this.sensor.getMessage());
    }

    private class TimestampQuery {
        private final QueryScope queryScope;
        private List<X> timestamps = new ArrayList<>();

        public TimestampQuery(@NonNull final QueryScope queryScope) {
            this.queryScope = queryScope;
        }

        public void setTimestamps(@NonNull final List<X> timestamps) {
            this.timestamps = timestamps;
        }
    }
}
