package is.hello.sense.api.model;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.joda.time.LocalDateTime;

import java.util.List;

import is.hello.sense.R;

public class TimelineSegment extends ApiResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private LocalDateTime timestamp;

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
    private long offset;

    @JsonProperty("sound")
    private SoundInfo sound;


    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
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

    public EventType getEventType() {
        return eventType;
    }

    public long getOffset() {
        return offset;
    }

    public SoundInfo getSound() {
        return sound;
    }

    @Override
    public String toString() {
        return "TimelineSegment{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                ", sleepDepth=" + sleepDepth +
                ", offset=" + offset +
                '}';
    }


    public static class SoundInfo {
        @JsonProperty("url")
        private String url;

        @JsonProperty("durationMillis")
        private Integer durationMillis;

        @JsonProperty("empty")
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
        MOTION(R.drawable.movement_medium, R.string.event_type_motion),
        NOISE(R.drawable.sound_medium, R.string.event_type_noise),
        SNORING(R.drawable.sound_medium, R.string.event_type_snoring),
        SLEEP_TALK(R.drawable.sound_medium, R.string.event_type_sleep_talk),
        LIGHT(R.drawable.light_medium, R.string.event_type_light),
        LIGHTS_OUT(R.drawable.light_good, R.string.event_type_lights_out),
        SLEEP_MOTION(R.drawable.movement_medium, R.string.event_type_sleep_motion),
        SLEEP(R.drawable.asleep, R.string.event_type_sleep),
        SUNSET(R.drawable.sunset, R.string.event_type_sunset),
        SUNRISE(R.drawable.sunrise, R.string.event_type_sunrise),
        PARTNER_MOTION(R.drawable.movement_medium, R.string.event_type_partner_moved),
        WAKE_UP(R.drawable.wakeup, R.string.event_type_wake_up),
        UNKNOWN(R.drawable.movement_medium, R.string.event_type_unknown);

        @JsonCreator
        @SuppressWarnings("UnusedDeclaration")
        public static @Nullable EventType fromString(@NonNull String value) {
            if (TextUtils.isEmpty(value))
                return null;
            else
                return Enums.fromString(value, values(), UNKNOWN);
        }

        public final @DrawableRes int iconRes;
        public final @StringRes int nameString;

        private EventType(@DrawableRes int iconRes, @StringRes int nameString) {
            this.iconRes = iconRes;
            this.nameString = nameString;
        }
    }
}
