package is.hello.sense.api.model.v2.expansions;

import com.google.gson.annotations.SerializedName;


import is.hello.sense.api.model.ApiResponse;

public class Configuration extends ApiResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("selected")
    private boolean selected;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "id=" + id +
                ", name=" + name +
                ", selected=" + selected +
                "}";

    }
}
