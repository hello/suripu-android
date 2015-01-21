package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.ReplaySubject;

@Singleton public class SmartAlarmPresenter extends ValuePresenter<ArrayList<Alarm>> {
    private final ApiService apiService;
    private ArrayList<Alarm.Sound> availableAlarmSounds;
    private ReplaySubject<ArrayList<Alarm.Sound>> pendingAlarmSounds;

    public final PresenterSubject<ArrayList<Alarm>> alarms = this.subject;

    @Inject SmartAlarmPresenter(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        forgetAvailableAlarmSoundsCache();
        return super.onForgetDataForLowMemory();
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
        return Alarm.validateAlarms(alarms);
    }


    public Observable<VoidResponse> addSmartAlarm(@NonNull Alarm alarm) {
        return alarms.take(1).flatMap(alarms -> {
            ArrayList<Alarm> newAlarms = new ArrayList<>(alarms);
            newAlarms.add(alarm);
            return save(newAlarms);
        });
    }

    public Observable<VoidResponse> saveSmartAlarm(int index, @NonNull Alarm alarm) {
        return alarms.take(1).flatMap(alarms -> {
            ArrayList<Alarm> newAlarms = new ArrayList<>(alarms);
            newAlarms.set(index, alarm);
            return save(newAlarms);
        });
    }

    public Observable<VoidResponse> deleteSmartAlarm(int index) {
        return alarms.take(1).flatMap(alarms -> {
            ArrayList<Alarm> newAlarms = new ArrayList<>(alarms);
            newAlarms.remove(index);
            return save(newAlarms);
        });
    }

    public Observable<VoidResponse> save(@NonNull ArrayList<Alarm> updatedAlarms) {
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

    public Observable<ArrayList<Alarm.Sound>> availableAlarmSounds() {
        if (availableAlarmSounds != null) {
            return Observable.just(availableAlarmSounds);
        }

        if (pendingAlarmSounds != null) {
            return pendingAlarmSounds;
        }

        this.pendingAlarmSounds = ReplaySubject.createWithSize(1);

        logEvent("Loading smart alarm sounds");

        Observable<ArrayList<Alarm.Sound>> alarmSounds = apiService.availableSmartAlarmSounds();
        alarmSounds.observeOn(AndroidSchedulers.mainThread())
                   .subscribe(sounds -> {
                       logEvent("Loaded smart alarm sounds");
                       this.availableAlarmSounds = sounds;

                       pendingAlarmSounds.onNext(sounds);
                       pendingAlarmSounds.onCompleted();
                       this.pendingAlarmSounds = null;
                   }, e -> {
                       logEvent("Could not load smart alarm sounds");

                       pendingAlarmSounds.onError(e);
                       this.pendingAlarmSounds = null;
                   });

        return pendingAlarmSounds;
    }

    public void forgetAvailableAlarmSoundsCache() {
        this.availableAlarmSounds = null;
    }


    public static class DayOverlapError extends Exception {
        public DayOverlapError() {
            super("Cannot have more than one smart alarm set per day");
        }
    }
}
