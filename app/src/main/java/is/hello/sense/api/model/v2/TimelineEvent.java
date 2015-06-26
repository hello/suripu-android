package is.hello.sense.api.model.v2;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.concurrent.TimeUnit;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Enums;
import is.hello.sense.util.markup.text.MarkupString;

public class TimelineEvent extends ApiResponse {
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private DateTime timestamp;

    @JsonProperty("timezone_offset")
    private int timezoneOffset;

    @JsonProperty("duration_millis")
    private long durationMillis;

    @JsonProperty("message")
    private MarkupString message;

    @JsonProperty("sleep_depth")
    private int sleepDepth;

    @JsonProperty("sleep_state")
    private SleepState sleepState;

    @JsonProperty("event_type")
    private Type eventType;


    public DateTime getTimestamp() {
        return timestamp;
    }

    public DateTimeZone getTimezone() {
        return DateTimeZone.forOffsetMillis(timezoneOffset);
    }

    public DateTime getShiftedTimestamp() {
        return timestamp.withZone(getTimezone());
    }

    public long getDuration(@NonNull TimeUnit timeUnit) {
        return timeUnit.convert(durationMillis, TimeUnit.MILLISECONDS);
    }

    public MarkupString getMessage() {
        return message;
    }

    public int getSleepDepth() {
        return sleepDepth;
    }

    public SleepState getSleepState() {
        return sleepState;
    }

    public Type getEventType() {
        return eventType;
    }


    @Override
    public String toString() {
        return "TimelineEvent{" +
                "timestamp=" + timestamp +
                ", timezoneOffset=" + timezoneOffset +
                ", durationMillis=" + durationMillis +
                ", message=" + message +
                ", sleepDepth=" + sleepDepth +
                ", sleepState='" + sleepState + '\'' +
                ", eventType='" + eventType + '\'' +
                '}';
    }


    public enum SleepState {
        AWAKE(R.string.sleep_depth_awake),
        LIGHT_SLEEP(R.string.sleep_depth_light),
        MEDIUM_SLEEP(R.string.sleep_depth_intermediate),
        DEEP_SLEEP(R.string.sleep_depth_deep);

        public final @StringRes int stringRes;

        SleepState(@StringRes int stringRes) {
            this.stringRes = stringRes;
        }

        @JsonCreator
        public static SleepState fromString(@NonNull String string) {
            return Enums.fromString(string, values(), AWAKE);
        }
    }

    public enum Type {
        UNKNOWN(R.drawable.timeline_unknown, R.string.accessibility_event_name_unknown);

        public final @DrawableRes int iconDrawableRes;
        public final @StringRes int accessibilityStringRes;

        Type(@DrawableRes int iconDrawableRes,
             @StringRes int accessibilityStringRes) {
            this.iconDrawableRes = iconDrawableRes;
            this.accessibilityStringRes = accessibilityStringRes;
        }

        @JsonCreator
        public static Type fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }
}
