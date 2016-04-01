package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

public class Duration extends ApiResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}