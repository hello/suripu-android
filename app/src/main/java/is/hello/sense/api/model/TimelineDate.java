package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimelineDate extends ApiResponse implements Serializable {
    private static final SimpleDateFormat TODAY_FORMAT = new SimpleDateFormat("M-d-yy");

    public final String month;
    public final String day;
    public final String year;

    public static @Nullable TimelineDate fromString(@Nullable String rawString) {
        if (TextUtils.isEmpty(rawString))
            return null;

        String[] pieces = TextUtils.split(rawString, "-");
        if (pieces.length != 3)
            return null;

        return new TimelineDate(pieces[0], pieces[1], pieces[2]);
    }

    public static @NonNull TimelineDate today() {
        TODAY_FORMAT.setTimeZone(TimeZone.getDefault());
        String now = TODAY_FORMAT.format(new Date());
        TimelineDate date = TimelineDate.fromString(now);
        if (date == null)
            throw new IllegalStateException("Date parsing should never fail on today's date");

        return date;
    }

    public TimelineDate(@NonNull String month,
                        @NonNull String day,
                        @NonNull String year) {
        if (year.length() != 2)
            throw new IllegalArgumentException("year malformed");

        this.year = year;
        this.day = day;
        this.month = month;
    }

    @Override
    public String toString() {
        return month + "-" + day + "-" + year;
    }
}
