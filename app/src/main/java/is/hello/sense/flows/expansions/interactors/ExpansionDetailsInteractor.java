package is.hello.sense.flows.expansions.interactors;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

import static is.hello.sense.api.model.v2.expansions.Expansion.NO_ID;

public class ExpansionDetailsInteractor extends ValueInteractor<Expansion> {

    private static final String KEY_EXPANSION_ID = ExpansionDetailsInteractor.class.getName() + "KEY_EXPANSION_ID";
    private final ApiService apiService;
    private long id = NO_ID;
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
        if(this.id == NO_ID){
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

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        super.onRestoreState(savedState);
        this.id = savedState.getLong(KEY_EXPANSION_ID, NO_ID);
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle bundle = super.onSaveState();
        if(bundle == null){
            bundle = new Bundle();
        }
        bundle.putLong(KEY_EXPANSION_ID, id);
        return bundle;
    }

    public Observable<Void> setState(@NonNull final State state) {
        return apiService.setExpansionState(id, State.Request.with(state));
    }

    public void setId(final long id){
        this.id = id;
    }
}
