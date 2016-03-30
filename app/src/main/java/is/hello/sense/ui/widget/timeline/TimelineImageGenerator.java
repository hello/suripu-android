package is.hello.sense.ui.widget.timeline;


import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.util.Log;
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
import is.hello.sense.ui.widget.util.Styles;

public class TimelineImageGenerator {

    public static Bitmap createShareableTimeline(@NonNull final Activity activity, @Nullable final Timeline timeline) {
        if (timeline == null) {
            return null;
        }
        Resources resources = activity.getResources();
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
        final View cluster = LayoutInflater.from(activity).inflate(R.layout.view_share_timeline, null);
        final View container = cluster.findViewById(R.id.view_share_timeline_container);
        final TextView sleptForTextView = (TextView) cluster.findViewById(R.id.view_share_timeline_slept_for_time);
        final TextView dateTextView = (TextView) cluster.findViewById(R.id.view_share_timeline_date);
        final LinearLayout sensorsLayout = (LinearLayout) cluster.findViewById(R.id.view_share_timeline_sensors);
        final TextView scoreTextView = new TextView(activity);
        Styles.setTextAppearance(scoreTextView, R.style.AppTheme_Text_Score_Share_Title);
        scoreTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final int sensorHorizontalMargin = resources.getDimensionPixelSize(R.dimen.timeline_shareable_sensor_horizontal_margin);
        final int scoreTopMargin = resources.getDimensionPixelSize(R.dimen.timeline_shareable_score_top_margin);
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);


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
            sleptForTextView.setText(Html.fromHtml(
                    resources.getString(R.string.share_timeline_sleep_time,
                                        getMetricTimeString(resources, timeToFallAsleepMetric.getValue()),
                                        getMetricTimeString(resources, timeSleptMetric.getValue()),
                                        getMetricQuanityString(resources, timesWokenUpMetric.getValue()))));
        }

        for (final SensorImage sensorImage : sensors) {
            final TimelineMetric metric = timeline.getMetricWithName(sensorImage.name);
            if (metric != null) {
                final SensorConditionView sensor = new SensorConditionView(activity);
                final Drawable sensorIcon = ResourcesCompat.getDrawable(resources, sensorImage.imageRes, null);
                if (sensorIcon != null) {
                    final int sensorColor = ContextCompat.getColor(activity, metric.getCondition().colorRes);
                    sensor.setIcon(sensorIcon);
                    sensor.setTint(sensorColor);
                    sensorsLayout.addView(sensor, layoutParams);
                }
            }
        }

        final SleepScoreDrawable scoreDrawable = new SleepScoreDrawable(resources, true);
        scoreDrawable.setValue(score);
        scoreDrawable.setStateful(true);
        scoreDrawable.setFillColor(color);
        scoreTextView.setText(scoreText);
        scoreTextView.setTextColor(color);

        dateTextView.setText(timeline.getDate().toString("MMMM dd, yyyy").toUpperCase());


        // Draw it
        cluster.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        cluster.layout(0, 0, cluster.getMeasuredWidth(), cluster.getMeasuredHeight());
        scoreTextView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        scoreTextView.layout(0, 0, scoreTextView.getMeasuredWidth(), scoreTextView.getMeasuredHeight());


        final Bitmap scoreBitmap = Bitmap.createBitmap(scoreDrawable.getIntrinsicWidth(), scoreDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas scoreCanvas = new Canvas(scoreBitmap);
        scoreDrawable.draw(scoreCanvas);

        final Bitmap bitmap = Bitmap.createBitmap(cluster.getMeasuredWidth(), cluster.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        cluster.draw(canvas);

        final Bitmap scoreTextBitmap = getBitmap(scoreTextView);

        canvas.drawBitmap(scoreBitmap,
                          (width - scoreDrawable.getIntrinsicWidth()) / 2,
                          (container.getTop()),
                          null);

        canvas.drawBitmap(scoreTextBitmap,
                          (width - scoreTextBitmap.getWidth()) / 2,
                          container.getTop() + scoreTopMargin,
                          null);
        for (int i = 0; i < sensorsLayout.getChildCount(); i++) {
            final View view = sensorsLayout.getChildAt(i);
            canvas.drawBitmap(getBitmap(view),
                              sensorsLayout.getLeft() + (i * (view.getWidth() + sensorHorizontalMargin) - sensorHorizontalMargin / 2),
                              sensorsLayout.getTop() + view.getMeasuredHeight() / 2,
                              null);
        }

        return bitmap;
    }

    private static String getMetricTimeString(@NonNull Resources resources, float minutes) {
        int unitRes = R.plurals.minutes;
        if (minutes >= 60) {
            minutes = minutes / 60;
            unitRes = R.plurals.hours;
        }
        String result = String.format("%.01f", minutes);
        result = Styles.stripTrailingPeriods(Styles.stripTrailingZeros(result));
        return result + " " + resources.getQuantityString(unitRes, (int) Math.ceil(minutes));
    }

    private static String getMetricQuanityString(@NonNull Resources resources, float times) {
        return Styles.stripTrailingPeriods(Styles.stripTrailingZeros(Float.toString(times))) + " " + resources.getQuantityString(R.plurals.times, (int) times);
    }

    private static Bitmap getBitmap(@NonNull View view) {
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
