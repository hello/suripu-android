package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;

@Singleton public class DateFormatter {
    private final Context context;

    private final DateTimeFormatter dateFormat;
    private final DateTimeFormatter timeFormat;

    @Inject public DateFormatter(@NonNull Context context) {
        this.context = context.getApplicationContext();

        this.dateFormat = DateTimeFormat.forPattern(context.getString(R.string.format_date));
        this.timeFormat = DateTimeFormat.forPattern(context.getString(R.string.format_time));
    }

    public @NonNull String formatAsTimelineDate(@Nullable DateTime date) {
        return formatAsDate(date);
    }

    public @NonNull String formatAsDate(@Nullable DateTime date) {
        if (date != null) {
            return dateFormat.print(date);
        }
        return context.getString(R.string.format_date_placeholder);
    }

    public @NonNull String formatAsTime(@Nullable DateTime date) {
        if (date != null) {
            return timeFormat.print(date);
        }
        return context.getString(R.string.format_date_placeholder);
    }
}
