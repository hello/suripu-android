package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SmartAlarm;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class SmartAlarmPresenter extends ValuePresenter<ArrayList<SmartAlarm>> {
    private final ApiService apiService;

    public final PresenterSubject<ArrayList<SmartAlarm>> alarms = this.subject;

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
    protected Observable<ArrayList<SmartAlarm>> provideUpdateObservable() {
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


    public Observable<VoidResponse> save(@NonNull ArrayList<SmartAlarm> updatedAlarms) {
        logEvent("save()");

        if (validateAlarms(updatedAlarms)) {
            return apiService.saveSmartAlarms(System.currentTimeMillis(), updatedAlarms)
                             .doOnCompleted(() -> {
                                 logEvent("smart alarms saved");
                                 this.alarms.onNext(updatedAlarms);
                             })
                             .doOnError(this.alarms::onError);
        } else {
            return Observable.error(new DayOverlapError());
        }
    }


    public static class DayOverlapError extends Exception {
        public DayOverlapError() {
            super("Cannot have more than one smart alarm set per day");
        }
    }
}
