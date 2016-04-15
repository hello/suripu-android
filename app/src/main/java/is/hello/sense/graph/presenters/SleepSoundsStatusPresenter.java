package is.hello.sense.graph.presenters;



import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class SleepSoundsStatusPresenter extends ScopedValuePresenter<SleepSoundStatus> {
    @Inject
    ApiService apiService;

    public final PresenterSubject<SleepSoundStatus> state = this.subject;

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