package is.hello.sense.api.model.v2;


import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.api.model.ApiResponse;

public class GraphSection extends ApiResponse {
    @SerializedName("values")
    private List<Float> values;

    @SerializedName("titles")
    private List<String> titles;

    @SerializedName("highlighted_values")
    private List<Integer> highlightedValues;

    @SerializedName("highlighted_title")
    @Nullable
    private Integer highlightedTitle;

    public List<Float> getValues() {
        return values;
    }

    public List<String> getTitles() {
        return titles;
    }

    public List<Integer> getHighlightedValues() {
        return highlightedValues;
    }

    @Nullable
    public Integer getHighlightedTitle() {
        return highlightedTitle;
    }

    @Override
    public String toString() {
        return "GraphSection{" +
                ", values='" + values.toString() + '\'' +
                ", titles='" + titles.toString() + '\'' +
                ", highlightedValues='" +highlightedValues.toString() + '\'' +
                ", highlightedTitle='" + highlightedTitle + '\'' +
                '}';
    }
}
