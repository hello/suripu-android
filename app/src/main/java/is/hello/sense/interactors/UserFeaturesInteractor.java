package is.hello.sense.interactors;

import android.support.annotation.NonNull;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.UserFeatures;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

@Singleton
public class UserFeaturesInteractor extends ValueInteractor<UserFeatures> {

    public static final int DEFAULT_NUM_RETRIES = 2;
    private final ApiService apiService;
    public InteractorSubject<UserFeatures> featureSubject = this.subject;

    @Inject
    public UserFeaturesInteractor(@NonNull final ApiService apiService) {
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
    protected Observable<UserFeatures> provideUpdateObservable() {
        return apiService.getUserFeatures()
                         .retry(DEFAULT_NUM_RETRIES);
    }

}
