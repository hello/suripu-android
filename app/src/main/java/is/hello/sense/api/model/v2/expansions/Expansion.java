package is.hello.sense.api.model.v2.expansions;

import android.support.annotation.NonNull;

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

    public Expansion(final long id,
                     @NonNull final Category category,
                     @NonNull final  String deviceName,
                     @NonNull final String serviceName,
                     @NonNull final MultiDensityImage icon,
                     @NonNull final String authUri,
                     @NonNull final String completionUri,
                     @NonNull final String description,
                     @NonNull final State state) {
        this.id = id;
        this.category = category;
        this.deviceName = deviceName;
        this.serviceName = serviceName;
        this.icon = icon;
        this.authUri = authUri;
        this.completionUri = completionUri;
        this.description = description;
        this.state = state;
    }

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

    public static Expansion generateThermostatTestCase(@NonNull final State state){
        return new Expansion(1,
                             Category.THERMOSTAT,
                             "Nest Thermostat",
                             "Nest",
                             new MultiDensityImage(),
                             "invalid uri",
                             "invalid uri",
                             "description",
                             state);
    }

    public static Expansion generateLightTestCase(@NonNull final State state){
        return new Expansion(2,
                             Category.LIGHT,
                             "Hue Light",
                             "Hue",
                             new MultiDensityImage(),
                             "invalid uri",
                             "invalid uri",
                             "description",
                             state);
    }


}
