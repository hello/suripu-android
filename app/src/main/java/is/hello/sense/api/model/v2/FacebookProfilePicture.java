package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

public class FacebookProfilePicture extends ApiResponse{

    @SerializedName("data")
    private Data imageData;

    public FacebookProfilePicture(@NonNull String imageUrl){
        this.imageData = new Data(imageUrl);
    }

    public String getImageUrl() {
        return imageData.getImageUrl();
    }


    public static class Data {
        @SerializedName("url")
        private String imageUrl;

        public Data(@NonNull String imageUrl){
            this.imageUrl = imageUrl;
        }

        public String getImageUrl() {
            return imageUrl;
        }

    }

}
