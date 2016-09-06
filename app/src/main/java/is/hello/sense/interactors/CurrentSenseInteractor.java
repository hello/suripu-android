package is.hello.sense.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

/**
 * Stores current sense device to be referenced later when this sense needs to be reset in {@link is.hello.sense.presenters.SenseResetOriginalPresenter}.
 */
public class CurrentSenseInteractor extends ValueInteractor<SenseDevice>{

    private final DevicesInteractor devicesInteractor;
    public InteractorSubject<SenseDevice> senseDevice = this.subject;

    public CurrentSenseInteractor(@NonNull final DevicesInteractor devicesInteractor){
        this.devicesInteractor = devicesInteractor;
    }

    @Override
    protected boolean isDataDisposable() {
        return false;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<SenseDevice> provideUpdateObservable() {
        return devicesInteractor.provideUpdateObservable()
                                .map(Devices::getSense);
    }

    @Nullable //todo how to ensure value stored is retained?
    public SenseDevice getCurrentSense(){
        return senseDevice.getValue();
    }
}
