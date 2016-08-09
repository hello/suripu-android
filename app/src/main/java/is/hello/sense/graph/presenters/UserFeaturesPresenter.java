package is.hello.sense.graph.presenters;

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
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

@Singleton
public class UserFeaturesPresenter extends ValuePresenter<UserFeatures>{

    private final ApiService apiService;
    private final UserFeaturesManager userFeaturesManager;
    public PresenterSubject<UserFeatures> featureSubject = this.subject;

    @Inject public UserFeaturesPresenter(@NonNull final Context context,
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
     * @return Void Observable
     */
    public Observable<Void> storeFeaturesInPrefs() {
        return provideUpdateObservable().map( features -> {
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
