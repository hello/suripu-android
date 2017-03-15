package is.hello.sense.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Devices;
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
                         .flatMap(SleepSoundsInteractor.this::hasSensePaired);
    }

    @VisibleForTesting
    protected Observable<Boolean> hasSensePaired(@Nullable final Devices devices) {
        final boolean paired = !(devices == null || devices.getSense() == null);
        if(paired) {
            return Observable.just(true);
        } else {
            return Observable.error(new SenseRequiredException());
        }
    }
}
