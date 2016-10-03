package is.hello.sense.mvp.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.SensorDetailScrollView;
import is.hello.sense.ui.widget.SensorScaleList;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphDrawable;
import is.hello.sense.ui.widget.graphing.sensors.SensorGraphView;
import is.hello.sense.units.UnitFormatter;

@SuppressLint("ViewConstructor")
public final class SensorDetailView extends PresenterView
        implements SelectorView.OnSelectionChangedListener {
    /**
     * Scales the graph to consume this much of remaining space below text.
     */
    private static final float GRAPH_HEIGHT_RATIO = .65f;


    private final SelectorView subNavSelector;
    private final TextView valueTextView;
    private final TextView messageTextView;
    private final SensorGraphView sensorGraphView;
    private final SensorDetailScrollView scrollView;
    private final SensorScaleList scaleList;
    private final ProgressBar progressBar;
    private final TextView about;
    private final SelectorView.OnSelectionChangedListener selectionChangedListener;
    private final UnitFormatter unitFormatter;
    private int graphHeight = 0;
    private int lastSelectedIndex = -1;

    public SensorDetailView(@NonNull final Activity activity,
                            @NonNull final UnitFormatter unitFormatter,
                            @NonNull final SelectorView.OnSelectionChangedListener listener,
                            @NonNull final SensorGraphDrawable.ScrubberCallback scrubberCallback) {
        super(activity);
        this.unitFormatter = unitFormatter;
        this.subNavSelector = (SelectorView) findViewById(R.id.fragment_sensor_detail_selector);
        this.valueTextView = (TextView) findViewById(R.id.fragment_sensor_detail_value);
        this.messageTextView = (TextView) findViewById(R.id.fragment_sensor_detail_message);
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
        this.sensorGraphView.setScrubberCallback(scrubberCallback);
        this.subNavSelector.setOnSelectionChangedListener(this);
        this.selectionChangedListener = listener;

        final View frameLayout = findViewById(R.id.fragment_sensor_detail_sensor_graph_container);
        //resize height and margin of sensor graph container to fit remainder of screen in portrait mode.
        post(() -> {
                    SensorDetailView.this.graphHeight = Math.max((int) ((scrollView.getHeight()) * GRAPH_HEIGHT_RATIO),
                                                                 SensorDetailView.this.context.getResources().getDimensionPixelSize(R.dimen.sensor_graph_height));
                    frameLayout.setLayoutParams(new LinearLayout.LayoutParams(scrollView.getWidth(), graphHeight));
                    subNavSelector.getButtonAt(0).callOnClick();
                });

    }

    /** in landscape mode only difference is top view uses {@link is.hello.sense.R.dimen#x3} margin */
    @Override
    protected final int getLayoutRes() {
        return R.layout.fragment_sensor_detail;
    }

    @Override
    public final void releaseViews() {
        this.subNavSelector.setOnSelectionChangedListener(null);
        this.sensorGraphView.release();
    }

    public final void updateSensor(@NonNull final Sensor sensor) {
        post(() -> {
            this.scaleList.renderScales(sensor.getScales(), unitFormatter.getMeasuredInString(sensor.getType()));
            this.about.setText(unitFormatter.getAboutStringRes(sensor.getType()));

            final int color = ContextCompat.getColor(context, sensor.getColor());
            this.subNavSelector.setBackgroundColor(color);
            this.subNavSelector.setBackground(new TabsBackgroundDrawable(context.getResources(),
                                                                         TabsBackgroundDrawable.Style.SENSOR_DETAIL,
                                                                         color));
            this.subNavSelector.getButtonAt(0).setBackgroundColor(color);
            this.subNavSelector.getButtonAt(1).setBackgroundColor(color);
            this.valueTextView.setTextColor(color);
        });
    }

    public final void bindError(@NonNull final Throwable throwable) {
        //  refreshGraph(SensorGraphView.StartDelay.SHORT); // todo change

    }

    public void setGraph(@NonNull final Sensor sensor,
                         @NonNull final SensorGraphView.StartDelay delay,
                         @NonNull final String[] labels) {
        post(() -> {
            //   this.valueTextView.setText(sensor.getFormattedValue(true));
            this.messageTextView.setText(sensor.getMessage());
            this.progressBar.setVisibility(GONE);
            this.sensorGraphView.setVisibility(VISIBLE);
            this.sensorGraphView.resetTimeToAnimate(delay);
            this.sensorGraphView.setSensorGraphDrawable(new SensorGraphDrawable(this.context,
                                                                                sensor,
                                                                                this.unitFormatter,
                                                                                this.graphHeight,
                                                                                labels));
            this.sensorGraphView.invalidate();
            postDelayed(() -> this.subNavSelector.setEnabled(true), delay.getLength());
        });
    }

    private void refreshWithProgress() {
        post(() -> {
            this.progressBar.setVisibility(VISIBLE);
            this.sensorGraphView.setVisibility(INVISIBLE);
        });
    }

    public final void setValueAndMessage(@Nullable final CharSequence value, @Nullable final String message) {
        this.valueTextView.setText(value);
        this.messageTextView.setText(message);

    }

    @Override
    public void onSelectionChanged(final int newSelectionIndex) {
        if (this.lastSelectedIndex == newSelectionIndex) {
            return;
        }
        this.subNavSelector.setEnabled(false);
        this.lastSelectedIndex = newSelectionIndex;
        refreshWithProgress();
        selectionChangedListener.onSelectionChanged(newSelectionIndex);
    }
}
