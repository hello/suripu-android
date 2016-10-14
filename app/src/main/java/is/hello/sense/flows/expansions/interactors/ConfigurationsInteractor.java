package is.hello.sense.flows.expansions.interactors;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

import static is.hello.sense.api.model.v2.expansions.Expansion.NO_ID;

public class ConfigurationsInteractor extends ValueInteractor<ArrayList<Configuration>> {

    private static final String KEY_EXPANSION_ID = ConfigurationsInteractor.class.getName() + "KEY_EXPANSION_ID";
    private final ApiService apiService;

    private long expansionId = NO_ID;
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
        if (expansionId == NO_ID){
            return Observable.just(new ArrayList<>());
        }
        return apiService.getConfigurations(expansionId);
    }

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        super.onRestoreState(savedState);
        this.expansionId = savedState.getLong(KEY_EXPANSION_ID, NO_ID);
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle bundle = super.onSaveState();
        if(bundle == null){
            bundle = new Bundle();
        }
        bundle.putLong(KEY_EXPANSION_ID, expansionId);
        return bundle;
    }

    public Observable<Configuration> setConfiguration(@NonNull final Configuration configuration){
        return apiService.setConfigurations(expansionId, configuration);
    }

    public void setExpansionId(final long id) {
        this.expansionId = id;
    }
}
