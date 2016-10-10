package is.hello.sense.api.model.v2.sensors;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import is.hello.sense.api.model.ApiResponse;

public class X extends ApiResponse {
    @SerializedName("t")
    private long timestamp;

    @SerializedName("o")
    private int offsetMillis;

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "X{" +
                "Timestamp=" + timestamp +
                ", OffsetMillis=" + offsetMillis +
                "}";
    }


}
