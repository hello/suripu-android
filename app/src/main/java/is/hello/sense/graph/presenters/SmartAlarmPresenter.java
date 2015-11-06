package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;
import rx.subjects.ReplaySubject;

@Singleton public class SmartAlarmPresenter extends ValuePresenter<ArrayList<Alarm>> {
    private static final int FUTURE_CUT_OFF_MINUTES = 5;

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


    //region Validation

    public boolean validateAlarms(@NonNull List<Alarm> alarms) {
        final Set<Integer> alarmDays = new HashSet<>();
        for (final Alarm alarm : alarms) {
            if (!alarm.isEnabled()) {
                continue;
            }

            if (!alarm.isRepeated()) {
                DateTime expectedRingTime;
                try {
                    expectedRingTime = alarm.getExpectedRingTime();
                } catch (Exception ignored) {
                    return false;
                }

                if (alarm.isSmart()) {
                    if (alarmDays.contains(expectedRingTime.getDayOfWeek())) {
                        return false;
                    }

                    alarmDays.add(expectedRingTime.getDayOfWeek());
                }
            } else {
                if (!alarm.isSmart()) {
                    continue;
                }

                for (final Integer dayOfWeek : alarm.getDaysOfWeek()) {
                    if (alarmDays.contains(dayOfWeek)) {
                        return false;
                    }

                    alarmDays.add(dayOfWeek);
                }
            }
        }

        return true;
    }

    @VisibleForTesting
    boolean isAlarmTooSoon(@NonNull LocalTime nowTime, @NonNull Alarm alarm) {
        DateTime nowDate = nowTime.toDateTimeToday();
        DateTime cutOff = nowDate.plusMinutes(FUTURE_CUT_OFF_MINUTES);
        DateTime alarmTime = alarm.getTime().toDateTimeToday();
        Set<Integer> daysOfWeek = alarm.getDaysOfWeek();
        if (daysOfWeek == null || daysOfWeek.size() == 0) {
            // Alarm is for every day of the week, check if next 2 minutes
            return new Interval(nowDate, cutOff).contains(alarmTime);
        }else{
            int dayOfWeek = nowDate.getDayOfWeek();
            if (dayOfWeek == 0){
                // S M T W T F S
                // 7 1 2 3 4 5 6 -- Our Alarm
                // 0 1 2 3 4 5 6 -- DateTime
                dayOfWeek = 7; // Sunday is 7 for our Alarm, but 0 in DateTime
            }
            if (daysOfWeek.contains(dayOfWeek)){
                // User choose today as one of their options, check if next 2 minutes.
                return new Interval(nowDate, cutOff).contains(alarmTime);
            }
        }
        // Alarm isn't for today.
        return false;
    }

    public boolean isAlarmTooSoon(@NonNull Alarm alarm) {
        LocalTime nowTime = LocalTime.now(DateTimeZone.getDefault());
        return isAlarmTooSoon(nowTime, alarm);
    }

    //endregion


    //region Modifying alarm list

    public Observable<VoidResponse> addSmartAlarm(@NonNull Alarm alarm) {
        return latest().flatMap(alarms -> {
            ArrayList<Alarm> newAlarms = new ArrayList<>(alarms);
            newAlarms.add(alarm);
            return save(newAlarms);
        });
    }

    public Observable<VoidResponse> saveSmartAlarm(int index, @NonNull Alarm alarm) {
        return latest().flatMap(alarms -> {
            ArrayList<Alarm> newAlarms = new ArrayList<>(alarms);
            newAlarms.set(index, alarm);
            return save(newAlarms);
        });
    }

    public Observable<VoidResponse> deleteSmartAlarm(int index) {
        return latest().flatMap(alarms -> {
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
                             });
        } else {
            return Observable.error(new DayOverlapError());
        }
    }

    //endregion


    //region Alarm Sounds

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
        alarmSounds.observeOn(Rx.mainThreadScheduler())
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

    //endregion


    public static class DayOverlapError extends Exception {
        public DayOverlapError() {
            super("Cannot have more than one smart alarm set per day");
        }
    }
}
