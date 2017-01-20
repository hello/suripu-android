package is.hello.sense.ui.widget.timeline;


import android.content.Context;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.LocalDate;

import java.util.Locale;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineMetric;
import is.hello.sense.databinding.ViewShareTimelineBinding;
import is.hello.sense.ui.widget.SensorConditionView;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.util.Styles;

public class TimelineImageGenerator {

    @Nullable
    public static Bitmap createShareableTimeline(@NonNull final Context context, @Nullable final Timeline timeline) {
        if (timeline == null) {
            return null;
        }
        final Resources resources = context.getResources();
        final SensorImage[] sensors = new SensorImage[]{
                SensorImage.TEMPERATURE,
                SensorImage.HUMIDITY,
                SensorImage.PARTICULATES,
                SensorImage.LIGHT,
                SensorImage.SOUND
        };
        // Initiate views
        final int width = resources.getDimensionPixelSize(R.dimen.timeline_shareable_view_width);
        final int height = resources.getDimensionPixelSize(R.dimen.timeline_shareable_view_height);
        final int sensorHorizontalMargin = resources.getDimensionPixelSize(R.dimen.x1);
        final int scoreTopMargin = resources.getDimensionPixelSize(R.dimen.x4);

        final ViewShareTimelineBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.view_share_timeline, null, false);
        final TextView scoreTextView = new TextView(context);
        Styles.setTextAppearance(scoreTextView, R.style.AppTheme_Text_Score_Share_Title);
        scoreTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);


        // Set values
        int score = 0;
        int color = ContextCompat.getColor(context, ScoreCondition.UNAVAILABLE.colorRes);
        String scoreText = context.getString(R.string.missing_data_placeholder);

        if (timeline.getScore() != null) {
            score = timeline.getScore();
            scoreText = Integer.toString(score);
        }

        if (timeline.getScoreCondition() != null) {
            color = ContextCompat.getColor(context, timeline.getScoreCondition().colorRes);
        }

        if (timeline.getDate() != null) {
            binding.viewShareTimelineDate.setText(getTimelineShareDateString(timeline.getDate()));
        }

        final TimelineMetric timeToFallAsleepMetric = timeline.getMetricWithName(TimelineMetric.Name.TIME_TO_SLEEP);
        final TimelineMetric timeSleptMetric = timeline.getMetricWithName(TimelineMetric.Name.TOTAL_SLEEP);
        final TimelineMetric timesWokenUpMetric = timeline.getMetricWithName(TimelineMetric.Name.TIMES_AWAKE);

        if (timeToFallAsleepMetric.isValid()
                && timeSleptMetric.isValid()
                && timesWokenUpMetric.isValid()) {
            binding.viewShareTimelineSleptForTime.setText(Styles.fromHtml(
                    resources.getString(R.string.share_timeline_sleep_time,
                                        getMetricTimeString(resources, timeToFallAsleepMetric.getValue()),
                                        getMetricTimeString(resources, timeSleptMetric.getValue()),
                                        getMetricQuantityString(resources, timesWokenUpMetric.getValue()))));
        }

        for (final SensorImage sensorImage : sensors) {
            final TimelineMetric metric = timeline.getMetricWithName(sensorImage.name);
            final SensorConditionView sensor = new SensorConditionView(context);
            final Drawable sensorIcon = ResourcesCompat.getDrawable(resources, sensorImage.imageRes, null);
            if (sensorIcon != null) {
                final int sensorColor = ContextCompat.getColor(context, metric.getCondition().colorRes);
                sensor.setIcon(sensorIcon);
                sensor.setTint(sensorColor);
                binding.viewShareTimelineSensors.addView(sensor, layoutParams);
            }
        }

        final SleepScoreDrawable scoreDrawable = new SleepScoreDrawable(resources, true);
        scoreDrawable.setValue(score);
        scoreDrawable.setStateful(true);
        scoreDrawable.setFillColor(color);
        scoreTextView.setText(scoreText);
        scoreTextView.setTextColor(color);


        // Draw it
        binding.getRoot().measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        binding.getRoot().layout(0, 0, binding.getRoot().getMeasuredWidth(), binding.getRoot().getMeasuredHeight());
        scoreTextView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        scoreTextView.layout(0, 0, scoreTextView.getMeasuredWidth(), scoreTextView.getMeasuredHeight());


        final Bitmap scoreBitmap = Bitmap.createBitmap(scoreDrawable.getIntrinsicWidth(), scoreDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas scoreCanvas = new Canvas(scoreBitmap);
        scoreDrawable.draw(scoreCanvas);

        final Bitmap bitmap = Bitmap.createBitmap(binding.getRoot().getMeasuredWidth(), binding.getRoot().getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        binding.getRoot().draw(canvas);

        final Bitmap scoreTextBitmap = getBitmap(scoreTextView);
        canvas.drawBitmap(scoreBitmap,
                          (width - scoreDrawable.getIntrinsicWidth()) / 2,
                          (binding.viewShareTimelineContainer.getTop()),
                          null);

        canvas.drawBitmap(scoreTextBitmap,
                          (width - scoreTextBitmap.getWidth()) / 2,
                          binding.viewShareTimelineContainer.getTop() + scoreTopMargin,
                          null);
        for (int i = 0; i < binding.viewShareTimelineSensors.getChildCount(); i++) {
            final View view = binding.viewShareTimelineSensors.getChildAt(i);
            canvas.drawBitmap(getBitmap(view),
                              binding.viewShareTimelineSensors.getLeft() + (i * (view.getWidth() + sensorHorizontalMargin) - sensorHorizontalMargin / 2),
                              binding.viewShareTimelineSensors.getTop() + view.getMeasuredHeight() / 2,
                              null);
        }

        return bitmap;
    }

    private static String getTimelineShareDateString(@NonNull final LocalDate date) {
        return date.toString("MMMM dd, yyyy").toUpperCase();
    }

    private static String getMetricTimeString(@NonNull final Resources resources, float minutes) {
        int unitRes = R.plurals.minutes;
        if (minutes >= 60) {
            minutes = minutes / 60;
            unitRes = R.plurals.hours;
        }
        String result = String.format(Locale.US, "%.01f", minutes);
        result = Styles.stripTrailingPeriods(Styles.stripTrailingZeros(result));
        return result + " " + resources.getQuantityString(unitRes, (int) Math.ceil(minutes));
    }

    private static String getMetricQuantityString(@NonNull final Resources resources,
                                                  final float times) {
        return Styles.stripTrailingPeriods(Styles.stripTrailingZeros(Float.toString(times))) + " " + resources.getQuantityString(R.plurals.times, (int) times);
    }

    private static Bitmap getBitmap(@NonNull final View view) {
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;

    }

    private enum SensorImage implements Enums.FromString {

        TEMPERATURE(TimelineMetric.Name.TEMPERATURE, R.drawable.room_check_sensor_filled_temperature),
        HUMIDITY(TimelineMetric.Name.HUMIDITY, R.drawable.room_check_sensor_filled_humidity),
        PARTICULATES(TimelineMetric.Name.PARTICULATES, R.drawable.room_check_sensor_filled_airquality),
        LIGHT(TimelineMetric.Name.LIGHT, R.drawable.room_check_sensor_filled_light),
        SOUND(TimelineMetric.Name.SOUND, R.drawable.room_check_sensor_filled_sound);

        private final TimelineMetric.Name name;
        private final
        @DrawableRes
        int imageRes;

        SensorImage(@NonNull final TimelineMetric.Name name,
                    @DrawableRes final int imageRes) {
            this.name = name;
            this.imageRes = imageRes;
        }
    }
}
