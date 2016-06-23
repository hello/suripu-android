package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

public class ShareUrl extends ApiResponse {

    @SerializedName("url")
    private String url;

    public ShareUrl(final String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
