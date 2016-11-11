package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Alert;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class AlertsInteractor extends ValueInteractor<ArrayList<Alert>> {


    private final ApiService apiService;

    public InteractorSubject<ArrayList<Alert>> alerts = this.subject;

    public AlertsInteractor(@NonNull final ApiService apiService){
        this.apiService = apiService;
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
    protected Observable<ArrayList<Alert>> provideUpdateObservable() {
        return apiService.getAlerts();
    }
}
