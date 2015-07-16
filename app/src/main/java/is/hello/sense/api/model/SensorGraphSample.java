package is.hello.sense.api.model;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import is.hello.sense.api.ApiService;

public class SensorGraphSample extends ApiResponse {
    @SerializedName("value")
    private float value;

    @SerializedName("datetime")
    private DateTime time;

    @SerializedName("offset_millis")
    private int offset;


    public boolean isValuePlaceholder() {
        return (value == ApiService.PLACEHOLDER_VALUE);
    }

    public float getValue() {
        return value;
    }

    public float getNormalizedValue() {
        return Math.max(0f, value);
    }

    public DateTimeZone getTimeZone() {
        return DateTimeZone.forOffsetMillis(offset);
    }

    public DateTime getShiftedTime() {
        return time.withZone(getTimeZone());
    }


    @Override
    public String toString() {
        return "SensorGraphSample{" +
                "value=" + value +
                ", shiftedTime=" + getShiftedTime() +
                '}';
    }


    //region Time Zone Fun

    /**
     * Returns the user's current time, in the UTC timezone. For use with sensor history.
     */
    public static long timeForLatest() {
        DateTime now = DateTime.now();
        return now.getMillis();
    }

    //endregion
}
