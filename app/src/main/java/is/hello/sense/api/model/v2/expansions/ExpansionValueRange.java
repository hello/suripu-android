package is.hello.sense.api.model.v2.expansions;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

/**
 * Hold the range of possible accepted values for an expansion
 */

public class ExpansionValueRange extends ApiResponse {

    @SerializedName("min")
    public final float min;

    @SerializedName("max")
    public final float max;

    public ExpansionValueRange(final float min, final float max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public String toString() {
        return "ExpansionValueRange{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }

    public boolean hasSameValues() {
        return min == max;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ExpansionValueRange that = (ExpansionValueRange) o;

        if (Float.compare(that.min, min) != 0) return false;
        return Float.compare(that.max, max) == 0;

    }

    @Override
    public int hashCode() {
        int result = (min != +0.0f ? Float.floatToIntBits(min) : 0);
        result = 31 * result + (max != +0.0f ? Float.floatToIntBits(max) : 0);
        return result;
    }
}
