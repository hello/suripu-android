package is.hello.sense.api.model.v2;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.fasterxml.jackson.annotation.JsonProperty;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Condition;

public class TimelineMetric extends ApiResponse implements Parcelable {
    @VisibleForTesting
    @JsonProperty("name")
    String name;

    @VisibleForTesting
    @JsonProperty("value")
    String value;

    @VisibleForTesting
    @JsonProperty("unit")
    String unit;

    @VisibleForTesting
    @JsonProperty("condition")
    Condition condition;


    public TimelineMetric() {

    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
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
        this.name = in.readString();
        this.value = in.readString();
        this.unit = in.readString();
        this.condition = Condition.fromString(in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeString(value);
        out.writeString(unit);
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
}
