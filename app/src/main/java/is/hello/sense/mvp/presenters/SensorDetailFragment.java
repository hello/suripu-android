package is.hello.sense.mvp.presenters;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.sensors.QueryScope;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorDataRequest;
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

    private Sensor sensor;


    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            this.presenterView = new SensorDetailView(getActivity(), this, sensor);
        }
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    private synchronized void updateSensors(@NonNull final QueryScope queryScope) {
        stateSafeExecutor.execute(() -> {
            final ArrayList<Sensor> sensors = new ArrayList<>();
            sensors.add(sensor);
            apiService.postSensors(new SensorDataRequest(queryScope, sensors))
                      .subscribe(this.presenterView::bindServerDataResponse,
                                 this.presenterView::bindError);
        });
    }
}
