package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class SmartAlarmPresenter extends ValuePresenter<ArrayList<Alarm>> {
    private final ApiService apiService;

    public final PresenterSubject<ArrayList<Alarm>> alarms = this.subject;

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
    protected Observable<ArrayList<Alarm>> provideUpdateObservable() {
        return apiService.smartAlarms();
    }

    public boolean validateAlarms(@NonNull List<Alarm> alarms) {
        return Alarm.Utils.isValidAlarms(alarms);
    }


    public Observable<VoidResponse> save(@NonNull ArrayList<Alarm> updatedAlarms) {
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
