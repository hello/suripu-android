package is.hello.sense.api.model.v2;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Enums;
import is.hello.sense.functional.Lists;
import is.hello.sense.util.markup.text.MarkupString;

public class TimelineEvent extends ApiResponse {
    @VisibleForTesting
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    DateTime timestamp;

    @VisibleForTesting
    @JsonProperty("timezone_offset")
    int timezoneOffset;

    @VisibleForTesting
    @JsonProperty("duration_millis")
    long durationMillis;

    @VisibleForTesting
    @JsonProperty("message")
    MarkupString message;

    @VisibleForTesting
    @JsonProperty("sleep_depth")
    int sleepDepth;

    @VisibleForTesting
    @JsonProperty("sleep_state")
    SleepState sleepState;

    @VisibleForTesting
    @JsonProperty("event_type")
    Type type;

    @VisibleForTesting
    @JsonProperty("valid_actions")
    ArrayList<Action> validActions;


    public DateTimeZone getTimezone() {
        return DateTimeZone.forOffsetMillis(timezoneOffset);
    }

    public DateTime getRawTimestamp() {
        return timestamp;
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

    public Type getType() {
        return type;
    }

    @JsonIgnore
    public boolean hasInfo() {
        return (type != Type.IN_BED);
    }

    @JsonIgnore
    public boolean hasSound() {
        return false;
    }

    @JsonIgnore
    public String getSoundUrl() {
        return null;
    }

    public boolean hasActions() {
        return !Lists.isEmpty(validActions);
    }

    public boolean supportsAction(@NonNull Action action) {
        return (validActions != null && validActions.contains(action));
    }


    @Override
    public String toString() {
        return "TimelineEvent{" +
                "timestamp=" + getShiftedTimestamp() +
                ", message=" + message +
                ", sleepDepth=" + sleepDepth +
                ", sleepState='" + sleepState + '\'' +
                ", type='" + type + '\'' +
                '}';
    }


    public enum SleepState {
        AWAKE(R.color.sleep_awake, R.string.sleep_depth_awake),
        LIGHT(R.color.sleep_light, R.string.sleep_depth_light),
        MEDIUM(R.color.sleep_medium, R.string.sleep_depth_medium),
        SOUND(R.color.sleep_sound, R.string.sleep_depth_sound);

        public final @ColorRes int colorRes;
        public final @StringRes int stringRes;

        SleepState(@ColorRes int colorRes,
                   @StringRes int stringRes) {
            this.colorRes = colorRes;
            this.stringRes = stringRes;
        }

        @JsonCreator
        public static SleepState fromString(@NonNull String string) {
            return Enums.fromString(string, values(), AWAKE);
        }
    }

    public enum Action {
        ADJUST_TIME,
        VERIFY,
        REMOVE,
        INCORRECT,
    }

    public enum Type {
        IN_BED(0, 0),
        GENERIC_MOTION(R.drawable.timeline_generic_motion, R.string.accessibility_event_name_generic_motion),
        PARTNER_MOTION(R.drawable.timeline_partner, R.string.accessibility_event_name_partner_moved),
        GENERIC_SOUND(R.drawable.timeline_sound, R.string.accessibility_event_name_noise),
        SNORED(R.drawable.timeline_sound, R.string.accessibility_event_name_snoring),
        SLEEP_TALKED(R.drawable.timeline_sound, R.string.accessibility_event_name_talk),
        LIGHT(R.drawable.timeline_light, R.string.accessibility_event_name_light),
        LIGHTS_OUT(R.drawable.timeline_lights_out, R.string.accessibility_event_name_lights_out),
        SUNSET(R.drawable.timeline_sunset, R.string.accessibility_event_name_sunset),
        SUNRISE(R.drawable.timeline_sunrise, R.string.accessibility_event_name_sunrise),
        GOT_IN_BED(R.drawable.timeline_got_in_bed, R.string.accessibility_event_name_got_in_bed),
        FELL_ASLEEP(R.drawable.timeline_fell_asleep, R.string.accessibility_event_name_fell_asleep),
        GOT_OUT_OF_BED(R.drawable.timeline_got_out_of_bed, R.string.accessibility_event_name_got_out_of_bed),
        WOKE_UP(R.drawable.timeline_woke_up, R.string.accessibility_event_name_woke_up),
        ALARM_RANG(R.drawable.timeline_alarm_rang, R.string.accessibility_event_name_alarm_rang),
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

    public static class TimeAmendment {
        @JsonProperty("new_event_time")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.TIME_FORMAT)
        public final LocalTime newTime;

        @JsonProperty("timezone_offset")
        public final long timeZoneOffset;

        public TimeAmendment(@NonNull LocalTime newTime, long timeZoneOffset) {
            this.newTime = newTime;
            this.timeZoneOffset = timeZoneOffset;
        }

        @Override
        public String toString() {
            return "TimeAmendment{" +
                    "newTime=" + newTime +
                    '}';
        }
    }
}
