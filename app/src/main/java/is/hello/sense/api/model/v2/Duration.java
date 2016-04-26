package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.ui.activities.ListActivity;

public class Duration extends ApiResponse implements ListActivity.ListItem {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @Override
    public String getPreviewUrl() {
        return null;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}