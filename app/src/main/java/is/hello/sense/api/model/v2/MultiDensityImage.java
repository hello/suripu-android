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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MultiDensityImage that = (MultiDensityImage) o;

        if (phone1x != null ? !phone1x.equals(that.phone1x) : that.phone1x != null) return false;
        if (phone2x != null ? !phone2x.equals(that.phone2x) : that.phone2x != null) return false;
        return !(phone3x != null ? !phone3x.equals(that.phone3x) : that.phone3x != null);

    }

    @Override
    public int hashCode() {
        int result = phone1x != null ? phone1x.hashCode() : 0;
        result = 31 * result + (phone2x != null ? phone2x.hashCode() : 0);
        result = 31 * result + (phone3x != null ? phone3x.hashCode() : 0);
        return result;
    }
}
