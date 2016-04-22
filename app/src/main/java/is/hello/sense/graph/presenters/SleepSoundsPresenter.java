package is.hello.sense.graph.presenters;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.DevicesInfo;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.api.model.v2.SleepSoundsStateDevice;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;
import rx.functions.Func2;

public class SleepSoundsPresenter extends ScopedValuePresenter<SleepSoundsStateDevice> {
    @Inject
    ApiService apiService;

    public final PresenterSubject<SleepSoundsStateDevice> sub = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<SleepSoundsStateDevice> provideUpdateObservable() {
        return Observable.zip(apiService.getSleepSoundsCurrentState(),
                              apiService.registeredDevices(),
                              SleepSoundsStateDevice::new);
    }

}
