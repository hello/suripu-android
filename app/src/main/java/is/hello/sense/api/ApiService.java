package is.hello.sense.api;

import is.hello.sense.api.model.Account;
import is.hello.sense.api.sessions.OAuthRequest;
import is.hello.sense.api.sessions.OAuthSession;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import rx.Observable;

public interface ApiService {
    // TODO: Make these dynamic
    public static final String CLIENT_ID = "android_dev";
    public static final String CLIENT_SECRET = "99999secret";
    public static final String BASE_URL = "https://dev-api.hello.is/v1";

    //region OAuth

    @POST("/oauth2/token")
    Observable<OAuthSession> authorize(@Body OAuthRequest request);

    //endregion


    //region Account

    @GET("/account")
    Observable<Account> getAccount();

    @POST("/account")
    Observable<Account> createAccount(@Body Account account);

    @PUT("/account")
    Observable<Account> updateAccount(@Body Account account);

    //endregion
}
