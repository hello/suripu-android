package is.hello.sense.api.model.v2;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.R;
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

    public String getUrlForSharing(@NonNull final Context context) {
        return context.getString(R.string.share_text, url);

    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
