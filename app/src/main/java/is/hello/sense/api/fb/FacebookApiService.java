package is.hello.sense.api.fb;

import is.hello.sense.api.fb.model.FacebookProfilePicture;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Query;
import rx.Observable;

public interface FacebookApiService {
    //TODO Currently passes Authtoken as parameter of this method. Ideally use OkHttp Interceptor
    @GET("/me/picture")
    Observable<FacebookProfilePicture> getProfilePicture(
            @Query(value = "redirect") String returnJson,
            @Query(value = "type") String size,
            @Header("Authorization") String authToken);
}
