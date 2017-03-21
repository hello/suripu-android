package is.hello.sense.flows.nightmode.interactors;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.PersistentPreferencesInteractor;
import is.hello.sense.interactors.ValueInteractor;
import is.hello.sense.util.InternalPrefManager;
import rx.Observable;

public class NightModeInteractor extends ValueInteractor<Integer> {

    private static final String NIGHT_MODE_PREF = "night_mode_pref_for_account_id";
    private final PersistentPreferencesInteractor persistentPreferencesInteractor;
    private final Context applicationContext;

    public final InteractorSubject<Integer> currentNightMode = this.subject;
    private final ApiSessionManager apiSessionManager;

    public NightModeInteractor(@NonNull final PersistentPreferencesInteractor persistentPreferencesInteractor,
                               @NonNull final ApiSessionManager apiSessionManager,
                               @NonNull final Context applicationContext) {
        super();
        this.apiSessionManager = apiSessionManager;
        this.persistentPreferencesInteractor = persistentPreferencesInteractor;
        this.applicationContext = applicationContext;
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
    protected Observable<Integer> provideUpdateObservable() {
        if(apiSessionManager.hasSession()) {
            return Observable.just(persistentPreferencesInteractor.getInt(getNightModePrefKey(),
                                                                          getDefaultMode()));
        } else {
            return Observable.just(getDefaultMode());
        }
    }

    private String getNightModePrefKey() {
        return NIGHT_MODE_PREF + InternalPrefManager.getAccountId(applicationContext);
    }

    @AppCompatDelegate.NightMode
    private int getDefaultMode() {
        return AppCompatDelegate.MODE_NIGHT_NO;
    }

    public void setMode(@AppCompatDelegate.NightMode final int mode) {

        persistentPreferencesInteractor.edit()
                                       .putInt(getNightModePrefKey(), mode)
                                       .commit();

        updateToMatchPrefAndSession();
    }

    /**
     * Need to call this every time application is restarted.
     *
     * Uses previously stored pref or default mode to update application wide night mode.
     * If no user session (logged out) also use default mode;
     */
    public void updateToMatchPrefAndSession() {
        if(!apiSessionManager.hasSession()) {
            AppCompatDelegate.setDefaultNightMode(getDefaultMode());
            return;
        }
        @AppCompatDelegate.NightMode
        final int accountPrefNightMode = persistentPreferencesInteractor.getInt(getNightModePrefKey(),
                                                                                getDefaultMode());
        AppCompatDelegate.setDefaultNightMode(accountPrefNightMode);
        update();
    }
}
