package is.hello.sense.interactors;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.expansions.Expansion;
import rx.Observable;

public class ExpansionsInteractor extends ValueInteractor<ArrayList<Expansion>> {
    @Inject
    ApiService apiService;

    @Override
    protected boolean isDataDisposable() {
        return false;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<ArrayList<Expansion>> provideUpdateObservable() {
        return apiService.getExpansions();
    }
}
