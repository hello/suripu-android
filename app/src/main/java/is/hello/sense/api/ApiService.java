package is.hello.sense.api;

import android.support.annotation.NonNull;

import java.util.List;

import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.PasswordUpdate;
import is.hello.sense.api.model.PushRegistration;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import retrofit.http.Body;
import retrofit.http.DELETE;
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
    Observable<OAuthSession> authorize(@NonNull @Body OAuthCredentials request);

    //endregion


    //region Account

    @GET("/account")
    Observable<Account> getAccount();

    @POST("/account")
    Observable<Account> createAccount(@NonNull @Body Account account);

    @PUT("/account")
    Observable<Account> updateAccount(@NonNull @Body Account account);

    @POST("/account/password")
    Observable<VoidResponse> changePassword(@NonNull @Body PasswordUpdate passwordUpdate);

    @POST("/notifications/registration")
    Observable<VoidResponse> registerForNotifications(@NonNull @Body PushRegistration registration);

    @POST("/timezone")
    Observable<SenseTimeZone> updateTimeZone(@NonNull @Body SenseTimeZone senseTimeZone);

    //endregion


    //region Timeline

    @GET("/timeline/{year}-{month}-{day}")
    Observable<List<Timeline>> timelineForDate(@NonNull @Path("year") String year,
                                               @NonNull @Path("month") String month,
                                               @NonNull @Path("day") String day);

    @GET("/insights")
    Observable<List<Insight>> currentInsights();

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
    Observable<VoidResponse> answerQuestion(@Query("account_question_id") long accountId,
                                            @NonNull @Body Question.Choice answer);

    @PUT("/questions/skip")
    Observable<VoidResponse> skipQuestion(@Query("account_question_id") long accountId,
                                          @Query("id") long questionId);

    //endregion


    //region Devices

    @GET("/devices")
    Observable<List<Device>> registeredDevices();

    @DELETE("/devices/pill/{id}")
    Observable<VoidResponse> unregisterPill(@Path("id") @NonNull String pillId);

    @DELETE("/devices/sense/{id}")
    Observable<VoidResponse> unregisterSense(@Path("id") @NonNull String senseId);

    //endregion


    //region Smart Alarms

    @GET("/alarms")
    Observable<List<SmartAlarm>> smartAlarms();

    @POST("/alarms/{client_time_utc}")
    Observable<VoidResponse> saveSmartAlarms(@Path("client_time_utc") long timestamp,
                                             @NonNull @Body List<SmartAlarm> alarms);

    //endregion
}
