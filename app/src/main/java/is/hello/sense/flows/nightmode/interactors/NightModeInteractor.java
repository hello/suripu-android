package is.hello.sense.flows.nightmode.interactors;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatDelegate;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.TimeZone;

import is.hello.sense.api.model.UserLocation;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.PersistentPreferencesInteractor;
import is.hello.sense.interactors.ValueInteractor;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

public class NightModeInteractor extends ValueInteractor<Integer> {

    private final PersistentPreferencesInteractor persistentPreferencesInteractor;

    public final InteractorSubject<Integer> currentNightMode = this.subject;
    private final ApiSessionManager apiSessionManager;

    public NightModeInteractor(@NonNull final PersistentPreferencesInteractor persistentPreferencesInteractor,
                               @NonNull final ApiSessionManager apiSessionManager) {
        super();
        this.apiSessionManager = apiSessionManager;
        this.persistentPreferencesInteractor = persistentPreferencesInteractor;
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
        return Observable.just(getCurrentMode());
    }

    public Integer getCurrentMode() {
        if (apiSessionManager.hasSession()) {
            return persistentPreferencesInteractor.getCurrentNightMode();
        } else {
            return getDefaultMode();
        }
    }


    @AppCompatDelegate.NightMode
    private int getDefaultMode() {
        return AppCompatDelegate.MODE_NIGHT_NO;
    }

    /**
     * {@link AppCompatDelegate#MODE_NIGHT_AUTO} isn't used.  What looks like AUTO is actually a
     * well made illusion flipping between on and off.
     * @param mode
     */
    public void setMode(@AppCompatDelegate.NightMode final int mode) {
        persistentPreferencesInteractor.saveNightMode(mode);
        updateToMatchPrefAndSession();
    }

    /**
     * Need to call this every time application is restarted.
     * <p>
     * Uses previously stored pref or default mode to update application wide night mode.
     * If no user session (logged out) also use default mode;
     */
    public void updateToMatchPrefAndSession() {
        if (!apiSessionManager.hasSession()) {
            AppCompatDelegate.setDefaultNightMode(getDefaultMode());
            return;
        }
        @AppCompatDelegate.NightMode
        final int accountPrefNightMode = persistentPreferencesInteractor.getCurrentNightMode();
        if (accountPrefNightMode != AppCompatDelegate.MODE_NIGHT_AUTO) {
            AppCompatDelegate.setDefaultNightMode(accountPrefNightMode);
        } else if (isNightTime()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        update();
    }

    /**
     * @param initialConfigMode should be fetched from {@link NightModeInteractor#getConfigMode(Resources)}
     *                          during onCreate to compare after updates occur.
     * @param current           resource used to derive current config mode.
     * @return true if current config mode doesn't match initial config mode
     */
    public boolean requiresRecreate(final int initialConfigMode,
                                    @NonNull final Resources current) {
        final int currentConfigMode = getConfigMode(current);
        return currentConfigMode != initialConfigMode;
    }

    public int getConfigMode(@NonNull final Resources resources) {
        return resources.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }

    private boolean isNightTime() {
        return isNightTime(TimeZone.getDefault(), DateTime.now());
    }

    @VisibleForTesting
    protected boolean isNightTime(@NonNull final TimeZone timeZone,
                                @NonNull final DateTime dateTime) {
        final UserLocation userLocation = persistentPreferencesInteractor.getUserLocation();
        if (userLocation == null) {
            return false;
        }
        final Location location = new Location(userLocation.getLatitude(), userLocation.getLongitude());
        final SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, timeZone);

        return !DateFormatter.isBetween(dateTime,
                                        new DateTime(calculator.getOfficialSunriseCalendarForDate(Calendar.getInstance()).getTimeInMillis()),
                                        new DateTime(calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance()).getTimeInMillis()));
    }
}
