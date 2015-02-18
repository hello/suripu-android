package is.hello.sense.api;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.AccountPreference;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.AvailableTrendGraph;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.model.DevicesInfo;
import is.hello.sense.api.model.Feedback;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.InsightCategory;
import is.hello.sense.api.model.InsightInfo;
import is.hello.sense.api.model.PasswordUpdate;
import is.hello.sense.api.model.PushRegistration;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.RoomSensorHistory;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.api.model.SensorGraphSample;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TrendGraph;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.util.Logger;
import retrofit.http.Body;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;
import rx.schedulers.Schedulers;

public final class TestApiService implements ApiService {
    private final Context context;
    private final ObjectMapper objectMapper;

    public TestApiService(@NonNull Context context, @NonNull ObjectMapper objectMapper) {
        this.context = context;
        this.objectMapper = objectMapper;
    }


    private <T> Observable<T> loadResponse(@NonNull String filename, TypeReference<T> responseType) {
        AssetManager assetManager = context.getAssets();
        InputStream stream = null;
        try {
            stream = assetManager.open(filename + ".json");
            T response = objectMapper.readValue(stream, responseType);
            return safeJust(response);
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
        return loadResponse("account", new TypeReference<Account>() {});
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
    public Observable<AccountPreference> updateAccountPreference(@NonNull @Body AccountPreference preference) {
        return unimplemented();
    }

    @Override
    public Observable<HashMap<AccountPreference.Key, Object>> accountPreferences() {
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
    public Observable<VoidResponse> registerForNotifications(@NonNull @Body PushRegistration registration) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<ArrayList<Timeline>> timelineForDate(@NonNull @Path("year") String year,
                                                      @NonNull @Path("month") String month,
                                                      @NonNull @Path("day") String day) {
        return loadResponse("timeline", new TypeReference<ArrayList<Timeline>>() {});
    }

    @Override
    public Observable<VoidResponse> submitCorrect(@NonNull @Body Feedback correction) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<ArrayList<Insight>> currentInsights() {
        return loadResponse("insights", new TypeReference<ArrayList<Insight>>() {});
    }

    @Override
    public Observable<RoomConditions> currentRoomConditions(@NonNull @Query("temp_unit") String unit) {
        return loadResponse("current_conditions", new TypeReference<RoomConditions>() {});
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
        return loadResponse("questions", new TypeReference<ArrayList<Question>>() {});
    }

    @Override
    public Observable<VoidResponse> answerQuestion(@Query("account_question_id") long accountId,
                                                   @NonNull @Body List<Question.Choice> answers) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<VoidResponse> skipQuestion(@Query("account_question_id") long accountId,
                                                 @Query("id") long questionId) {
        return safeJust(new VoidResponse());
    }

    @Override
    public Observable<ArrayList<Device>> registeredDevices() {
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
        return loadResponse("smart_alarms", new TypeReference<ArrayList<Alarm>>() {});
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
    public Observable<ArrayList<AvailableTrendGraph>> availableTrendGraphs() {
        return loadResponse("available_trend_graphs", new TypeReference<ArrayList<AvailableTrendGraph>>() {});
    }

    @Override
    public Observable<ArrayList<TrendGraph>> allTrends() {
        return loadResponse("all_trends", new TypeReference<ArrayList<TrendGraph>>() {});
    }

    @Override
    public Observable<ArrayList<TrendGraph>> trendGraph(@NonNull @Query("data_type") String dataType,
                                                        @NonNull @Query("time_period") String timePeriod) {
        return loadResponse("single_trend", new TypeReference<ArrayList<TrendGraph>>() {});
    }

    @Override
    public Observable<ArrayList<InsightInfo>> insightInfo(@NonNull @Path("category") InsightCategory category) {
        return unimplemented();
    }

    @Override
    public Observable<RoomSensorHistory> roomSensorHistory(@Query("quantity") int numberOfHours,
                                                           @Query("from_utc") long timestamp) {
        return loadResponse("room_sensor_history", new TypeReference<RoomSensorHistory>() {});
    }
    
    
    private static <T> Observable<T> safeJust(T value) {
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
}
