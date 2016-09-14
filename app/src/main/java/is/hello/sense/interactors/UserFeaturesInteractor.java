package is.hello.sense.interactors;

import android.content.Context;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.UserFeatures;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.UserFeaturesManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

@Singleton
public class UserFeaturesInteractor extends ValueInteractor<UserFeatures> {

    public static final int DEFAULT_NUM_RETRIES = 2;
    private final ApiService apiService;
    private final UserFeaturesManager userFeaturesManager;
    public InteractorSubject<UserFeatures> featureSubject = this.subject;

    @Inject public UserFeaturesInteractor(@NonNull final Context context,
                                          @NonNull final ApiService apiService,
                                          @NonNull final UserFeaturesManager userFeaturesManager){
        Rx.fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT))
                .subscribe(ignored -> reset(), Functions.LOG_ERROR);
        this.apiService = apiService;
        this.userFeaturesManager = userFeaturesManager;
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
        return apiService.getUserFeatures();
    }

    /**
     * Circumvents the presenter subject to store features in preferences.
     * Retries {@link this#DEFAULT_NUM_RETRIES} times on error.
     * @return Void Observable
     */
    public Observable<Void> storeFeaturesInPrefs() {
        return storeFeaturesInPrefs(DEFAULT_NUM_RETRIES);
    }

    /**
     * Circumvents the presenter subject to store features in preferences.
     * @param numRetries is the number of retry attempts to make if an error is thrown
     * @return Void Observable
     */
    public Observable<Void> storeFeaturesInPrefs(final int numRetries) {
        return provideUpdateObservable()
                .retry(numRetries)
                .map( features -> {
                    setFeatures(features);
                    return null;
                });
    }

    public boolean hasVoice(){
        return userFeaturesManager.hasFeatures()
                && userFeaturesManager.getFeatures() != null
                && userFeaturesManager.getFeatures().voice;
    }

    public void reset() {
        userFeaturesManager.setFeatures(null);
        featureSubject.forget();
    }

    private void setFeatures(final UserFeatures features) {
        logEvent("Pulling user features from backend");
        userFeaturesManager.setFeatures(features);
        logEvent("Pulled user features");
    }

}
