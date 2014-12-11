package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseBooleanArray;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.annotations.CacheDirectoryFile;
import is.hello.sense.util.CachedObject;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscription;

public class SmartAlarmPresenter extends UpdatablePresenter<List<SmartAlarm>> {
    private final ApiService apiService;

    public final PresenterSubject<List<SmartAlarm>> alarms = this.subject;

    @Inject SmartAlarmPresenter(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<List<SmartAlarm>> provideUpdateObservable() {
        return apiService.smartAlarms();
    }

    public boolean validateAlarms(@NonNull List<SmartAlarm> alarms) {
        Set<Integer> alarmDays = new HashSet<>();
        for (SmartAlarm alarm : alarms) {
            for (Integer dayOfWeek : alarm.getDaysOfWeek()) {
                if (alarmDays.contains(dayOfWeek)) {
                    return false;
                } else {
                    alarmDays.add(dayOfWeek);
                }
            }
        }

        return true;
    }

    public Observable<VoidResponse> save(@NonNull List<SmartAlarm> updatedAlarms) {
        logEvent("save()");

        return apiService.saveSmartAlarms(System.currentTimeMillis(), updatedAlarms)
                         .doOnCompleted(() -> {
                             logEvent("smart alarms saved");
                             this.alarms.onNext(updatedAlarms);
                         })
                         .doOnError(this.alarms::onError);
    }
}
