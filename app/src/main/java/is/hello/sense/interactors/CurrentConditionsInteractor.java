package is.hello.sense.interactors;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.sensors.SensorResponse;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class CurrentConditionsInteractor extends ValueInteractor<SensorResponse> {
    @Inject
    ApiService apiService;

    public final InteractorSubject<SensorResponse> sensors = this.subject;

    @Override
    protected boolean isDataDisposable() {
        return false;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<SensorResponse> provideUpdateObservable() {
        return apiService.getSensors();
    }

}
