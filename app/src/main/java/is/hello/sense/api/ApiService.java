package is.hello.sense.api;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.AccountPreference;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.AvailableTrendGraph;
import is.hello.sense.api.model.InsightCategory;
import is.hello.sense.api.model.InsightInfo;
import is.hello.sense.api.model.RoomSensorHistory;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.PasswordUpdate;
import is.hello.sense.api.model.PushRegistration;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TrendGraph;
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
    String DATE_FORMAT = "yyyy-MM-dd";

    String UNIT_TEMPERATURE_CELCIUS = "c";
    String UNIT_TEMPERATURE_US_CUSTOMARY = "f";

    String SENSOR_NAME_TEMPERATURE = "temperature";
    String SENSOR_NAME_HUMIDITY = "humidity";
    String SENSOR_NAME_PARTICULATES = "particulates";
    String SENSOR_NAME_LIGHT = "light";
    String SENSOR_NAME_SOUND = "sound";

    //region OAuth

    @POST("/oauth2/token")
    Observable<OAuthSession> authorize(@NonNull @Body OAuthCredentials request);

    //endregion


    //region Updates

    @POST("/app/checkin")
    Observable<UpdateCheckIn.Response> checkInForUpdates(@NonNull @Body UpdateCheckIn body);

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

    @POST("/account/email")
    Observable<Account> updateEmailAddress(@NonNull @Body Account account);

    @GET("/preferences")
    Observable<HashMap<AccountPreference.Key, Object>> accountPreferences();

    @PUT("/preferences")
    Observable<AccountPreference> updateAccountPreference(@NonNull @Body AccountPreference preference);

    //endregion


    //region Timeline

    @GET("/timeline/{year}-{month}-{day}")
    Observable<ArrayList<Timeline>> timelineForDate(@NonNull @Path("year") String year,
                                                    @NonNull @Path("month") String month,
                                                    @NonNull @Path("day") String day);

    @GET("/insights")
    Observable<ArrayList<Insight>> currentInsights();

    @GET("/insights/info/{category}")
    Observable<ArrayList<InsightInfo>> insightInfo(@NonNull @Path("category") InsightCategory category);

    //endregion


    //region Room Conditions

    @GET("/room/current")
    Observable<RoomConditions> currentRoomConditions(@NonNull @Query("temp_unit") String unit);

    @GET("/room/all_sensors/hours")
    Observable<RoomSensorHistory> roomSensorHistory(@Query("quantity") int numberOfHours,
                                                    @Query("from_utc") long timestamp);

    @GET("/room/{sensor}/day")
    Observable<ArrayList<SensorGraphSample>> sensorHistoryForDay(@Path("sensor") String sensor,
                                                             @Query("from") long timestamp);

    @GET("/room/{sensor}/week")
    Observable<ArrayList<SensorGraphSample>> sensorHistoryForWeek(@Path("sensor") String sensor,
                                                              @Query("from") long timestamp);

    //endregion


    //region Questions

    @GET("/questions")
    Observable<ArrayList<Question>> questions(@NonNull @Query("date") String timestamp);

    @POST("/questions/save")
    Observable<VoidResponse> answerQuestion(@Query("account_question_id") long accountId,
                                            @NonNull @Body List<Question.Choice> answers);

    @PUT("/questions/skip")
    Observable<VoidResponse> skipQuestion(@Query("account_question_id") long accountId,
                                          @Query("id") long questionId);

    //endregion


    //region Devices

    @GET("/devices")
    Observable<ArrayList<Device>> registeredDevices();

    @DELETE("/devices/pill/{id}")
    Observable<VoidResponse> unregisterPill(@Path("id") @NonNull String pillId);

    @DELETE("/devices/sense/{id}")
    Observable<VoidResponse> unregisterSense(@Path("id") @NonNull String senseId);

    //endregion


    //region Smart Alarms

    @GET("/alarms")
    Observable<ArrayList<Alarm>> smartAlarms();

    @POST("/alarms/{client_time_utc}")
    Observable<VoidResponse> saveSmartAlarms(@Path("client_time_utc") long timestamp,
                                             @NonNull @Body List<Alarm> alarms);

    @GET("/alarms/sounds")
    Observable<ArrayList<Alarm.Sound>> availableSmartAlarmSounds();

    //endregion


    //region Trends

    @GET("/insights/trends/list")
    Observable<ArrayList<AvailableTrendGraph>> availableTrendGraphs();

    @GET("/insights/trends/all")
    Observable<ArrayList<TrendGraph>> allTrends();

    @GET("/insights/trends/graph")
    Observable<ArrayList<TrendGraph>> trendGraph(@NonNull @Query("data_type") String dataType,
                                                 @NonNull @Query("time_period") String timePeriod);

    //endregion
}
