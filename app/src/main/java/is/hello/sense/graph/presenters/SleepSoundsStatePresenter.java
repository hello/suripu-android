package is.hello.sense.graph.presenters;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class SleepSoundsStatePresenter extends ScopedValuePresenter<SleepSoundsState> {
    @Inject
    ApiService apiService;

    public final PresenterSubject<SleepSoundsState> state = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<SleepSoundsState> provideUpdateObservable() {
        return apiService.getSleepSoundsCurrentState();
    }

}
