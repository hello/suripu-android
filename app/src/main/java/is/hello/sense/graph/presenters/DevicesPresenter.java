package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.PresenterSubject;
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

    public Observable<Void> unregisterAllDevices() {
        return devices.flatMap(devices -> {
            List<Observable<VoidResponse>> unregisterCalls = Lists.map(devices, this::unregisterDevice);
            return Observable.combineLatest(unregisterCalls, ignored -> null);
        });
    }
}
