package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.google.gson.annotations.SerializedName;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import is.hello.sense.util.Constants;

public class NotificationSetting extends ApiResponse {
    @NonNull
    @SerializedName("name")
    private final String name;

    @NonNull
    @SerializedName("type")
    private final String type;

    @SerializedName("enabled")
    private final boolean enabled;

    @Nullable
    @SerializedName("schedule")
    private final NotificationSchedule schedule;

    public NotificationSetting(@Nullable final String name,
                               @Nullable final String type,
                               final boolean enabled,
                               @Nullable final NotificationSchedule schedule) {
        this.name = name == null ? Constants.EMPTY_STRING : name;
        this.type = typeFromString(type);
        this.enabled = enabled;
        this.schedule = schedule;
    }

    @Override
    public String toString() {
        return "NotificationSetting{" +
                "name='" + name + '\'' +
                ", typed='" + type + '\'' +
                ", enabled=" + enabled +
                ", schedule=" + (schedule == null ? "null" : schedule.toString()) +
                '}';
    }

    @Type
    private static String typeFromString(@Nullable final String string) {
        if (string == null) {
            return UNKNOWN;
        }
        final int index = TYPES.indexOf(string.toUpperCase());
        if (index != Constants.NONE) {
            @Type final String type = TYPES.get(index);
            return type;

        }
        return UNKNOWN;
    }


    private static final String SLEEP_SCORE = "sleep_score";
    private static final String SYSTEM = "system";
    private static final String SLEEP_REMINDER = "sleep_reminder";
    private static final String UNKNOWN = "UNKNOWN";
    private static final List<String> TYPES = new ArrayList<String>(4) {{
        add(SLEEP_SCORE);
        add(SYSTEM);
        add(SLEEP_REMINDER);
        add(UNKNOWN);
    }};

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            SLEEP_SCORE,
            SYSTEM,
            SLEEP_REMINDER,
            UNKNOWN
    })
    @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    public @interface Type {
    }

}
