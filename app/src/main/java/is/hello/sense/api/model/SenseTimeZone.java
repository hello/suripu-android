package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTimeZone;

public class SenseTimeZone extends ApiResponse {
    @SerializedName("timezone_offset")
    public final int offsetMillis;

    @SerializedName("timezone_id")
    public final String timeZoneId;

    public static @NonNull SenseTimeZone fromDefault() {
        return fromDateTimeZone(DateTimeZone.getDefault());
    }

    public static @NonNull SenseTimeZone fromDateTimeZone(@NonNull DateTimeZone timeZone) {
        return new SenseTimeZone(timeZone.getOffset(System.currentTimeMillis()), timeZone.getID());
    }

    public SenseTimeZone(int offsetMillis, String timeZoneId){
        this.offsetMillis = offsetMillis;
        this.timeZoneId = timeZoneId;
    }


    public DateTimeZone toDateTimeZone() {
        if (!TextUtils.isEmpty(timeZoneId)) {
            return DateTimeZone.forID(timeZoneId);
        } else {
            return DateTimeZone.forOffsetMillis(offsetMillis);
        }
    }


    @Override
    public String toString() {
        return "SenseTimeZone{" +
                "offsetMillis=" + offsetMillis +
                ", timeZoneId='" + timeZoneId + '\'' +
                '}';
    }
}
