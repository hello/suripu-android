package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Condition;

public class Annotation extends ApiResponse {
    @SerializedName("title")
    private String title;

    @SerializedName("value")
    private float value;

    @SerializedName("data_type")
    private Graph.DataType dataType;

    @SerializedName("condition")
    private Condition condition;

    public String getTitle() {
        return title;
    }

    public float getValue() {
        return value;
    }

    public Graph.DataType getDataType() {
        return dataType;
    }

    public Condition getCondition() {
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