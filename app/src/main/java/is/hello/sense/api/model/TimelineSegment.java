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

import java.util.List;

import is.hello.sense.R;

public class TimelineSegment extends ApiResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("timestamp")
    private DateTime timestamp;

    @JsonProperty("duration")
    private long duration;

    @JsonProperty("message")
    private String message;

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


    public String getId() {
        return id;
    }

    public long getDuration() {
        return duration;
    }

    public String getMessage() {
        return message;
    }

    public List<TimelineSensor> getSensors() {
        return sensors;
    }

    public int getSleepDepth() {
        return sleepDepth;
    }

    public boolean hasEventInfo() {
        return (eventType != null && eventType != EventType.SLEEPING);
    }

    public boolean isBeforeSleep() {
        return (eventType == null);
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


    public static enum EventType {
        MOTION(R.string.event_type_motion),
        SLEEPING(R.string.event_type_sleeping),
        SLEEP_MOTION(R.string.event_type_sleep_motion),
        PARTNER_MOTION(R.string.event_type_partner_moved),
        NOISE(R.string.event_type_noise),
        SNORING(R.string.event_type_snoring),
        SLEEP_TALK(R.string.event_type_sleep_talk),
        LIGHT(R.string.event_type_light),
        LIGHTS_OUT(R.string.event_type_lights_out),
        SUNSET(R.string.event_type_sunset),
        SUNRISE(R.string.event_type_sunrise),
        IN_BED(R.string.event_type_in_bed),
        SLEEP(R.string.event_type_sleep),
        OUT_OF_BED(R.string.event_type_out_of_bed),
        WAKE_UP(R.string.event_type_wake_up),
        ALARM(R.string.event_type_alarm),
        UNKNOWN(R.string.event_type_unknown);

        @JsonCreator
        @SuppressWarnings("UnusedDeclaration")
        public static @Nullable EventType fromString(@NonNull String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            } else {
                return Enums.fromString(value, values(), UNKNOWN);
            }
        }

        public final @StringRes int nameString;

        private EventType(@StringRes int nameString) {
            this.nameString = nameString;
        }
    }
}
