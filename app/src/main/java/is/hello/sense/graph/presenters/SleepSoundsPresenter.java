package is.hello.sense.graph.presenters;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class SleepSoundsPresenter extends ScopedValuePresenter<SleepSounds> {
    @Inject
    ApiService apiService;

    public final PresenterSubject<SleepSounds> sounds = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<SleepSounds> provideUpdateObservable() {
        return apiService.getSounds();
    }

}
