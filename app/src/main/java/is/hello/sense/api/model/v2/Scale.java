package is.hello.sense.api.model.v2;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

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

    public boolean containsValue(final double value) {
        if (min == null && value < max) {
            return true;
        } else if (max == null && value > min) {
            return true;
        }
        if (min == null || max == null) {
            return false;
        }
        return min < value && max > value;
    }

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
