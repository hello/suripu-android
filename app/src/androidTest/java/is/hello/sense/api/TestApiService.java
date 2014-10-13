package is.hello.sense.api;

import android.support.annotation.NonNull;

import java.util.List;

import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.SensorHistory;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import retrofit.http.Body;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

public final class TestApiService implements ApiService {
    private <T> Observable<T> unimplemented() {
        return Observable.error(new IllegalAccessException("unimplemented method in TestApiService"));
    }


    @Override
    public Observable<OAuthSession> authorize(@Body OAuthCredentials request) {
        return unimplemented();
    }

    @Override
    public Observable<Account> getAccount() {
        return unimplemented();
    }

    @Override
    public Observable<Account> createAccount(@Body Account account) {
        return unimplemented();
    }

    @Override
    public Observable<Account> updateAccount(@Body Account account) {
        return unimplemented();
    }

    @Override
    public Observable<List<Timeline>> timelineForDate(@NonNull @Path("year") String year, @NonNull @Path("month") String month, @NonNull @Path("day") String day) {
        return unimplemented();
    }

    @Override
    public Observable<RoomConditions> currentRoomConditions() {
        return unimplemented();
    }

    @Override
    public Observable<List<SensorHistory>> sensorHistoryForDay(@Path("sensor") String sensor, @Query("timestamp_millis") long timestamp) {
        return unimplemented();
    }

    @Override
    public Observable<List<SensorHistory>> sensorHistoryForWeek(@Path("sensor") String sensor, @Query("timestamp_millis") long timestamp) {
        return unimplemented();
    }

    @Override
    public Observable<List<Question>> questions(@NonNull @Query("date") String timestamp) {
        return unimplemented();
    }

    @Override
    public Observable<ApiResponse> answerQuestion(@NonNull @Body Question.Choice answer) {
        return unimplemented();
    }
}
