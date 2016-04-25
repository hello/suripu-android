package is.hello.sense.api.model.v2;


import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.ui.activities.ListActivity;

public class Sound extends ApiResponse implements ListActivity.ListItem{
    @SerializedName("id")
    private int id;

    @SerializedName("preview_url")
    private String previewUrl;

    @SerializedName("name")
    private String name;

    public int getId() {
        return id;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}
