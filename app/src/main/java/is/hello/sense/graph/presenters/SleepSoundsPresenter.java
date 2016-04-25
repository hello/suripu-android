package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.SleepSoundActionPlay;
import is.hello.sense.api.model.v2.SleepSoundActionStop;
import is.hello.sense.api.model.v2.SleepSoundsStateDevice;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

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

    public Observable<VoidResponse> play(final @NonNull SleepSoundActionPlay actionPlay) {
        // todo validate actionPlay
        return latest().flatMap(a -> {
            return apiService.playSleepSound(actionPlay);
        });
    }

    public Observable<VoidResponse> stop(final @NonNull SleepSoundActionStop actionStop) {
        // todo validate action stop
        return latest().flatMap(a -> {
            return apiService.stopSleepSound(actionStop);
        });
    }
}
