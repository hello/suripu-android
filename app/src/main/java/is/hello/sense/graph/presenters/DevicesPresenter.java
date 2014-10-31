package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Device;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class DevicesPresenter extends Presenter {
    @Inject ApiService apiService;

    public final PresenterSubject<List<Device>> devices = PresenterSubject.create();

    @Override
    protected void onReloadForgottenData() {
        update();
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        devices.forget();
        return true;
    }

    public void update() {
        apiService.registeredDevices().subscribe(devices);
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
}
