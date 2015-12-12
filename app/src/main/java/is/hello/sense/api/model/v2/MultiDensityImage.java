package is.hello.sense.api.model.v2;


import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

public class MultiDensityImage extends ApiResponse {
    @SerializedName("phone_1x")
    private String phone1x;

    @SerializedName("phone_2x")
    private String phone2x;

    @SerializedName("phone_3x")
    private String phone3x;

    public String getUrl(@NonNull Resources resources) {
        final float density = resources.getDisplayMetrics().density;
        if (density >= 3f) {
            return phone3x;
        } else if (density >= 1.5f) {
            return phone2x;
        } else {
            return phone1x;
        }
    }

    @Override
    public String toString() {
        return "MultiDensityImage{" +
                "phone1x=" + phone1x +
                ", phone2x='" + phone2x + '\'' +
                ", phone3x='" + phone3x + '\'' +
                '}';
    }
}
