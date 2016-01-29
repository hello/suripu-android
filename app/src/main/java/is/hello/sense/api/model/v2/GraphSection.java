package is.hello.sense.api.model.v2;


import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

import is.hello.sense.api.model.ApiResponse;

public class GraphSection extends ApiResponse {
    @VisibleForTesting
    @SerializedName("values")
    private Float[] values;

    @VisibleForTesting
    @SerializedName("titles")
    @Nullable
    private String[] titles;

    @VisibleForTesting
    @SerializedName("highlighted_values")
    private int[] highlightedValues;

    @VisibleForTesting
    @SerializedName("highlighted_title")
    private int highlightedTitle;

    public Float[] getValues() {
        return values;
    }
    public float getValue(int i){
        return values[i];
    }

    @Nullable
    public String[] getTitles() {
        return titles;
    }

    public int[] getHighlightedValues() {
        return highlightedValues;
    }

    public int getHighlightedTitle() {
        return highlightedTitle;
    }

    @Override
    public String toString() {
        return "GraphSection{" +
                ", values='" + Arrays.toString(values) + '\'' +
                ", titles='" + Arrays.toString(titles) + '\'' +
                ", highlightedValues='" + Arrays.toString(highlightedValues) + '\'' +
                ", highlightedTitle='" + highlightedTitle + '\'' +
                '}';
    }
}
