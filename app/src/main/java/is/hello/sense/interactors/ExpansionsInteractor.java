package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class ExpansionsInteractor extends ValueInteractor<ArrayList<Expansion>> {

    private final ApiService apiService;
    public InteractorSubject<ArrayList<Expansion>> expansions = this.subject;

    public ExpansionsInteractor(@NonNull final ApiService apiService){
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
    protected Observable<ArrayList<Expansion>> provideUpdateObservable() {
        return apiService.getExpansions();
    }
}
