package is.hello.sense.api.model.v2;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;

public class Scale implements Serializable {
    @SerializedName("min")
    private Double min;

    @SerializedName("max")
    private Double max;

    @SerializedName("name")
    private String name;

    @SerializedName("condition")
    private Condition condition;

    public Condition getCondition() {
        return condition;
    }

    public String getName() {
        return name;
    }

    public Double getMax() {
        return max;
    }

    public Double getMin() {
        return min;
    }

    @NonNull
    public final String getScaleViewValueText(@NonNull final Resources resources) {
        if (min == null || min <= 0) {
            if (max != null) {
                return resources.getString(R.string.sensor_scale_min_value, format(max));
            }
        } else if (max != null) {
            return resources.getString(R.string.sensor_scale_mid_value, format(min), format(max));
        } else {
            return resources.getString(R.string.sensor_scale_max_value, format(min));
        }
        return "";
    }

    private String format(final double value) {
        return String.format("%.0f", value);
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Scale) {
            final Scale scale = (Scale) obj;
            return isEqual(this.min, scale.min) &&
                    isEqual(this.max, scale.max) &&
                    isEqual(this.name, scale.name) &&
                    isEqual(this.condition, scale.condition);
        }
        return false;
    }

    private boolean isEqual(@Nullable final Object obj1, @Nullable final Object obj2) {
        if (obj1 == null) {
            if (obj2 != null) {
                return false;
            }
        } else if (obj2 == null) {
            return false;
        } else if (!obj1.equals(obj2)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Scale{" +
                "Name-" + name +
                "Max=" + max +
                "Min=" + min +
                "Condition" + condition +
                "}";
    }
}
