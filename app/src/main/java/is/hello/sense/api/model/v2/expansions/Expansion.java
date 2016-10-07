package is.hello.sense.api.model.v2.expansions;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.v2.MultiDensityImage;

public class Expansion extends ApiResponse {
    @SerializedName("id")
    private long id;

    @SerializedName("category")
    private Category category;

    @SerializedName("device_name")
    private String deviceName;

    @SerializedName("service_name")
    private String serviceName;

    @SerializedName("icon")
    private MultiDensityImage icon;

    @SerializedName("auth_uri")
    private String authUri;

    @SerializedName("completion_uri")
    private String completionUri;

    @SerializedName("description")
    private String description;

    @SerializedName("state")
    private State state;

    public long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public MultiDensityImage getIcon() {
        return icon;
    }

    public String getAuthUri() {
        return authUri;
    }

    public String getCompletionUri() {
        return completionUri;
    }

    public String getDescription() {
        return description;
    }

    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        return "Expansion{" +
                "id=" + id +
                ", category=" + category +
                ", deviceName=" + deviceName +
                ", serviceName=" + serviceName +
                ", icon=" + icon +
                ", authUri=" + authUri +
                ", completionUri=" + completionUri +
                ", description=" + description +
                ", state=" + state +
                "}";

    }


}
