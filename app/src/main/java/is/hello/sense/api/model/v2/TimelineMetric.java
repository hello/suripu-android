package is.hello.sense.api.model.v2;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Condition;

public class TimelineMetric extends ApiResponse implements Parcelable {
    private static final long VALUE_MISSING = Long.MIN_VALUE;

    @VisibleForTesting
    @SerializedName("name")
    Name name;

    @VisibleForTesting
    @SerializedName("value")
    @Nullable Long value;

    @VisibleForTesting
    @SerializedName("unit")
    Unit unit;

    @VisibleForTesting
    @SerializedName("condition")
    Condition condition;


    @SuppressWarnings("unused")
    public TimelineMetric() {

    }

    public Name getName() {
        return name;
    }

    public @Nullable Long getValue() {
        return value;
    }

    public Unit getUnit() {
        return unit;
    }

    public Condition getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return "TimelineMetric{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", unit='" + unit + '\'' +
                ", condition=" + condition +
                '}';
    }


    //region Parceling

    public TimelineMetric(@NonNull Parcel in) {
        this.name = Name.fromString(in.readString());
        long value = in.readLong();
        this.value = value == VALUE_MISSING ? null : value;
        this.unit = Unit.fromString(in.readString());
        this.condition = Condition.fromString(in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name.toString());
        out.writeLong(value != null ? value : VALUE_MISSING);
        out.writeString(unit.toString());
        out.writeString(condition.toString());
    }

    public static final Creator<TimelineMetric> CREATOR = new Creator<TimelineMetric>() {
        @Override
        public TimelineMetric createFromParcel(Parcel in) {
            return new TimelineMetric(in);
        }

        @Override
        public TimelineMetric[] newArray(int size) {
            return new TimelineMetric[size];
        }
    };

    //endregion


    public enum Name implements Enums.FromString {
        UNKNOWN(R.string.missing_data_placeholder),
        TOTAL_SLEEP(R.string.timeline_info_label_total_sleep),
        SOUND_SLEEP(R.string.timeline_info_label_sound_sleep),
        TIME_TO_SLEEP(R.string.timeline_info_label_time_to_sleep),
        TIMES_AWAKE(R.string.timeline_info_label_times_awake),
        FELL_ASLEEP(R.string.timeline_info_label_sleep_time),
        WOKE_UP(R.string.timeline_info_label_wake_up_time),
        TEMPERATURE(R.string.condition_temperature),
        HUMIDITY(R.string.condition_humidity),
        PARTICULATES(R.string.condition_airquality),
        SOUND(R.string.condition_sound),
        LIGHT(R.string.condition_light);

        public final @StringRes int stringRes;

        Name(@StringRes int stringRes) {
            this.stringRes = stringRes;
        }

        public static Name fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.getDefault());
        }
    }

    public enum Unit implements Enums.FromString {
        MINUTES,
        QUANTITY,
        TIMESTAMP,
        CONDITION;

        public static Unit fromString(@NonNull String string) {
            return Enums.fromString(string, values(), QUANTITY);
        }
    }
}
