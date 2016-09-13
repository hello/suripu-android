package is.hello.sense.interactors;


import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.CurrentConditions;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class CurrentConditionsInteractor extends ValueInteractor<CurrentConditions> {
    @Inject
    ApiService apiService;

    public final InteractorSubject<CurrentConditions> sensors = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return false;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<CurrentConditions> provideUpdateObservable() {
        return apiService.getSensors();
    }

}
