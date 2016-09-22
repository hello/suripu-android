package is.hello.sense.mvp.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphDrawable;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphView;

@SuppressLint("ViewConstructor")
public final class SensorDetailView extends PresenterView {
    private final SelectorView subNavSelector;
    private final TextView value;
    private final TextView message;
    private final SensorGraphView sensorGraphView;

    public SensorDetailView(@NonNull final Activity activity) {
        super(activity);
        this.subNavSelector = (SelectorView) findViewById(R.id.fragment_sensor_detail_selector);
        this.value = (TextView) findViewById(R.id.fragment_sensor_detail_value);
        this.message = (TextView) findViewById(R.id.fragment_sensor_detail_message);
        this.sensorGraphView = (SensorGraphView) findViewById(R.id.fragment_sensor_detail_graph_view);

    }

    @Override
    protected final int getLayoutRes() {
        return R.layout.fragment_sensor_detail;
    }

    @Override
    public final void releaseViews() {

    }

    public final void showSensor(@NonNull final Sensor sensor) {
        this.subNavSelector.removeAllButtons();
        final int color = sensor.getColor(context);
        this.subNavSelector.setToggleButtonColor(R.color.white);
        this.subNavSelector.addOption(R.string.sensor_detail_last_day, false)
                           .setBackgroundColor(color);
        this.subNavSelector.addOption(R.string.sensor_detail_past_week, false)
                           .setBackgroundColor(color);
        this.subNavSelector.setBackgroundColor(color);
        this.value.setText(sensor.getFormattedValue(true));
        this.message.setText(sensor.getMessage());
        this.value.setTextColor(color);
        this.sensorGraphView.resetTimeToAnimate(SensorGraphView.StartDelay.LONG);
        this.sensorGraphView.setSensorGraphDrawable(new SensorGraphDrawable(context, sensor));
    }
}
