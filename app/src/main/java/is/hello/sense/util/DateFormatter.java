package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;

@Singleton public class DateFormatter {
    private final Context context;

    @Inject public DateFormatter(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public static boolean isLastNight(@NonNull DateTime instant) {
        Interval interval = new Interval(Days.ONE, DateTime.now().withTimeAtStartOfDay());
        return interval.contains(instant);
    }

    public static @NonNull DateTime lastNight() {
        return DateTime.now().minusDays(1);
    }

    public @NonNull String formatAsTimelineDate(@Nullable DateTime date) {
        if (date != null && isLastNight(date))
            return context.getString(R.string.format_date_last_night);
        else
            return formatAsDate(date);
    }

    public @NonNull String formatAsBirthDate(@Nullable LocalDate date) {
        if (date != null) {
            return date.toString(context.getString(R.string.format_birth_date));
        } else {
            return context.getString(R.string.format_date_placeholder);
        }
    }

    public @NonNull String formatAsDate(@Nullable DateTime date) {
        if (date != null) {
            return date.toString(context.getString(R.string.format_date));
        } else {
            return context.getString(R.string.format_date_placeholder);
        }
    }

    public @NonNull String formatAsTime(@Nullable LocalDateTime date, boolean use24Time) {
        if (date != null) {
            if (use24Time)
                return date.toString(context.getString(R.string.format_time_24_hr));
            else
                return date.toString(context.getString(R.string.format_time_12_hr));
        }
        return context.getString(R.string.format_date_placeholder);
    }

    public @NonNull String formatAsTime(@Nullable LocalTime time, boolean use24Time) {
        if (time != null) {
            if (use24Time)
                return time.toString(context.getString(R.string.format_time_24_hr));
            else
                return time.toString(context.getString(R.string.format_time_12_hr));
        }
        return context.getString(R.string.format_date_placeholder);
    }
}
