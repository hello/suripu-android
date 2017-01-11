package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Minutes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.alarms.AlarmGroups;
import is.hello.sense.api.model.v2.alarms.AlarmSource;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.util.DateFormatter;
import rx.Observable;
import rx.subjects.ReplaySubject;

@Singleton
public class SmartAlarmInteractor extends ValueInteractor<ArrayList<Alarm>> {
    // Interval is an exclusive range, so we have to add on to
    // the actual too soon minutes to get the correct result.
    private static final Minutes TOO_SOON = Minutes.minutes(Alarm.TOO_SOON_MINUTES + 1);

    private final ApiService apiService;
    private ArrayList<Alarm.Sound> availableAlarmSounds;
    private ReplaySubject<ArrayList<Alarm.Sound>> pendingAlarmSounds;

    public final InteractorSubject<ArrayList<Alarm>> alarms = this.subject;

    @Inject
    SmartAlarmInteractor(@NonNull ApiService apiService) {
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
        return apiService.smartAlarms()
                         .map(AlarmGroups::getAll); //todo check if expansions and voice enabled
    }


    //region Validation

    /**
     * @param checkAlarm alarm under test
     * @param alarms list to validate
     * @return true if 'alarms' contains no other enabled smart alarms with overlapping days as 'checkAlarm'
     */
    public boolean canBeSmartWith(@NonNull final Alarm checkAlarm,
                                  @NonNull final List<Alarm> alarms) {
        @DateFormatter.JodaWeekDay
        final Integer defaultDay = checkAlarm.getDefaultRingTime()
                                             .getDayOfWeek();

        final Set<Integer> daysOfWeek = checkAlarm.getDaysOfWeek();

        for(final Alarm alarm : alarms) {
            if (!(alarm.isSmart() && alarm.isEnabled())) {
                continue;
            }
            final Set<Integer> reservedDaysOfWeek = alarm.getDaysOfWeek();

            if(daysOfWeek.isEmpty()
                    && (reservedDaysOfWeek.contains(defaultDay) || reservedDaysOfWeek.isEmpty())) {
                return false;
            } else if (daysOfWeek.contains(defaultDay) && reservedDaysOfWeek.isEmpty()) {
                return false;
            }
            // test that no overlapping days are shared
            if(!Collections.disjoint(daysOfWeek, reservedDaysOfWeek)) {
                return false;
            }
        }
        return true;
    }

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

    /**
     * Checks if an alarms ring time is within the next {@link #TOO_SOON} minutes.
     *
     * @param alarm The alarm to check
     * @return true if the alarm would ring within the too soon time period; false otherwise.
     */
    public boolean isAlarmTooSoon(@NonNull Alarm alarm) {
        final DateTime now = DateTime.now(DateTimeZone.getDefault())
                                     .withSecondOfMinute(0)
                                     .withMillisOfSecond(0);
        final DateTime alarmTime = alarm.toTimeToday();
        final Set<Integer> daysOfWeek = alarm.getDaysOfWeek();
        if (Lists.isEmpty(daysOfWeek) || daysOfWeek.contains(now.getDayOfWeek())) {
            // Alarm includes today, check if next 2 minutes
            return new Interval(now, TOO_SOON).contains(alarmTime);
        }
        // Alarm isn't for today.
        return false;
    }

    //endregion


    //region Modifying alarm list

    public Observable<VoidResponse> addSmartAlarm(@NonNull Alarm alarm) {
        return latest().flatMap(alarms -> {
            alarm.setSource(AlarmSource.MOBILE_APP);
            ArrayList<Alarm> newAlarms = new ArrayList<>(alarms);
            newAlarms.add(alarm);
            return save(newAlarms);
        });
    }

    public Observable<VoidResponse> saveSmartAlarm(int index, @NonNull Alarm alarm) {
        return latest().flatMap(alarms -> {
            alarm.setSource(AlarmSource.MOBILE_APP);
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

    public Observable<VoidResponse> save(@NonNull final ArrayList<Alarm> updatedAlarms) {
        if (validateAlarms(updatedAlarms)) {
            return apiService.saveSmartAlarms(System.currentTimeMillis(),
                                              AlarmGroups.from(updatedAlarms))
                             .doOnCompleted(() -> {
                                 logEvent("smart alarms saved");
                                 this.alarms.onNext(updatedAlarms);
                             }).map(Functions.TO_VOID_RESPONSE);
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
