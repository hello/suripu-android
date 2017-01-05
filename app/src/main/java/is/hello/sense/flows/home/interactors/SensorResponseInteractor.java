package is.hello.sense.flows.home.interactors;

import android.support.annotation.NonNull;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.sensors.SensorDataRequest;
import is.hello.sense.api.model.v2.sensors.SensorResponse;
import is.hello.sense.api.model.v2.sensors.SensorsDataResponse;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

public class SensorResponseInteractor extends ValueInteractor<SensorResponse> {
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

    public Observable<SensorsDataResponse> getDataFrom(@NonNull final SensorDataRequest request) {
        return this.apiService.postSensors(request)
                              .flatMap(sensorsDataResponse -> {
                                  sensorsDataResponse.removeLastInvalidSensorDataValues();
                                  return Observable.just(sensorsDataResponse);
                              });
    }
}
