package is.hello.sense.api;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
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
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.api.model.v2.InsightInfo;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineBuilder;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.api.model.v2.TimelineEventBuilder;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.util.Logger;
import is.hello.sense.util.markup.text.MarkupString;
import retrofit.http.Body;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;
import rx.schedulers.Schedulers;

public final class TestApiService implements ApiService {
    private final Context context;
    private final Gson gson;

    public TestApiService(@NonNull Context context, @NonNull Gson gson) {
        this.context = context;
        this.gson = gson;
    }


    private <T> Observable<T> loadResponse(@NonNull String filename, @NonNull Type responseType) {
        AssetManager assetManager = context.getAssets();
        InputStream stream = null;
        try {
            stream = assetManager.open(filename + ".json");
            JsonReader jsonReader = new JsonReader(new InputStreamReader(stream));
            try {
                T response = gson.fromJson(jsonReader, responseType);
                return safeJust(response);
            } finally {
                jsonReader.close();
            }
        } catch (IOException e) {
            return Observable.error(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Logger.warn(TestApiService.class.getSimpleName(), "Could not close response input stream.", e);
                }
            }
        }
    }

    private <T> Observable<T> unimplemented() {
        return Observable.error(new IllegalAccessException("unimplemented method in TestApiService"));
    }


    @Override
    public Observable<OAuthSession> authorize(@NonNull @Body OAuthCredentials request) {
        return unimplemented();
    }

    @Override
    public Observable<Account> getAccount() {
        return loadResponse("account", new TypeToken<Account>() {
        }.getType());
    }

    @Override
    public Observable<Account> createAccount(@NonNull @Body Account account) {
        return unimplemented();
    }

    @Override
    public Observable<Account> updateAccount(@NonNull @Body Account account) {
        return safeJust(account);
    }

    @Override
    public Observable<UpdateCheckIn.Response> checkInForUpdates(@NonNull @Body UpdateCheckIn body) {
        return unimplemented();
    }

    @Override
    public Observable<Account.Preferences> accountPreferences() {
        return unimplemented();
    }

    @Override
    public Observable<Account.Preferences> updateAccountPreferences(@NonNull @Body Account.Preferences preferences) {
        return unimplemented();
    }

    @Override
    public Observable<VoidResponse> changePassword(@NonNull @Body PasswordUpdate passwordUpdate) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<SenseTimeZone> updateTimeZone(@NonNull @Body SenseTimeZone senseTimeZone) {
        return unimplemented();
    }

    @Override
    public Observable<SenseTimeZone> currentTimeZone() {
        return unimplemented();
    }

    @Override
    public Observable<VoidResponse> registerForNotifications(@NonNull @Body PushRegistration registration) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<Timeline> timelineForDate(@NonNull @Path("date") String date) {
        LocalDate dateTime = LocalDate.parse(date, DateTimeFormat.forPattern(ApiService.DATE_FORMAT));
        return safeJust(new TimelineBuilder()
                                .setDate(dateTime)
                                .setScore(90, ScoreCondition.IDEAL)
                                .setMessage(new MarkupString("This is *just* a test."))
                                .build());
    }



    @Override
    public Observable<VoidResponse> verifyTimelineEvent(@NonNull @Path("date") String date,
                                                        @NonNull @Path("type") TimelineEvent.Type type,
                                                        @Path("timestamp") long timestamp,
                                                        @NonNull @Body String stupidOkHttp) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<Timeline> amendTimelineEventTime(@NonNull @Path("date") String date,
                                                       @NonNull @Path("type") TimelineEvent.Type type,
                                                       @Path("timestamp") long timestamp,
                                                       @NonNull @Body TimelineEvent.TimeAmendment amendment) {
        LocalDate dateTime = LocalDate.parse(date, DateTimeFormat.forPattern(ApiService.DATE_FORMAT));
        return safeJust(new TimelineBuilder()
                                .setDate(dateTime)
                                .setScore(90, ScoreCondition.IDEAL)
                                .setMessage(new MarkupString("This is *just* a test."))
                                .addEvent(new TimelineEventBuilder()
                                                  .setType(type)
                                                  .setShiftedTimestamp(new DateTime(timestamp,
                                                                                    DateTimeZone.getDefault())
                                                                               .withTime(amendment.newTime))
                                                  .build())
                                .build());
    }

    @Override
    public Observable<Timeline> deleteTimelineEvent(@NonNull @Path("date") String date,
                                                    @NonNull @Path("type") TimelineEvent.Type type,
                                                    @Path("timestamp") long timestamp) {
        LocalDate dateTime = LocalDate.parse(date, DateTimeFormat.forPattern(ApiService.DATE_FORMAT));
        return safeJust(new TimelineBuilder()
                                .setDate(dateTime)
                                .setScore(90, ScoreCondition.IDEAL)
                                .setMessage(new MarkupString("This is *just* a test."))
                                .addEvent(new TimelineEventBuilder()
                                                  .setType(type)
                                                  .setShiftedTimestamp(new DateTime(timestamp,
                                                                                    DateTimeZone.getDefault()))
                                                  .build())
                                .build());
    }

    @Override
    public Observable<ArrayList<Insight>> currentInsights() {
        return loadResponse("insights", new TypeToken<ArrayList<Insight>>(){}.getType());
    }

    @Override
    public Observable<RoomConditions> currentRoomConditions(@NonNull @Query("temp_unit") String unit) {
        return loadResponse("current_conditions", new TypeToken<RoomConditions>() {
        }.getType());
    }

    @Override
    public Observable<ArrayList<SensorGraphSample>> sensorHistoryForDay(@Path("sensor") String sensor,
                                                               @Query("timestamp_millis") long timestamp) {
        return unimplemented();
    }

    @Override
    public Observable<ArrayList<SensorGraphSample>> sensorHistoryForWeek(@Path("sensor") String sensor,
                                                                @Query("timestamp_millis") long timestamp) {
        return unimplemented();
    }

    @Override
    public Observable<ArrayList<Question>> questions(@NonNull @Query("date") String timestamp) {
        return loadResponse("questions", new TypeToken<ArrayList<Question>>() {
        }.getType());
    }

    @Override
    public Observable<VoidResponse> answerQuestion(@Query("account_question_id") long accountId,
                                                   @NonNull @Body List<Question.Choice> answers) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<VoidResponse> skipQuestion(@Query("account_question_id") long accountId,
                                                 @Query("id") long questionId,
                                                 @NonNull @Body String stupidOkHttp) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<Devices> registeredDevices() {
        return unimplemented();
    }

    @Override
    public Observable<DevicesInfo> devicesInfo() {
        return unimplemented();
    }

    @Override
    public Observable<VoidResponse> unregisterPill(@Path("id") @NonNull String pillId) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<VoidResponse> unregisterSense(@Path("id") @NonNull String senseId) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<VoidResponse> removeSenseAssociations(@Path("id") @NonNull String senseId) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<ArrayList<Alarm>> smartAlarms() {
        return loadResponse("smart_alarms", new TypeToken<ArrayList<Alarm>>() {
        }.getType());
    }

    @Override
    public Observable<VoidResponse> saveSmartAlarms(@Query("client_time_utc") long timestamp,
                                                   @NonNull @Body List<Alarm> alarms) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<ArrayList<Alarm.Sound>> availableSmartAlarmSounds() {
        return unimplemented();
    }

    @Override
    public Observable<Account> updateEmailAddress(@NonNull @Body Account account) {
        return safeJust(account);
    }


    @Override
    public Observable<ArrayList<TrendGraph>> allTrends() {
        return loadResponse("all_trends", new TypeToken<ArrayList<TrendGraph>>(){}.getType());
    }

    @Override
    public Observable<ArrayList<TrendGraph>> trendGraph(@NonNull @Query("data_type") String dataType,
                                                        @NonNull @Query("time_period") String timePeriod) {
        return loadResponse("single_trend", new TypeToken<ArrayList<TrendGraph>>() {
        }.getType());
    }

    @Override
    public Observable<Trends> trends(@NonNull @Path("time_scale") Trends.TimeScale timeScale) {
        return null;
    }

    @Override
    public Observable<ArrayList<InsightInfo>> insightInfo(@NonNull @Path("category") String category) {
        return unimplemented();
    }

    @Override
    public Observable<RoomSensorHistory> roomSensorHistory(@Query("quantity") int numberOfHours,
                                                           @Query("from_utc") long timestamp) {
        return loadResponse("room_sensor_history", new TypeToken<RoomSensorHistory>(){}.getType());
    }

    @Override
    public Observable<ArrayList<SupportTopic>> supportTopics() {
        return unimplemented();
    }

    static <T> Observable<T> safeJust(T value) {
        Observable<T> observable = Observable.create(s -> {
            if (s.isUnsubscribed()) {
                return;
            }

            s.onNext(value);

            if (s.isUnsubscribed()) {
                return;
            }

            s.onCompleted();
        });

        return observable.subscribeOn(Schedulers.computation());
    }


    @Override
    public Observable<VoidResponse> updateStats(@NonNull @Body AppStats stats) {
        return Observable.just(new VoidResponse());
    }

    @Override
    public Observable<AppUnreadStats> unreadStats() {
        return unimplemented();
    }


    @Override
    public Observable<Void> trackStoreReview(@NonNull @Body StoreReview review) {
        return safeJust(null);
    }
}
