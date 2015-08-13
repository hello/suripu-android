package is.hello.sense.rating;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;

public class LocalUsageTracker {
    public static final Days OLDEST_DAY = Days.days(31);

    public DateTime today() {
        return DateTime.now().withTimeAtStartOfDay();
    }

    public void reset() {
    }

    public void increment(@NonNull Identifier identifier) {
    }

    public int usageWithin(@NonNull Identifier identifier,
                           @NonNull Interval interval) {
        return -1;
    }

    public void collect() {
    }


    public enum Identifier {
        SYSTEM_ALERT_SHOWN,
        APP_LAUNCHED,
        TIMELINE_SHOWN_WITH_DATA,
    }
}
