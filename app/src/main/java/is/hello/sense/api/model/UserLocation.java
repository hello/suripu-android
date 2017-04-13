package is.hello.sense.api.model;


import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class UserLocation implements Serializable {
    @SerializedName("latitude")
    private final double latitude;

    @SerializedName("longitude")
    private final double longitude;

    public UserLocation(@Nullable final Double latitude,
                        @Nullable final Double longitude) {
        this.latitude = latitude == null ? 0 : latitude;
        this.longitude = longitude == null ? 0 : longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return "UserLocation{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
