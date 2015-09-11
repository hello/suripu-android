package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

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
}
