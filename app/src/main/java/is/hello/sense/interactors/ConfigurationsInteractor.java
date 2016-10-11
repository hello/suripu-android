package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class ConfigurationsInteractor extends ValueInteractor<ArrayList<Configuration>> {

    private final ApiService apiService;

    private long expansionId = -1;
    public InteractorSubject<ArrayList<Configuration>> configSubject = this.subject;

    public ConfigurationsInteractor(@NonNull final ApiService apiService){
        this.apiService = apiService;
    }

    @Override
    protected boolean isDataDisposable() {
        return false;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<ArrayList<Configuration>> provideUpdateObservable() {
        if (expansionId == -1){
            return Observable.just(new ArrayList<>());
        }
        return apiService.getConfigurations(expansionId);
    }

    public void setExpansionId(final long id) {
        this.expansionId = id;
    }
}
