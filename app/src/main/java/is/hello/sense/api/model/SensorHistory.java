package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class SensorHistory extends ApiResponse {
    public static final String SENSOR_NAME_TEMPERATURE = "temperature";
    public static final String SENSOR_NAME_HUMIDITY = "humidity";
    public static final String SENSOR_NAME_PARTICULATES = "particulates";

    @JsonProperty("value")
    private float value;

    @JsonProperty("datetime")
    private DateTime time;

    @JsonProperty("offset_millis")
    private long offset;


    public float getValue() {
        return value;
    }

    public DateTime getTime() {
        return time;
    }

    public long getOffset() {
        return offset;
    }


    @Override
    public String toString() {
        return "SensorHistory{" +
                "value=" + value +
                ", time=" + time +
                ", offset=" + offset +
                '}';
    }


    //region Time Zone Fun

    /**
     * Returns the user's current time, in the UTC timezone. For use with sensor history.
     */
    public static long currentTimeMillisShifted() {
        DateTime now = DateTime.now();
        DateTime nowUTC = new DateTime(
                now.getYear(),
                now.getMonthOfYear(),
                now.getDayOfMonth(),
                now.getHourOfDay(),
                now.getMinuteOfHour(),
                now.getSecondOfMinute(),
                DateTimeZone.UTC
        );
        return nowUTC.getMillis();
    }

    //endregion
}
