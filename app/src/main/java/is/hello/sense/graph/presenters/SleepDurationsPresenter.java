package is.hello.sense.graph.presenters;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.SleepDurations;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class SleepDurationsPresenter extends ScopedValuePresenter<SleepDurations> {
    @Inject
    ApiService apiService;

    public final PresenterSubject<SleepDurations> durations = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<SleepDurations> provideUpdateObservable() {
        return apiService.getDurations();
    }



}
