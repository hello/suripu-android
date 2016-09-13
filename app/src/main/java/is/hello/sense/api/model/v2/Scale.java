package is.hello.sense.api.model.v2;

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
