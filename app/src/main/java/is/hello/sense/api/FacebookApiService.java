package is.hello.sense.api;

import is.hello.sense.api.model.v2.FacebookProfilePicture;
import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

/**
 * Created by simonchen on 5/17/16.
 */
public interface FacebookApiService {
    String baseUrl = "https://graph.facebook.com/v2.6";

    @GET("/me/picture")
    Observable<FacebookProfilePicture> getProfilePicture(
            @Query(value = "redirect") String returnJson,
            @Query(value = "type") String size);
}
