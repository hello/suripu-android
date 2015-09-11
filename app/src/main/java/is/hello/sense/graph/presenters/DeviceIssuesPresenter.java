package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Device;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.Analytics;
import rx.Observable;

public class DeviceIssuesPresenter extends ScopedValuePresenter<DeviceIssuesPresenter.Issue> {
    @Inject ApiService apiService;

    public final PresenterSubject<Issue> topIssue = this.subject;

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
                         .map(DeviceIssuesPresenter::getTopIssue);
    }


    public static Issue getTopIssue(@NonNull List<Device> devices) {
        Map<Device.Type, Device> devicesMap = Device.getDevicesMap(devices);
        Device sense = devicesMap.get(Device.Type.SENSE);
        Device pill = devicesMap.get(Device.Type.PILL);

        if (sense != null) {
            Analytics.setSenseId(sense.getDeviceId());
        }

        if (sense == null) {
            return Issue.NO_SENSE_PAIRED;
        } else if (sense.getHoursSinceLastUpdated() >= Device.MISSING_THRESHOLD_HRS) {
            return Issue.SENSE_MISSING;
        } else if (pill == null) {
            return Issue.NO_SLEEP_PILL_PAIRED;
        } else if (pill.getState() == Device.State.LOW_BATTERY) {
            return Issue.SLEEP_PILL_LOW_BATTERY;
        } else if (pill.getHoursSinceLastUpdated() >= Device.MISSING_THRESHOLD_HRS) {
            return Issue.SLEEP_PILL_MISSING;
        }

        return Issue.NONE;
    }


    public enum Issue {
        NONE(0, 0, 0),
        NO_SENSE_PAIRED(Analytics.Timeline.SYSTEM_ALERT_TYPE_SENSE_NOT_PAIRED,
                        R.string.issue_title_no_sense,
                        R.string.issue_message_no_sense),
        SENSE_MISSING(Analytics.Timeline.SYSTEM_ALERT_TYPE_SENSE_NOT_SEEN,
                      R.string.issue_title_missing_sense,
                      R.string.issue_message_missing_sense),
        NO_SLEEP_PILL_PAIRED(Analytics.Timeline.SYSTEM_ALERT_TYPE_PILL_NOT_PAIRED,
                             R.string.issue_title_no_pill,
                             R.string.issue_message_no_pill),
        SLEEP_PILL_LOW_BATTERY(Analytics.Timeline.SYSTEM_ALERT_TYPE_PILL_LOW_BATTERY,
                               R.string.issue_title_low_battery,
                               R.string.issue_message_low_battery),
        SLEEP_PILL_MISSING(Analytics.Timeline.SYSTEM_ALERT_TYPE_PILL_NOT_SEEN,
                           R.string.issue_title_missing_pill,
                           R.string.issue_message_missing_pill);

        public final int systemAlertType;
        public final @StringRes
        int titleRes;
        public final @StringRes int messageRes;

        Issue(int systemAlertType,
              @StringRes int titleRes,
              @StringRes int messageRes) {
            this.systemAlertType = systemAlertType;
            this.titleRes = titleRes;
            this.messageRes = messageRes;
        }
    }
}
