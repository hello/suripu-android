package is.hello.sense.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Days;
import org.joda.time.Hours;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.util.Analytics;
import rx.Observable;

public class DeviceIssuesInteractor extends ScopedValueInteractor<DeviceIssuesInteractor.Issue> {
    @Inject ApiService apiService;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    PersistentPreferencesInteractor persistentPreferences;

    public final InteractorSubject<Issue> topIssue = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Issue> provideUpdateObservable() {
        return apiService.registeredDevices()
                         .map(this::getTopIssue);
    }


    public Issue getTopIssue(@NonNull final Devices devices) {
        final SenseDevice sense = devices.getSense();
        final SleepPillDevice pill = devices.getSleepPill();

        if (sense != null) {
            Analytics.setSenseId(sense.deviceId);
        }

        if (sense == null) {
            return Issue.NO_SENSE_PAIRED;
        } else if (sense.getHoursSinceLastUpdated() >= BaseDevice.MISSING_THRESHOLD_HRS && shouldReportSenseMissing()) {
            return Issue.SENSE_MISSING;
        } else if (pill == null) {
            return Issue.NO_SLEEP_PILL_PAIRED;
        } else if (pill.state == BaseDevice.State.LOW_BATTERY && shouldReportLowBattery()) {
            return Issue.SLEEP_PILL_LOW_BATTERY;
        } else if (pill.getHoursSinceLastUpdated() >= BaseDevice.MISSING_THRESHOLD_HRS && shouldReportPillMissing()) {
            return Issue.SLEEP_PILL_MISSING;
        } else if (pill.shouldUpdate() && shouldReportPillUpdate()){
            return Issue.SLEEP_PILL_FIRMWARE_UPDATE_AVAILABLE;
        }

        return Issue.NONE;
    }

    private @Nullable DateTime getSystemAlertLastShown() {
        if (preferences.contains(PreferencesInteractor.SYSTEM_ALERT_LAST_SHOWN)) {
            return new DateTime(preferences.getLong(PreferencesInteractor.SYSTEM_ALERT_LAST_SHOWN, 0));
        } else {
            return null;
        }
    }

    private @Nullable DateTime getSenseMissingAlertLastShown() {
        if (preferences.contains(PreferencesInteractor.SENSE_ALERT_LAST_SHOWN)) {
            return new DateTime(preferences.getLong(PreferencesInteractor.SENSE_ALERT_LAST_SHOWN, 0));
        } else {
            return null;
        }
    }

    private @Nullable DateTime getPillMissingAlertLastShown() {
        if (preferences.contains(PreferencesInteractor.PILL_MISSING_ALERT_LAST_SHOWN)) {
            return new DateTime(preferences.getLong(PreferencesInteractor.PILL_MISSING_ALERT_LAST_SHOWN, 0));
        } else {
            return null;
        }
    }

    private @Nullable DateTime getPillUpdateAlertLastShown() {
        if (preferences.contains(PreferencesInteractor.PILL_FIRMWARE_UPDATE_ALERT_LAST_SHOWN)) {
            return new DateTime(preferences.getLong(PreferencesInteractor.PILL_FIRMWARE_UPDATE_ALERT_LAST_SHOWN, 0));
        } else {
            return null;
        }
    }

    @VisibleForTesting
    boolean shouldReportLowBattery() {
        final DateTime lastShown = getSystemAlertLastShown();
        return (lastShown == null || Days.daysBetween(lastShown, DateTime.now()).getDays() >= 1);
    }

    boolean shouldReportSenseMissing() {
        final DateTime lastShown = getSenseMissingAlertLastShown();
        return (lastShown == null || Days.daysBetween(lastShown, DateTime.now()).getDays() >= 1);
    }

    boolean shouldReportPillMissing() {
        final DateTime lastShown = getPillMissingAlertLastShown();
        return (lastShown == null || Days.daysBetween(lastShown, DateTime.now()).getDays() >= 1);
    }

    boolean shouldReportPillUpdate() {
        final DateTime lastShown = getPillUpdateAlertLastShown();
        return lastShown == null || Hours.hoursBetween(lastShown, DateTime.now()).isGreaterThan(Hours.TWO);
    }

    public boolean shouldShowUpdateFirmwareAction(@NonNull final String deviceId) {
        final DateTime lastUpdated = persistentPreferences.getLastPillUpdateDateTime(deviceId);
        return (lastUpdated == null || Hours.hoursBetween(lastUpdated, DateTime.now()).isGreaterThan(Hours.ONE));
    }

    public void updateLastUpdatedDevice(@NonNull final String deviceId) {
        persistentPreferences.updateLastUpdatedDevice(deviceId);
        preferences.edit()
                   .putLong(PreferencesInteractor.PILL_FIRMWARE_UPDATE_ALERT_LAST_SHOWN,
                            DateTimeUtils.currentTimeMillis())
                   .apply();
    }

    public void updateLastShown(@NonNull final Issue issue){
        switch (issue) {
            case SENSE_MISSING:
                preferences.edit()
                           .putLong(PreferencesInteractor.SENSE_ALERT_LAST_SHOWN,
                                    DateTimeUtils.currentTimeMillis())
                           .apply();
                break;
            case SLEEP_PILL_LOW_BATTERY:
                preferences.edit()
                           .putLong(PreferencesInteractor.SYSTEM_ALERT_LAST_SHOWN,
                                    DateTimeUtils.currentTimeMillis())
                           .apply();
                break;
            case SLEEP_PILL_MISSING:
                preferences.edit()
                           .putLong(PreferencesInteractor.PILL_MISSING_ALERT_LAST_SHOWN,
                                    DateTimeUtils.currentTimeMillis())
                           .apply();
                break;
        }
    }

    public enum Issue {
        NONE(0, 0, 0, 0),
        NO_SENSE_PAIRED(Analytics.Timeline.SYSTEM_ALERT_TYPE_SENSE_NOT_PAIRED,
                        R.string.issue_title_no_sense,
                        R.string.issue_message_no_sense,
                        R.string.action_fix_now),
        SENSE_MISSING(Analytics.Timeline.SYSTEM_ALERT_TYPE_SENSE_NOT_SEEN,
                      R.string.issue_title_missing_sense,
                      R.string.issue_message_missing_sense,
                      R.string.action_fix_now),
        NO_SLEEP_PILL_PAIRED(Analytics.Timeline.SYSTEM_ALERT_TYPE_PILL_NOT_PAIRED,
                             R.string.issue_title_no_pill,
                             R.string.issue_message_no_pill,
                             R.string.action_fix_now),
        SLEEP_PILL_LOW_BATTERY(Analytics.Timeline.SYSTEM_ALERT_TYPE_PILL_LOW_BATTERY,
                               R.string.issue_title_low_battery,
                               R.string.issue_message_low_battery,
                               R.string.action_replace),
        SLEEP_PILL_MISSING(Analytics.Timeline.SYSTEM_ALERT_TYPE_PILL_NOT_SEEN,
                           R.string.issue_title_missing_pill,
                           R.string.issue_message_missing_pill,
                           R.string.action_fix_now),
        SLEEP_PILL_FIRMWARE_UPDATE_AVAILABLE(Analytics.Timeline.SYSTEM_ALERT_TYPE_PILL_FIRMWARE_UPDATE_AVAILABLE,
                                             R.string.issue_title_pill_firmware_update_available,
                                             R.string.issue_message_pill_firmware_update_available,
                                             R.string.action_update_now);

        public final int systemAlertType;
        public final @StringRes int titleRes;
        public final @StringRes int messageRes;
        public final @StringRes int buttonActionRes;

        Issue(final int systemAlertType,
              @StringRes final int titleRes,
              @StringRes final int messageRes,
              @StringRes final int buttonActionRes) {
            this.systemAlertType = systemAlertType;
            this.titleRes = titleRes;
            this.messageRes = messageRes;
            this.buttonActionRes = buttonActionRes;
        }
    }
}
