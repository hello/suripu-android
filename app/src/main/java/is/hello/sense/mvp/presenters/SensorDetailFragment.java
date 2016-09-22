package is.hello.sense.mvp.presenters;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.mvp.view.SensorDetailView;

public final class SensorDetailFragment extends PresenterFragment<SensorDetailView> {
    private static final String ARG_SENSOR = SensorDetailFragment.class.getName() + ".ARG_SENSOR";

    public static SensorDetailFragment createFragment(@NonNull final Sensor sensor) {
        final SensorDetailFragment sensorDetailFragment = new SensorDetailFragment();
        final Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_SENSOR, sensor);
        sensorDetailFragment.setArguments(bundle);
        return sensorDetailFragment;
    }

    private Sensor sensor;


    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            this.presenterView = new SensorDetailView(getActivity());
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
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.presenterView.showSensor(this.sensor);
    }
}
