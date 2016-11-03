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
}
