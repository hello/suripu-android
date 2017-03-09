package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.SleepSoundActionPlay;
import is.hello.sense.api.model.v2.SleepSoundActionStop;
import is.hello.sense.api.model.v2.SleepSoundsStateDevice;
import is.hello.sense.bluetooth.exceptions.SenseRequiredException;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class SleepSoundsInteractor extends ScopedValueInteractor<SleepSoundsStateDevice> {
    @Inject
    ApiService apiService;

    public final InteractorSubject<SleepSoundsStateDevice> combinedState = this.subject;

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

    public Observable<VoidResponse> play(final @NonNull SleepSoundActionPlay actionPlay) {
        return latest().flatMap(a -> apiService.playSleepSound(actionPlay));
    }

    public Observable<VoidResponse> stop(final @NonNull SleepSoundActionStop actionStop) {
        return latest().flatMap(a -> apiService.stopSleepSound(actionStop));
    }

    public Observable<Boolean> hasSensePaired() {

        return apiService.registeredDevices()
                         .flatMap(devices -> {
                             final boolean paired = !(devices.getSense() == null || devices.getSense().isMissing());
                             if(paired) {
                                return Observable.just(true);
                             } else {
                                 return Observable.error(new SenseRequiredException());
                             }
                         });
    }
}
