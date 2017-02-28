package is.hello.sense.api.model;

import com.google.gson.annotations.SerializedName;

public class NotificationSchedule extends ApiResponse {

    @SerializedName("hour")
    private final int hour;
    @SerializedName("minute")
    private final int minute;

    public NotificationSchedule(final int hour,
                                final int minute) {
        this.hour = hour;
        this.minute = minute;
    }
}
