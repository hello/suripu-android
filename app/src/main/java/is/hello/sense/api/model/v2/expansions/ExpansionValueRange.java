package is.hello.sense.api.model.v2.expansions;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

/**
 * Hold the range of possible accepted values for an expansion
 */

public class ExpansionValueRange extends ApiResponse {

    @SerializedName("min")
    public final int min;

    @SerializedName("max")
    public final int max;

    public ExpansionValueRange(final int min, final int max) {
        this.min = min;
        this.max = max;
    }

}
