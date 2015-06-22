package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.Analytics;
import rx.Observable;

@Singleton
public class DevicesPresenter extends ValuePresenter<ArrayList<Device>> {
    @Inject ApiService apiService;

    public final PresenterSubject<ArrayList<Device>> devices = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<ArrayList<Device>> provideUpdateObservable() {
        return apiService.registeredDevices();
    }

    public Observable<VoidResponse> unregisterDevice(@NonNull Device device) {
        switch (device.getType()) {
            case PILL:
                return apiService.unregisterPill(device.getDeviceId());

            case SENSE:
                return apiService.unregisterSense(device.getDeviceId());

            case OTHER:
            default:
                return Observable.error(new Exception("Unknown device type '" + device.getType() + "'"));
        }
    }

    public Observable<VoidResponse> removeSenseAssociations(@NonNull Device senseDevice) {
        if (senseDevice.getType() != Device.Type.SENSE) {
            return Observable.error(new InvalidParameterException("removeSenseAssociations requires Type.SENSE"));
        }

        return apiService.removeSenseAssociations(senseDevice.getDeviceId());
    }

    public static Issue topIssue(@NonNull List<Device> devices) {
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

    public Observable<Issue> latestTopIssue() {
        return latest().map(DevicesPresenter::topIssue);
    }


    public enum Issue {
        NONE(0, 0),
        NO_SENSE_PAIRED(R.string.issue_title_no_sense, R.string.issue_message_no_sense),
        SENSE_MISSING(R.string.issue_title_missing_sense, R.string.issue_message_missing_sense),
        NO_SLEEP_PILL_PAIRED(R.string.issue_title_no_pill, R.string.issue_message_no_pill),
        SLEEP_PILL_LOW_BATTERY(R.string.issue_title_low_battery, R.string.issue_message_low_battery),
        SLEEP_PILL_MISSING(R.string.issue_title_missing_pill, R.string.issue_message_missing_pill);

        public final @StringRes int titleRes;
        public final @StringRes int messageRes;

        Issue(@StringRes int titleRes, @StringRes int messageRes) {
            this.titleRes = titleRes;
            this.messageRes = messageRes;
        }
    }
}
