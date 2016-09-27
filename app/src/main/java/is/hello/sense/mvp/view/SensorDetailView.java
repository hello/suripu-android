package is.hello.sense.mvp.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.joda.time.LocalDate;

import java.util.Calendar;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorsDataResponse;
import is.hello.sense.api.model.v2.sensors.X;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.SensorDetailScrollView;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphDrawable;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphView;
import is.hello.sense.util.DateFormatter;

@SuppressLint("ViewConstructor")
public final class SensorDetailView extends PresenterView
        implements SensorGraphDrawable.ScrubberCallback {
    /**
     * Scales the graph to consume this much of remaining space below text.
     */
    private static final float GRAPH_HEIGHT_RATIO = .65f;
    private final SelectorView subNavSelector;
    private final TextView value;
    private final TextView message;
    private final SensorGraphView sensorGraphView;
    private final SensorDetailScrollView scrollView;
    private final ProgressBar progressBar;
    private final Sensor sensor;
    private int graphHeight = 0;
    private List<X> timestamps;

    public SensorDetailView(@NonNull final Activity activity, @NonNull final SelectorView.OnSelectionChangedListener listener, @NonNull final Sensor sensor) {
        super(activity);
        this.sensor = sensor;
        this.subNavSelector = (SelectorView) findViewById(R.id.fragment_sensor_detail_selector);
        this.value = (TextView) findViewById(R.id.fragment_sensor_detail_value);
        this.message = (TextView) findViewById(R.id.fragment_sensor_detail_message);
        this.sensorGraphView = (SensorGraphView) findViewById(R.id.fragment_sensor_detail_graph_view);
        this.scrollView = (SensorDetailScrollView) findViewById(R.id.fragment_sensor_detail_scroll_view);
        this.progressBar = (ProgressBar) findViewById(R.id.fragment_sensor_detail_progress);
        this.scrollView.requestDisallowInterceptTouchEvent(true);
        this.scrollView.setGraphView(sensorGraphView);
        this.subNavSelector.setToggleButtonColor(R.color.white);
        this.subNavSelector.addOption(R.string.sensor_detail_last_day, false);
        this.subNavSelector.addOption(R.string.sensor_detail_past_week, false);
        this.subNavSelector.setSelectedButton(subNavSelector.getButtonAt(0));
        this.subNavSelector.setBackground(new TabsBackgroundDrawable(activity.getResources(),
                                                                     TabsBackgroundDrawable.Style.SUBNAV));
        this.subNavSelector.setOnSelectionChangedListener(newSelectionIndex -> {
            lockSubNavBar(true);
            listener.onSelectionChanged(newSelectionIndex);
        });
        this.sensorGraphView.setScrubberCallback(this);
        this.subNavSelector.getButtonAt(0).callOnClick();
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // gets called after layout has been done but before display
                // so we can get the height then hide the view
                final int scrollViewHeight = scrollView.getHeight();
                final float top = message.getY() + message.getHeight();
                SensorDetailView.this.graphHeight = (int) ((scrollViewHeight - top) * GRAPH_HEIGHT_RATIO);
                final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SensorDetailView.this.graphHeight);
                final int topMargin = scrollViewHeight - SensorDetailView.this.graphHeight;
                layoutParams.setMargins(0, topMargin, 0, 0);
                sensorGraphView.setLayoutParams(layoutParams);

                final RelativeLayout.LayoutParams progressBarLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                progressBarLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                progressBarLayoutParams.setMargins(0, topMargin + (graphHeight / 2) - progressBar.getHeight() / 2, 0, 0);
                progressBar.setLayoutParams(progressBarLayoutParams);
                refreshView(SensorGraphView.StartDelay.LONG);
                getViewTreeObserver().removeOnGlobalLayoutListener(this);

            }
        });
    }

    @Override
    protected final int getLayoutRes() {
        return R.layout.fragment_sensor_detail;
    }

    @Override
    public final void releaseViews() {
        this.subNavSelector.setOnSelectionChangedListener(null);
        this.sensorGraphView.release();
    }

    public final void bindServerDataResponse(@NonNull final SensorsDataResponse sensorResponse) {
        this.lockSubNavBar(false);
        this.timestamps = sensorResponse.getTimestamps();
        this.sensor.updateSensorValues(sensorResponse);
        refreshView(SensorGraphView.StartDelay.SHORT);
    }

    public final void bindError(@NonNull final Throwable throwable) {
        this.lockSubNavBar(false);

    }

    private void lockSubNavBar(final boolean lock) {
        subNavSelector.setClickable(!lock);
        refreshView(SensorGraphView.StartDelay.SHORT);
    }

    private void refreshView(@NonNull final SensorGraphView.StartDelay delay) {
        post(() -> {
            final int color = sensor.getColor(context);
            this.subNavSelector.setBackgroundColor(color);
            this.subNavSelector.getButtonAt(0).setBackgroundColor(color);
            this.subNavSelector.getButtonAt(1).setBackgroundColor(color);
            this.value.setTextColor(color);
            this.value.setText(sensor.getFormattedValue(true));
            this.message.setText(sensor.getMessage());
            if (subNavSelector.isClickable()) {
                this.progressBar.setVisibility(GONE);
                this.sensorGraphView.setVisibility(VISIBLE);
                this.sensorGraphView.resetTimeToAnimate(delay);
                this.sensorGraphView.setSensorGraphDrawable(new SensorGraphDrawable(context, sensor, this.graphHeight));
                this.sensorGraphView.invalidate();
            } else {
                this.progressBar.setVisibility(VISIBLE);
                this.sensorGraphView.setVisibility(INVISIBLE);

            }
        });
    }

    @Override
    public void onPositionScrubbed(final int position) {
        this.value.setText(sensor.getFormattedValueAtPosition(position));
        if (timestamps != null && timestamps.size() > position) {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamps.get(position).getTimestamp());

            this.message.setText(LocalDate.fromCalendarFields(calendar).toString());
        }

    }

    @Override
    public void onScrubberReleased() {
        this.value.setText(sensor.getFormattedValue(true));
        this.message.setText(sensor.getMessage());

    }
}
