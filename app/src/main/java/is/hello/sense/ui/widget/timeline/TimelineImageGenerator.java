package is.hello.sense.ui.widget.timeline;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineMetric;
import is.hello.sense.ui.widget.SensorConditionView;
import is.hello.sense.ui.widget.SleepScoreDrawable;

public class TimelineImageGenerator {

    public static Bitmap createShareableTimeline(@NonNull final Activity activity, @Nullable final Timeline timeline) {
        if (timeline == null) {
            return null;
        }
        final SensorImage[] sensors = new SensorImage[]{
                SensorImage.TEMPERATURE,
                SensorImage.HUMIDITY,
                SensorImage.PARTICULATES,
                SensorImage.LIGHT,
                SensorImage.SOUND
        };
        // Initiate views
        final int width = activity.getResources().getDimensionPixelSize(R.dimen.timeline_shareable_view_width);
        final int height = activity.getResources().getDimensionPixelSize(R.dimen.timeline_shareable_view_height);
        final View cluster = LayoutInflater.from(activity).inflate(R.layout.view_share_timeline, null);
        final View container = cluster.findViewById(R.id.view_share_timeline_container);
        final TextView scoreTextView = (TextView) cluster.findViewById(R.id.view_share_timeline_score_text);
        final TextView sleptForTextView = (TextView) cluster.findViewById(R.id.view_share_timeline_slept_for_time);
        final TextView dateTextView = (TextView) cluster.findViewById(R.id.view_share_timeline_date);
        final LinearLayout sensorsLayout = (LinearLayout) cluster.findViewById(R.id.view_share_timeline_sensors);


        // Set values
        int score = 0;
        int color = ContextCompat.getColor(activity, ScoreCondition.UNAVAILABLE.colorRes);
        String scoreText = activity.getString(R.string.missing_data_placeholder);


        if (timeline.getScore() != null) {
            score = timeline.getScore();
            scoreText = Integer.toString(score);
        }
        if (timeline.getScoreCondition() != null) {
            color = ContextCompat.getColor(activity, timeline.getScoreCondition().colorRes);
        }
        final TimelineMetric timeToFallAsleepMetric = timeline.getMetricWithName(TimelineMetric.Name.TIME_TO_SLEEP);
        final TimelineMetric timeSleptMetric = timeline.getMetricWithName(TimelineMetric.Name.TOTAL_SLEEP);
        final TimelineMetric timesWokenUpMetric = timeline.getMetricWithName(TimelineMetric.Name.TIMES_AWAKE);

        if (timeToFallAsleepMetric != null && timeToFallAsleepMetric.getValue() != null && timeToFallAsleepMetric.getUnit() != null
                && timeSleptMetric != null && timeSleptMetric.getValue() != null && timeSleptMetric.getUnit() != null
                && timesWokenUpMetric != null && timesWokenUpMetric.getValue() != null && timesWokenUpMetric.getUnit() != null) {
            float timeToFallAsleepValue = timeToFallAsleepMetric.getValue();

            sleptForTextView.setText(Html.fromHtml(
                    activity.getString(R.string.share_timeline_sleep_time,
                                       getMetricTimeString(timeToFallAsleepMetric.getValue()),
                                       getMetricTimeString(timeSleptMetric.getValue()),
                                       getMetricQuanityString(timesWokenUpMetric.getValue()))));
        }

        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        for (final SensorImage sensorImage : sensors) {
            final TimelineMetric metric = timeline.getMetricWithName(sensorImage.name);
            if (metric != null) {
                final SensorConditionView sensor = new SensorConditionView(activity);
                final Drawable sensorIcon = ResourcesCompat.getDrawable(activity.getResources(), sensorImage.imageRes, null);
                final int sensorColor = ContextCompat.getColor(activity, metric.getCondition().colorRes);
                sensor.setIcon(sensorIcon);
                sensor.setTint(sensorColor);
                sensorsLayout.addView(sensor, layoutParams);
            }
        }

        final SleepScoreDrawable scoreDrawable = new SleepScoreDrawable(activity.getResources(), true);
        scoreDrawable.setValue(score);
        scoreDrawable.setStateful(true);
        scoreDrawable.setFillColor(color);
        scoreTextView.setText(scoreText);
        scoreTextView.setTextColor(color);

        dateTextView.setText(timeline.getDate().toString("MMMM dd, yyyy").toUpperCase());


        // Draw it
        cluster.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        cluster.layout(0, 0, cluster.getMeasuredWidth(), cluster.getMeasuredHeight());


        final Bitmap scoreBitmap = Bitmap.createBitmap(scoreDrawable.getIntrinsicWidth(), scoreDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas scoreCanvas = new Canvas(scoreBitmap);
        scoreDrawable.draw(scoreCanvas);

        final Bitmap bitmap = Bitmap.createBitmap(cluster.getMeasuredWidth(), cluster.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        cluster.draw(canvas);


        canvas.drawBitmap(scoreBitmap,
                          (width - scoreDrawable.getIntrinsicWidth()) / 2,
                          (scoreTextView.getBottom() / 2),
                          null);

        int sensorHorizontalMargin = activity.getResources().getDimensionPixelSize(R.dimen.timeline_shareable_sensor_horizontal_margin);
        for (int i = 0; i < sensorsLayout.getChildCount(); i++) {
            final View view = sensorsLayout.getChildAt(i);
            canvas.drawBitmap(getBitmap(view),
                              sensorsLayout.getLeft() + (i * (view.getWidth() + sensorHorizontalMargin) - sensorHorizontalMargin),
                              sensorsLayout.getBottom(),
                              null);
        }
        return bitmap;
    }

    private static String getMetricTimeString(float minutes) {
        String unit = "minutes";
        if (minutes >= 60) {
            minutes = minutes / 60;
            unit = "hour";
            if (minutes > 1) {
                unit += "s";
            }
        } else if (minutes == 1) {
            unit = "minute";
        }
        return String.format("%.02f ", minutes) + " " + unit;
    }

    private static String getMetricQuanityString(float times) {
        String unit = "time";
        if (times != 1) {
            unit += "s";
        }
        return (int)times + " " + unit;
    }

    private static Bitmap getBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
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

        SensorImage(@NonNull TimelineMetric.Name name, @DrawableRes int imageRes) {
            this.name = name;
            this.imageRes = imageRes;
        }
    }
}
