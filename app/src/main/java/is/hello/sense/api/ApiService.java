package is.hello.sense.api;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.AppStats;
import is.hello.sense.api.model.AppUnreadStats;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.DevicesInfo;
import is.hello.sense.api.model.PasswordUpdate;
import is.hello.sense.api.model.PushRegistration;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.RoomSensorHistory;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.api.model.StoreReview;
import is.hello.sense.api.model.SupportTopic;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.api.model.v2.InsightInfo;
import is.hello.sense.api.model.v2.MultiDensityImage;
import is.hello.sense.api.model.v2.SleepDurations;
import is.hello.sense.api.model.v2.SleepSoundActionPlay;
import is.hello.sense.api.model.v2.SleepSoundActionStop;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Multipart;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedFile;
import rx.Observable;

public interface ApiService {
    String HEADER_CLIENT_VERSION = "X-Client-Version";

    String DATE_FORMAT = "yyyy-MM-dd";
    String TIME_FORMAT = "HH:mm";

    String UNIT_TEMPERATURE_CELSIUS = "c";
    String UNIT_TEMPERATURE_US_CUSTOMARY = "f";

    String SENSOR_NAME_TEMPERATURE = "temperature";
    String SENSOR_NAME_HUMIDITY = "humidity";
    String SENSOR_NAME_PARTICULATES = "particulates";
    String SENSOR_NAME_LIGHT = "light";
    String SENSOR_NAME_SOUND = "sound";


    /**
     * Sentinel value used by graphing APIs.
     */
    int PLACEHOLDER_VALUE = -1;


    //region OAuth

    @POST("/v1/oauth2/token")
    Observable<OAuthSession> authorize(@NonNull @Body OAuthCredentials request);

    //endregion


    //region Updates

    @POST("/v1/app/checkin")
    Observable<UpdateCheckIn.Response> checkInForUpdates(@NonNull @Body UpdateCheckIn body);

    //endregion


    //region Account

    @GET("/v1/account")
    Observable<Account> getAccount(@Query("photo") Boolean includePhoto);

    @POST("/v1/account")
    Observable<Account> createAccount(@NonNull @Body Account account);

    @PUT("/v1/account")
    Observable<Account> updateAccount(@NonNull @Body Account account);

    @POST("/v1/account/password")
    Observable<VoidResponse> changePassword(@NonNull @Body PasswordUpdate passwordUpdate);

    @POST("/v1/notifications/registration")
    Observable<VoidResponse> registerForNotifications(@NonNull @Body PushRegistration registration);

    @GET("/v1/timezone")
    Observable<SenseTimeZone> currentTimeZone();

    @POST("/v1/timezone")
    Observable<SenseTimeZone> updateTimeZone(@NonNull @Body SenseTimeZone senseTimeZone);

    @POST("/v1/account/email")
    Observable<Account> updateEmailAddress(@NonNull @Body Account account);

    @GET("/v2/account/preferences")
    Observable<Account.Preferences> accountPreferences();

    @PUT("/v2/account/preferences")
    Observable<Account.Preferences> updateAccountPreferences(@NonNull @Body Account.Preferences preference);

    //endregion

    @Multipart
    @POST("/v1/photo/profile")
    Observable<MultiDensityImage> uploadProfilePhoto(@NonNull @Part("file") TypedFile profilePhoto);


    //region Timeline

    @GET("/v2/timeline/{date}")
    Observable<Timeline> timelineForDate(@NonNull @Path("date") String date);

    @PATCH("/v2/timeline/{date}/events/{type}/{timestamp}")
    Observable<Timeline> amendTimelineEventTime(@NonNull @Path("date") String date,
                                                @NonNull @Path("type") TimelineEvent.Type type,
                                                @Path("timestamp") long timestamp,
                                                @NonNull @Body TimelineEvent.TimeAmendment amendment);

    @DELETE("/v2/timeline/{date}/events/{type}/{timestamp}")
    Observable<Timeline> deleteTimelineEvent(@NonNull @Path("date") String date,
                                             @NonNull @Path("type") TimelineEvent.Type type,
                                             @Path("timestamp") long timestamp);

    @PUT("/v2/timeline/{date}/events/{type}/{timestamp}")
    @Headers("Content-Type: application/json")
    Observable<VoidResponse> verifyTimelineEvent(@NonNull @Path("date") String date,
                                                 @NonNull @Path("type") TimelineEvent.Type type,
                                                 @Path("timestamp") long timestamp,
                                                 @NonNull @Body String stupidOkHttp);

    //endregion


    //region Room Conditions

    @GET("/v1/room/current")
    Observable<RoomConditions> currentRoomConditions(@NonNull @Query("temp_unit") String unit);

    @GET("/v1/room/all_sensors/hours")
    Observable<RoomSensorHistory> roomSensorHistory(@Query("quantity") int numberOfHours,
                                                    @Query("from_utc") long timestamp);

    @GET("/v1/room/{sensor}/day")
    Observable<ArrayList<SensorGraphSample>> sensorHistoryForDay(@Path("sensor") String sensor,
                                                                 @Query("from") long timestamp);

    @GET("/v1/room/{sensor}/week")
    Observable<ArrayList<SensorGraphSample>> sensorHistoryForWeek(@Path("sensor") String sensor,
                                                                  @Query("from") long timestamp);

    //endregion


    //region Insights

    @GET("/v2/insights")
    Observable<ArrayList<Insight>> currentInsights();

    @GET("/v2/insights/info/{category}")
    Observable<ArrayList<InsightInfo>> insightInfo(@NonNull @Path("category") String category);

    //endregion


    //region Questions

    @GET("/v1/questions")
    Observable<ArrayList<Question>> questions(@NonNull @Query("date") String timestamp);

    @POST("/v1/questions/save")
    Observable<VoidResponse> answerQuestion(@Query("account_question_id") long accountId,
                                            @NonNull @Body List<Question.Choice> answers);

    @PUT("/v1/questions/skip")
    @Headers("Content-Type: application/json")
    Observable<VoidResponse> skipQuestion(@Query("account_question_id") long accountId,
                                          @Query("id") long questionId,
                                          @NonNull @Body String stupidOkHttp);

    //endregion


    //region Devices

    @GET("/v2/devices")
    Observable<Devices> registeredDevices();

    @GET("/v2/devices/info")
    Observable<DevicesInfo> devicesInfo();

    @DELETE("/v2/devices/pill/{id}")
    Observable<VoidResponse> unregisterPill(@Path("id") @NonNull String pillId);

    @DELETE("/v2/devices/sense/{id}")
    Observable<VoidResponse> unregisterSense(@Path("id") @NonNull String senseId);

    @DELETE("/v2/devices/sense/{id}/all")
    Observable<VoidResponse> removeSenseAssociations(@Path("id") @NonNull String senseId);

    //endregion


    //region Smart Alarms

    @GET("/v1/alarms")
    Observable<ArrayList<Alarm>> smartAlarms();

    @POST("/v1/alarms/{client_time_utc}")
    Observable<VoidResponse> saveSmartAlarms(@Path("client_time_utc") long timestamp,
                                             @NonNull @Body List<Alarm> alarms);

    @GET("/v1/alarms/sounds")
    Observable<ArrayList<Alarm.Sound>> availableSmartAlarmSounds();

    //endregion

    //region Sleep Sounds

    @GET("/v2/sleep_sounds/sounds")
    Observable<SleepSounds> getSounds();

    @GET("/v2/sleep_sounds/durations")
    Observable<SleepDurations> getDurations();

    @GET("/v2/sleep_sounds/combined_state")
    Observable<SleepSoundsState> getSleepSoundsCurrentState();

    @GET("/v2/sleep_sounds/status")
    Observable<SleepSoundStatus> getSleepSoundStatus();

    @POST("/v2/sleep_sounds/play")
    Observable<VoidResponse> playSleepSound(@NonNull @Body SleepSoundActionPlay action);

    @POST("/v2/sleep_sounds/stop")
    Observable<VoidResponse> stopSleepSound(@NonNull @Body SleepSoundActionStop action);

    //endregion


    //region Trends

    @GET("/v2/trends/{time_scale}")
    Observable<Trends> trendsForTimeScale(@NonNull @Path("time_scale") Trends.TimeScale timeScale);

    //endregion


    //region Support

    @GET("/v1/support/topics")
    Observable<ArrayList<SupportTopic>> supportTopics();

    //endregion


    //region Stats

    @PATCH("/v1/app/stats")
    Observable<VoidResponse> updateStats(@NonNull @Body AppStats stats);

    @GET("/v1/app/stats/unread")
    Observable<AppUnreadStats> unreadStats();

    //endregion


    //region Analytics

    @POST("/v2/store/feedback")
    Observable<Void> trackStoreReview(@NonNull @Body StoreReview review);

    //endregion
}
