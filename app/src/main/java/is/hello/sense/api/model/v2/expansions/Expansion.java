package is.hello.sense.api.model.v2.expansions;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.v2.MultiDensityImage;

public class Expansion extends ApiResponse implements Comparable{

    public static final long NO_ID = -1;
    public static final String INVALID_URL = Expansion.class.getName() + "invalid_expansion_url";

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

    @SerializedName("value_range")
    private ExpansionValueRange valueRange;

    private Expansion(final long id,
                      @NonNull final Category category,
                      @NonNull final String deviceName,
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

    public boolean isConnected(){
        return State.CONNECTED_ON.equals(state);
    }

    public boolean requiresConfiguration() {
        return State.NOT_CONFIGURED.equals(state);
    }

    public boolean requiresAuthentication() {
        return State.REVOKED.equals(state);
    }

    /**
     * Replace with real field once server is ready
     */
    public String getConfigurationType(){
        return category.displayString;
    }

    public void setState(@NonNull final State state) {
        this.state = state;
    }

    public ExpansionValueRange getValueRange() {
        return valueRange;
    }

    @Override
    public String toString() {
        return "Expansion{" +
                "id=" + id +
                ", category=" + category +
                ", deviceName=" + deviceName +
                ", serviceName=" + serviceName +
                ", iconRes=" + icon +
                ", authUri=" + authUri +
                ", completionUri=" + completionUri +
                ", description=" + description +
                ", state=" + state +
                ", valueRange=" + valueRange +
                "}";

    }

    @Override
    public int compareTo(@NonNull final Object o) {
        return Long.compare(this.id, ((Expansion) o).id);
    }

    public static Expansion generateTemperatureTestCase(@NonNull final State state){
        return new Expansion(1,
                             Category.TEMPERATURE,
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

    public static Expansion generateInvalidTestCase(){
        return new Expansion(NO_ID,
                             Category.UNKNOWN,
                             "Invalid name",
                             "Invalid service name",
                             new MultiDensityImage(),
                             "invalid uri",
                             "invalid uri",
                             "description",
                             State.UNKNOWN);
    }
}
