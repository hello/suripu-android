package is.hello.sense.flows.expansions.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

public class ExpansionDetailsInteractor extends ValueInteractor<Expansion> {

    private final ApiService apiService;
    private long id = -1;
    public InteractorSubject<Expansion> expansionSubject = this.subject;

    public ExpansionDetailsInteractor(@NonNull final ApiService apiService){
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

    @Nullable
    @Override
    protected Observable<Expansion> provideUpdateObservable() {
        if(this.id == -1){
            return Observable.error(new IllegalStateException("invalid expansion id"));
        }
        return apiService.getExpansionDetail(id).map( expansions -> {
            if(expansions.isEmpty()){
                return null;
            } else {
                return expansions.get(0);
            }
        });
    }

    public Observable<Void> setState(@NonNull final State state) {
        return apiService.setExpansionState(id, State.Request.with(state));
    }

    public void setId(final long id){
        this.id = id;
    }
}
