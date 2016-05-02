package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.IListObject;

public class Duration extends ApiResponse implements IListObject.IListItem {
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