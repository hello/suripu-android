package is.hello.sense.mvp.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorType;
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
    private List<is.hello.sense.api.model.v2.sensors.X> timestamps;
    private boolean use24Hour = false;
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


        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                /*

                    There isn't a way to position the graph at the bottom of the screen while it's in a
                    ScrollView via xml.

                    The following will wait until the layout is drawn to measure the screen height and
                    how much space remains underneath the message TextView.It will then take that space
                    and scale the graph to fit 65 % of it.Then it will set the top margin of the graph
                    to be 45 % of the remaining space, pushing it to the bottom of the screen.
                */
                final int topMargin;
                // Height of ScrollView
                final int scrollViewHeight = scrollView.getHeight();
                // Y position of message's bottom
                final float messageBottomY = messageTextView.getY() + messageTextView.getHeight();
                // Scaled graph height if its larger than default height
                SensorDetailView.this.graphHeight = Math.max((int) ((scrollViewHeight - messageBottomY) * GRAPH_HEIGHT_RATIO),
                                                             SensorDetailView.this.context.getResources().getDimensionPixelSize(R.dimen.sensor_graph_height));

                if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    // in portrait mode we want the graph to sit at the bottom of the screen.
                    topMargin = scrollViewHeight - SensorDetailView.this.graphHeight;
                } else {
                    graphHeight *= 1.5f;
                    // in landscape it can sit underneath the message textview.
                    topMargin = (int) messageBottomY;
                }
                final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SensorDetailView.this.graphHeight);
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
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                subNavSelector.getButtonAt(0).callOnClick();
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

    public final void updateSensor(@NonNull final Sensor sensor) {
        post(() -> {
            this.scaleList.renderScales(sensor.getScales(), unitFormatter.getMeasuredInString(sensor.getType()));
            this.about.setText(unitFormatter.getAboutStringRes(sensor.getType()));

            final int color = ContextCompat.getColor(context, sensor.getColor());
            this.subNavSelector.setBackgroundColor(color);
            this.subNavSelector.setBackground(new TabsBackgroundDrawable(context.getResources(),
                                                                         TabsBackgroundDrawable.Style.SUBNAV,
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
            this.valueTextView.setText(unitFormatter.getUnitPrinterForSensorAverageValue(sensor.getType()).print(sensor.getValue()));
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
