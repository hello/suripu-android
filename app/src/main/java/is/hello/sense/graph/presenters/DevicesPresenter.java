package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class DevicesPresenter extends ValuePresenter<Devices> {
    @Inject ApiService apiService;

    public final PresenterSubject<Devices> devices = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Devices> provideUpdateObservable() {
        return apiService.registeredDevices();
    }

    public Observable<VoidResponse> unregisterDevice(@NonNull BaseDevice device) {
        if (device instanceof SleepPillDevice) {
            return apiService.unregisterPill(device.deviceId);
        } else if (device instanceof SenseDevice) {
            return apiService.unregisterSense(device.deviceId);
        } else {
            return Observable.error(new Exception("Unknown device type '" + device.getClass() + "'"));
        }
    }

    public Observable<VoidResponse> removeSenseAssociations(@NonNull SenseDevice senseDevice) {
        return apiService.removeSenseAssociations(senseDevice.deviceId);
    }
}
