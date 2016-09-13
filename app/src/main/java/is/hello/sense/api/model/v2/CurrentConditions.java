package is.hello.sense.api.model.v2;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import is.hello.sense.api.gson.Enums;

public class CurrentConditions implements Serializable {
    @SerializedName("status")
    private Status status;

    @SerializedName("sensors")
    private List<Sensor> sensors;

    @Override
    public String toString() {
        return "CurrentConditions{" +
                "Status=" + status.toString() +
                "}";
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public enum Status implements Enums.FromString {
        OK,
        NO_SENSE,
        WAITING_FOR_DATA;

        public static Status fromString(@Nullable final String string) {
            return Enums.fromString(string, values(), OK);
        }
    }
}
