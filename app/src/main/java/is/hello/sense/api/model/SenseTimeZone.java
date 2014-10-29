package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTimeZone;

public class SenseTimeZone extends ApiResponse {
    @JsonProperty("timezone_offset")
    public final int offsetMillis;

    @JsonProperty("timezone_id")
    public final String timeZoneId;

    public static @NonNull SenseTimeZone fromDefault() {
        return fromDateTimeZone(DateTimeZone.getDefault());
    }

    public static @NonNull SenseTimeZone fromDateTimeZone(@NonNull DateTimeZone timeZone) {
        return new SenseTimeZone(timeZone.getOffset(System.currentTimeMillis()), timeZone.getID());
    }

    @JsonCreator
    public SenseTimeZone(@JsonProperty("timezone_offset") int offsetMillis,
                         @JsonProperty("timezone_id") final String timeZoneId){
        this.offsetMillis = offsetMillis;
        this.timeZoneId = timeZoneId;
    }


    @Override
    public String toString() {
        return "SenseTimeZone{" +
                "offsetMillis=" + offsetMillis +
                ", timeZoneId='" + timeZoneId + '\'' +
                '}';
    }
}
