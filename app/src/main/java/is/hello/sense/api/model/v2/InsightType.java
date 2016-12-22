package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

public class InsightType extends ApiResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private final String type = "insight";

    public InsightType(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }
}
