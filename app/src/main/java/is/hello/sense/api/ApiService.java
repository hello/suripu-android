package is.hello.sense.api;

import android.support.annotation.NonNull;

import java.util.List;

import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.PushRegistration;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

public interface ApiService {
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    //region OAuth

    @POST("/oauth2/token")
    Observable<OAuthSession> authorize(@Body OAuthCredentials request);

    //endregion


    //region Account

    @GET("/account")
    Observable<Account> getAccount();

    @POST("/account")
    Observable<Account> createAccount(@Body Account account);

    @PUT("/account")
    Observable<Account> updateAccount(@Body Account account);

    @POST("/notifications/registration")
    Observable<ApiResponse> registerForNotifications(@Body PushRegistration registration);

    //endregion


    //region Timeline

    @GET("/timeline/{year}-{month}-{day}")
    Observable<List<Timeline>> timelineForDate(@NonNull @Path("year") String year,
                                               @NonNull @Path("month") String month,
                                               @NonNull @Path("day") String day);

    //endregion


    //region Room Conditions

    @GET("/room/current")
    Observable<RoomConditions> currentRoomConditions();

    @GET("/room/{sensor}/day")
    Observable<List<SensorHistory>> sensorHistoryForDay(@Path("sensor") String sensor,
                                                        @Query("from") long timestamp);

    @GET("/room/{sensor}/week")
    Observable<List<SensorHistory>> sensorHistoryForWeek(@Path("sensor") String sensor,
                                                         @Query("from") long timestamp);

    //endregion


    //region Questions

    @GET("/questions")
    Observable<List<Question>> questions(@NonNull @Query("date") String timestamp);

    @POST("/questions")
    Observable<ApiResponse> answerQuestion(@NonNull @Body Question.Choice answer);

    @PUT("/questions/:id/skip")
    Observable<ApiResponse> skipQuestion(@Path("id") long questionId);

    //endregion
}
