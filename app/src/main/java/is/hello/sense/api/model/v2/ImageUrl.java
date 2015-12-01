package is.hello.sense.api.model.v2;


import android.content.Context;

import com.google.gson.annotations.SerializedName;


import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.ImageLoader;

public class ImageUrl extends ApiResponse {

    @SerializedName("phone_1x")
    private String phone1x;

    @SerializedName("phone_2x")
    private String phone2x;

    @SerializedName("phone_3x")
    private String phone3x;

    public String getUrl(Context context){
        int size = ImageLoader.getScreenDenisty(context);
        if (size == ImageLoader.MEDIUM){
            return phone2x;
        }else if (size == ImageLoader.LARGE){
            return phone3x;
        }
        return phone1x;
    }

    @Override
    public String toString() {
        return "ImageUrl{" +
                "phone1x=" + phone1x +
                ", phone2x='" + phone2x + '\'' +
                ", phone3x='" + phone3x + '\'' +
                '}';
    }
}
