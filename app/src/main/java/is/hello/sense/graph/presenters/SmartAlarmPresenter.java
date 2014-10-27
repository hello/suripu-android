package is.hello.sense.graph.presenters;

import android.content.Context;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.CachedObject;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscription;

public class SmartAlarmPresenter extends Presenter {
    private static final String CACHE_FILENAME = "Suripu-Smart-Alarms.json";

    @Inject ApiService apiService;
    @Inject Context applicationContext;
    @Inject ObjectMapper objectMapper;

    private final CachedObject<List<SmartAlarm>> alarmCache;

    public final PresenterSubject<List<SmartAlarm>> alarms = PresenterSubject.create();

    public SmartAlarmPresenter() {
        super();

        this.alarmCache = new CachedObject<>(applicationContext,
                                             CACHE_FILENAME,
                                             new TypeReference<List<SmartAlarm>>() {},
                                             objectMapper);
    }

    public void update() {
        logEvent("update()");

        Subscription cacheSubscription = alarmCache.get().subscribe(alarms);
        apiService.smartAlarms().subscribe(alarms -> {
            if (!cacheSubscription.isUnsubscribed())
                cacheSubscription.unsubscribe();

            this.alarms.onNext(alarms);
            alarmCache.set(alarms).subscribe(ignored -> logEvent("cache updated"), Functions.LOG_ERROR);
        }, e -> {
            Logger.error(SmartAlarmPresenter.class.getSimpleName(), "Could not refresh smart alarms.", e);
            if (cacheSubscription.isUnsubscribed()) {
                alarms.onError(e);
            }
        });
    }

    public Observable<ApiResponse> save(@NonNull List<SmartAlarm> alarms) {
        return apiService.saveSmartAlarms(System.currentTimeMillis(), alarms)
                         .doOnCompleted(() -> alarmCache.set(alarms));
    }
}
