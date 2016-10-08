package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.api.model.v2.expansions.Expansion;
import rx.Observable;

public class ConfigurationsInteractor extends ValueInteractor<ArrayList<Configuration>> {
    @Inject
    ApiService apiService;

    private long expansionId = -1;

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

    public void setExpansion(@NonNull final Expansion expansion) {
        this.expansionId = expansion.getId();
    }
}
