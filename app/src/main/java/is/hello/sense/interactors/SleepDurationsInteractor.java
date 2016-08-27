package is.hello.sense.interactors;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.SleepDurations;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class SleepDurationsInteractor extends ScopedValueInteractor<SleepDurations> {
    @Inject
    ApiService apiService;

    public final InteractorSubject<SleepDurations> durations = this.subject;

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
