package is.hello.sense.flows.expansions.interactors;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
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

    public Observable<Expansion> findByCategories(@NonNull final List<Category> category){
        return expansions.flatMap(Observable::from)
                       .filter( expansion -> category.contains(expansion.getCategory()));
    }
}
