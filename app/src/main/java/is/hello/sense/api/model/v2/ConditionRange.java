package is.hello.sense.api.model.v2;

import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;


import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Condition;

public class ConditionRange extends ApiResponse {
    @VisibleForTesting
    @SerializedName("min_value")
    private int minValue;

    @VisibleForTesting
    @SerializedName("max_value")
    private int maxValue;

    @VisibleForTesting
    @SerializedName("condition")
    private Condition condition;

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public Condition getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return "ConditionRange{" +
                ", minValue='" + minValue + '\'' +
                ", maxValue='" + maxValue + '\'' +
                ", condition='" + condition + '\'' +
                '}';
    }
}