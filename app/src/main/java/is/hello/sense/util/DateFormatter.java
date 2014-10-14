package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;

@Singleton public class DateFormatter {
    private final Context context;

    private final DateTimeFormatter dateFormat;
    private final DateTimeFormatter timeFormat12Hour;
    private final DateTimeFormatter timeFormat24Hour;

    @Inject public DateFormatter(@NonNull Context context) {
        this.context = context.getApplicationContext();

        this.dateFormat = DateTimeFormat.forPattern(context.getString(R.string.format_date));
        this.timeFormat12Hour = DateTimeFormat.forPattern(context.getString(R.string.format_time_12_hr));
        this.timeFormat24Hour = DateTimeFormat.forPattern(context.getString(R.string.format_time_24_hr));
    }

    public static boolean isToday(@NonNull ReadableInstant instant) {
        Interval interval = new Interval(DateTime.now().withTimeAtStartOfDay(), Days.ONE);
        return interval.contains(instant);
    }

    public @NonNull String formatAsTimelineDate(@Nullable ReadableInstant date) {
        if (date != null && isToday(date))
            return context.getString(R.string.format_date_last_night);
        else
            return formatAsDate(date);
    }

    public @NonNull String formatAsDate(@Nullable ReadablePartial date) {
        if (date != null) {
            return dateFormat.print(date);
        }
        return context.getString(R.string.format_date_placeholder);
    }

    public @NonNull String formatAsDate(@Nullable ReadableInstant date) {
        if (date != null) {
            return dateFormat.print(date);
        }
        return context.getString(R.string.format_date_placeholder);
    }

    public @NonNull String formatAsTime(@Nullable ReadablePartial date, boolean use24Time) {
        if (date != null) {
            if (use24Time)
                return timeFormat24Hour.print(date);
            else
                return timeFormat12Hour.print(date);
        }
        return context.getString(R.string.format_date_placeholder);
    }

    public @NonNull String formatAsTime(@Nullable ReadableInstant date, boolean use24Time) {
        if (date != null) {
            if (use24Time)
                return timeFormat24Hour.print(date);
            else
                return timeFormat12Hour.print(date);
        }
        return context.getString(R.string.format_date_placeholder);
    }
}
