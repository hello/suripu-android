package is.hello.sense.interactors; // Because provideUpdateObservable is package protected we can't use this class in our flow package,

import android.support.annotation.NonNull;

import javax.inject.Inject;

import is.hello.sense.functional.Functions;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

/**
 * Temporary interactor responsible for tracking whether or not the device associated with this
 * account has voice. Will use {@link DevicesInteractor} and {@link PreferencesInteractor}.
 * <p>
 * todo We should figure out a way to get the functions this class offers into {@link DevicesInteractor}
 */
public class HasVoiceInteractor extends ValueInteractor<Boolean> {

    private final DevicesInteractor devicesInteractor;
    private final PreferencesInteractor preferencesInteractor;

    public InteractorSubject<Boolean> hasVoice = this.subject;

    @Inject
    public HasVoiceInteractor(@NonNull final DevicesInteractor devicesInteractor,
                              @NonNull final PreferencesInteractor preferencesInteractor) {
        this.devicesInteractor = devicesInteractor;
        this.preferencesInteractor = preferencesInteractor;
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Boolean> provideUpdateObservable() {
        devicesInteractor.provideUpdateObservable()
                         .subscribe(devices -> {
                                        preferencesInteractor.setDevice(devices.getSense());
                                    },
                                    Functions.LOG_ERROR);
        return preferencesInteractor.observableBoolean(PreferencesInteractor.HAS_VOICE, false);

    }

}
