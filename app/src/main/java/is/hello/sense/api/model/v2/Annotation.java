package is.hello.sense.api.model.v2;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;


import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.TrendGraph;

public class Annotation extends ApiResponse {
    @SerializedName("title")
    private String title;

    @SerializedName("value")
    private float value;

    @SerializedName("data_type")
    private TrendGraph.DataType dataType;

    @SerializedName("condition")
    @Nullable
    private String condition;

    public String getTitle() {
        return title;
    }

    public float getValue() {
        return value;
    }

    public TrendGraph.DataType getDataType() {
        return dataType;
    }

    @Nullable
    public String getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return "Annotation{" +
                ", title='" + title + '\'' +
                ", value='" + value + '\'' +
                ", dataType='" + dataType + '\'' +
                ", condition='" + condition + '\'' +
                '}';
    }
}