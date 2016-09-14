package is.hello.sense.api.model.v2.sensors;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class X implements Serializable {
    @SerializedName("t")
    private long timestamp;

    @SerializedName("o")
    private int offsetMillis;


    @Override
    public String toString() {
        return "X{" +
                "Timestamp=" + timestamp +
                ", OffsetMillis=" + offsetMillis +
                "}";
    }


}
