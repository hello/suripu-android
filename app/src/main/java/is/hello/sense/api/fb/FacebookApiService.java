package is.hello.sense.api.fb;

import is.hello.sense.api.fb.model.FacebookProfile;
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

    /**
     *
     * @param fields needs to be comma separated for listing multiple fields
     * @param requireSSL specifies if a picture resource should be returned over HTTPS to avoid mixed content warnings in browsers
     * @param authToken necessary to pass validations
     * @return {@link FacebookProfile}
     */
    @GET("/me")
    Observable<FacebookProfile> getProfile(
            @Query(value = "fields") String fields,
            @Query(value = "return_ssl_resources") boolean requireSSL,
            @Header("Authorization") String authToken);
}
