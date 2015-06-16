package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.util.markup.text.MarkupString;

public class TimelineSegment extends ApiResponse implements Cloneable {
    @JsonProperty("id")
    private String id;

    @JsonProperty("timestamp")
    private DateTime timestamp;

    @JsonProperty("duration")
    private long duration;

    @JsonProperty("message")
    private MarkupString message;

    @JsonProperty("sensors")
    @JsonDeserialize(contentAs = TimelineSensor.class)
    private List<TimelineSensor> sensors;

    @JsonProperty("sleep_depth")
    private int sleepDepth;

    @JsonProperty("event_type")
    private EventType eventType;

    @JsonProperty("offset_millis")
    private int offset;

    @JsonProperty("sound")
    private SoundInfo sound;

    @JsonIgnore
    private @Nullable DateTime shiftedTimestamp;


    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    @Override
    public TimelineSegment clone() {
        try {
            return (TimelineSegment) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getId() {
        return id;
    }

    public long getDuration() {
        return duration;
    }

    public MarkupString getMessage() {
        return message;
    }

    public List<TimelineSensor> getSensors() {
        return sensors;
    }

    public int getSleepDepth() {
        return sleepDepth;
    }

    public int getDisplaySleepDepth() {
        if (isBeforeSleep()) {
            return 0;
        } else {
            return getSleepDepth();
        }
    }

    public boolean hasEventInfo() {
        return (eventType != null && eventType != EventType.SLEEPING);
    }

    public boolean isBeforeSleep() {
        return (eventType == null ||
                eventType == EventType.IN_BED ||
                eventType == EventType.LIGHTS_OUT);
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean isTimeAdjustable() {
        return (eventType == EventType.IN_BED ||
                eventType == EventType.SLEEP ||
                eventType == EventType.WAKE_UP ||
                eventType == EventType.OUT_OF_BED);
    }

    public DateTimeZone getTimeZone() {
        return DateTimeZone.forOffsetMillis(offset);
    }

    public DateTime getShiftedTimestamp() {
        if (shiftedTimestamp == null) {
            this.shiftedTimestamp = timestamp.withZone(getTimeZone());
        }

        return shiftedTimestamp;
    }

    public TimelineSegment withTimestamp(@NonNull DateTime newTimestamp) {
        TimelineSegment copy = clone();
        copy.timestamp = newTimestamp.withZone(DateTimeZone.UTC);
        copy.shiftedTimestamp = newTimestamp.withZone(getTimeZone());
        return copy;
    }

    public LocalTime getUnshiftedTime() {
        return timestamp.toLocalTime();
    }

    public SoundInfo getSound() {
        return sound;
    }

    @Override
    public String toString() {
        return "TimelineSegment{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", duration=" + duration +
                ", message='" + message + '\'' +
                ", sensors=" + sensors +
                ", sleepDepth=" + sleepDepth +
                ", eventType=" + eventType +
                ", offset=" + offset +
                ", sound=" + sound +
                '}';
    }


    public static class SoundInfo {
        @JsonProperty("url")
        private String url;

        @JsonProperty("duration_millis")
        private Integer durationMillis;

        @JsonProperty("is_empty")
        private boolean empty;

        public String getUrl() {
            return url;
        }

        public Integer getDurationMillis() {
            return durationMillis;
        }

        public boolean isEmpty() {
            return empty;
        }

        @Override
        public String toString() {
            return "SoundInfo{" +
                    "url='" + url + '\'' +
                    ", durationMillis=" + durationMillis +
                    ", empty=" + empty +
                    '}';
        }
    }


    public enum EventType {
        MOTION(R.string.accessibility_event_name_motion),
        SLEEPING(R.string.accessibility_event_name_sleeping),
        SLEEP_MOTION(R.string.accessibility_event_name_sleep_motion),
        PARTNER_MOTION(R.string.accessibility_event_name_partner_moved),
        NOISE(R.string.accessibility_event_name_noise),
        SNORING(R.string.accessibility_event_name_snoring),
        SLEEP_TALK(R.string.accessibility_event_name_talk),
        LIGHT(R.string.accessibility_event_name_light),
        LIGHTS_OUT(R.string.accessibility_event_name_lights_out),
        SUNSET(R.string.accessibility_event_name_sunset),
        SUNRISE(R.string.accessibility_event_name_sunrise),
        IN_BED(R.string.accessibility_event_name_in_bed),
        SLEEP(R.string.accessibility_event_name_sleep),
        OUT_OF_BED(R.string.accessibility_event_name_out_of_bed),
        WAKE_UP(R.string.accessibility_event_name_wake_up),
        ALARM(R.string.accessibility_event_name_alarm),
        UNKNOWN(R.string.accessibility_event_name_unknown);

        @JsonCreator
        @SuppressWarnings("UnusedDeclaration")
        public static @Nullable EventType fromString(@NonNull String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            } else {
                return Enums.fromString(value, values(), UNKNOWN);
            }
        }

        public final @StringRes int accessibilityStringRes;

        EventType(@StringRes int accessibilityStringRes) {
            this.accessibilityStringRes = accessibilityStringRes;
        }
    }
}
