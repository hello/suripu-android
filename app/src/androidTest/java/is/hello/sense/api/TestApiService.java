package is.hello.sense.api;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.PushRegistration;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.util.Logger;
import retrofit.http.Body;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

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
            return Observable.just(response);
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
    public Observable<OAuthSession> authorize(@Body OAuthCredentials request) {
        return unimplemented();
    }

    @Override
    public Observable<Account> getAccount() {
        return loadResponse("account", new TypeReference<Account>() {});
    }

    @Override
    public Observable<Account> createAccount(@Body Account account) {
        return unimplemented();
    }

    @Override
    public Observable<Account> updateAccount(@Body Account account) {
        return Observable.just(account);
    }

    @Override
    public Observable<ApiResponse> registerForNotifications(@Body PushRegistration registration) {
        return unimplemented();
    }

    @Override
    public Observable<List<Timeline>> timelineForDate(@NonNull @Path("year") String year,
                                                      @NonNull @Path("month") String month,
                                                      @NonNull @Path("day") String day) {
        return loadResponse("timeline", new TypeReference<List<Timeline>>() {});
    }

    @Override
    public Observable<List<Insight>> currentInsights() {
        return loadResponse("insights", new TypeReference<List<Insight>>() {});
    }

    @Override
    public Observable<RoomConditions> currentRoomConditions() {
        return loadResponse("current_conditions", new TypeReference<RoomConditions>() {});
    }

    @Override
    public Observable<List<SensorHistory>> sensorHistoryForDay(@Path("sensor") String sensor,
                                                               @Query("timestamp_millis") long timestamp) {
        return unimplemented();
    }

    @Override
    public Observable<List<SensorHistory>> sensorHistoryForWeek(@Path("sensor") String sensor,
                                                                @Query("timestamp_millis") long timestamp) {
        return unimplemented();
    }

    @Override
    public Observable<List<Question>> questions(@NonNull @Query("date") String timestamp) {
        return loadResponse("questions", new TypeReference<List<Question>>() {});
    }

    @Override
    public Observable<ApiResponse> answerQuestion(@NonNull @Body Question.Choice answer) {
        return unimplemented();
    }

    @Override
    public Observable<ApiResponse> skipQuestion(@Path("id") long questionId) {
        return unimplemented();
    }

    @Override
    public Observable<List<Device>> registeredDevices() {
        return unimplemented();
    }

    @Override
    public Observable<ApiResponse> unregisterPill(@Path("id") @NonNull String pillId) {
        return unimplemented();
    }

    @Override
    public Observable<ApiResponse> unregisterSense(@Path("id") @NonNull String senseId) {
        return unimplemented();
    }

    @Override
    public Observable<List<SmartAlarm>> smartAlarms() {
        return loadResponse("smart_alarms", new TypeReference<List<SmartAlarm>>() {});
    }

    @Override
    public Observable<ApiResponse> saveSmartAlarms(@Query("client_time_utc") long timestamp,
                                                   @NonNull @Body List<SmartAlarm> alarms) {
        return Observable.just(new ApiResponse());
    }
}
