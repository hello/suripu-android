package is.hello.sense.interactors;



import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class SleepSoundsStatusInteractor extends ScopedValueInteractor<SleepSoundStatus> {
    @Inject
    ApiService apiService;

    public final InteractorSubject<SleepSoundStatus> state = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<SleepSoundStatus> provideUpdateObservable() {
        return apiService.getSleepSoundStatus();
    }


}