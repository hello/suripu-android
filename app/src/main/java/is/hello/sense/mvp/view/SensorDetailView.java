package is.hello.sense.mvp.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorsDataResponse;
import is.hello.sense.api.model.v2.sensors.X;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.SensorDetailScrollView;
import is.hello.sense.ui.widget.SensorScaleList;
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
    private final SensorScaleList scaleList;
    private final ProgressBar progressBar;
    private final TextView about;
    private final Sensor sensor;
    private int graphHeight = 0;
    private List<X> timestamps;
    private boolean use24Hour = false;
    private int lastSelectedIndex = -1;

    public SensorDetailView(@NonNull final Activity activity,
                            @NonNull final SelectorView.OnSelectionChangedListener listener,
                            @NonNull final Sensor sensor) {
        super(activity);
        this.sensor = sensor;
        this.subNavSelector = (SelectorView) findViewById(R.id.fragment_sensor_detail_selector);
        this.value = (TextView) findViewById(R.id.fragment_sensor_detail_value);
        this.message = (TextView) findViewById(R.id.fragment_sensor_detail_message);
        this.sensorGraphView = (SensorGraphView) findViewById(R.id.fragment_sensor_detail_graph_view);
        this.scrollView = (SensorDetailScrollView) findViewById(R.id.fragment_sensor_detail_scroll_view);
        this.progressBar = (ProgressBar) findViewById(R.id.fragment_sensor_detail_progress);
        this.scaleList = (SensorScaleList) findViewById(R.id.fragment_sensor_detail_scales);
        this.about = (TextView) findViewById(R.id.fragment_sensor_detail_about_body);
        this.scrollView.requestDisallowInterceptTouchEvent(true);
        this.scrollView.setGraphView(sensorGraphView);
        this.subNavSelector.setToggleButtonColor(R.color.white);
        this.subNavSelector.addOption(R.string.sensor_detail_last_day, false);
        this.subNavSelector.addOption(R.string.sensor_detail_past_week, false);
        this.subNavSelector.setSelectedButton(subNavSelector.getButtonAt(0));
        this.subNavSelector.setBackground(new TabsBackgroundDrawable(activity.getResources(),
                                                                     TabsBackgroundDrawable.Style.SUBNAV));
        this.subNavSelector.setOnSelectionChangedListener(newSelectionIndex -> {
            if (lastSelectedIndex == newSelectionIndex) {
                return;
            }
            lastSelectedIndex = newSelectionIndex;
            refreshWithProgress();
            listener.onSelectionChanged(newSelectionIndex);
        });
        this.scaleList.renderScales(sensor.getScales(), sensor.getSensorSuffix());
        this.about.setText(sensor.getAboutStringRes());
        this.sensorGraphView.setScrubberCallback(this);
        this.subNavSelector.getButtonAt(0).callOnClick();

        final int color = sensor.getColor(context);
        this.subNavSelector.setBackgroundColor(color);
        this.subNavSelector.getButtonAt(0).setBackgroundColor(color);
        this.subNavSelector.getButtonAt(1).setBackgroundColor(color);
        this.value.setTextColor(color);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                /*

                There isn't a way to position the graph at the bottom of the screen while it's in a
                ScrollView via xml.

                The following will wait until the layout is drawn to measure the screen height and
                how much space remains underneath the message TextView. It will then take that space
                and scale the graph to fit 65% of it. Then it will set the top margin of the graph
                to be 45% of the remaining space, pushing it to the bottom of the screen.

                 */
                // Height of ScrollView
                final int scrollViewHeight = scrollView.getHeight();
                // Y position of message's bottom
                final float messageBottomY = message.getY() + message.getHeight();
                // Scaled graph height
                SensorDetailView.this.graphHeight = (int) ((scrollViewHeight - messageBottomY) * GRAPH_HEIGHT_RATIO);
                final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SensorDetailView.this.graphHeight);
                final int topMargin = scrollViewHeight - SensorDetailView.this.graphHeight;
                layoutParams.setMargins(0, topMargin, 0, 0);
                sensorGraphView.setLayoutParams(layoutParams);

                /*
                  Because SensorGraphView is only a view we can't put a progress bar inside of it.

                  The following will position a ProgressBar directly on the center of it.
                 */
                final RelativeLayout.LayoutParams progressBarLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                progressBarLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                progressBarLayoutParams.setMargins(0, topMargin + (graphHeight / 2) - progressBar.getHeight() / 2, 0, 0);
                progressBar.setLayoutParams(progressBarLayoutParams);
                refreshWithGraph(SensorGraphView.StartDelay.LONG);
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

    public final void set24HourTime(final boolean use24hour) {
        this.use24Hour = use24hour;
    }

    public final void bindServerDataResponse(@NonNull final SensorsDataResponse sensorResponse) {
        this.timestamps = sensorResponse.getTimestamps();
        this.sensor.setSensorValues(sensorResponse);
        if (this.sensorGraphView.getVisibility() == VISIBLE) {
            refreshWithGraph(SensorGraphView.StartDelay.SHORT);
        } else {
            sensorGraphView.setVisibility(VISIBLE);
            refreshWithGraph(SensorGraphView.StartDelay.LONG);
        }
    }

    public final void bindError(@NonNull final Throwable throwable) {
        refreshWithGraph(SensorGraphView.StartDelay.SHORT); // todo change

    }

    private void refreshWithGraph(@NonNull final SensorGraphView.StartDelay delay) {
        post(() -> {
            this.value.setText(sensor.getFormattedValue(true));
            this.message.setText(sensor.getMessage());
            this.progressBar.setVisibility(GONE);
            this.sensorGraphView.setVisibility(VISIBLE);
            this.sensorGraphView.resetTimeToAnimate(delay);
            this.sensorGraphView.setSensorGraphDrawable(new SensorGraphDrawable(context, sensor, this.graphHeight));
            this.sensorGraphView.invalidate();
            postDelayed(() -> this.subNavSelector.setClickable(true), delay.getLength());
        });
    }

    private void refreshWithProgress() {
        post(() -> {
            this.subNavSelector.setClickable(false);
            this.progressBar.setVisibility(VISIBLE);
            this.sensorGraphView.setVisibility(INVISIBLE);
        });
    }


    @Override
    public void onPositionScrubbed(final int position) {
        this.value.setText(sensor.getFormattedValueAtPosition(position));
        if (timestamps != null && timestamps.size() > position) {
            this.message.setText(new DateFormatter(context)
                                         .formatAsTime(new DateTime(timestamps.get(position)
                                                                              .getTimestamp()),
                                                       use24Hour));
        }

    }

    @Override
    public void onScrubberReleased() {
        this.value.setText(sensor.getFormattedValue(true));
        this.message.setText(sensor.getMessage());

    }
}
