package is.hello.sense.flows.nightmode.interactors;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatDelegate;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.TimeZone;

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.nightmode.NightMode;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.LocationInteractor;
import is.hello.sense.interactors.PersistentPreferencesInteractor;
import is.hello.sense.interactors.ValueInteractor;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.InternalPrefManager;
import rx.Observable;

import static is.hello.sense.flows.nightmode.NightMode.AUTO;
import static is.hello.sense.flows.nightmode.NightMode.OFF;
import static is.hello.sense.flows.nightmode.NightMode.ON;

public class NightModeInteractor extends ValueInteractor<Integer> {

    private static final String NIGHT_MODE_PREF = "night_mode_pref_for_account_id";
    private final PersistentPreferencesInteractor persistentPreferencesInteractor;
    private final Context applicationContext;

    public final InteractorSubject<Integer> currentNightMode = this.subject;
    private final ApiSessionManager apiSessionManager;
    protected final LocationInteractor locationInteractor;

    public NightModeInteractor(@NonNull final PersistentPreferencesInteractor persistentPreferencesInteractor,
                               @NonNull final ApiSessionManager apiSessionManager,
                               @NonNull final Context applicationContext,
                               @NonNull final LocationInteractor locationInteractor) {
        super();
        this.apiSessionManager = apiSessionManager;
        this.persistentPreferencesInteractor = persistentPreferencesInteractor;
        this.applicationContext = applicationContext;
        this.locationInteractor = locationInteractor;
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
        if(apiSessionManager.hasSession()) {
            return persistentPreferencesInteractor.getInt(getNightModePrefKey(),
                                                          getDefaultMode());
        } else {
            return getDefaultMode();
        }
    }

    private String getNightModePrefKey() {
        return NIGHT_MODE_PREF + InternalPrefManager.getAccountId(applicationContext);
    }

    @NightMode
    private int getDefaultMode() {
        return OFF;
    }

    @AppCompatDelegate.NightMode
    private int getAppCompatMode(final int mode) {
        switch (mode) {
            case OFF:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case ON:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case AUTO:
                return AppCompatDelegate.MODE_NIGHT_NO;
            default:
                logEvent("no case found for mode defaulting to off.");
                return AppCompatDelegate.MODE_NIGHT_NO;
        }
    }

    public void setMode(@NightMode final int mode) {

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
        @NightMode
        int accountPrefNightMode = getCurrentMode();

        if (accountPrefNightMode == AUTO) {
            accountPrefNightMode = getModeBasedOnLocationAndTime();
        }

        AppCompatDelegate.setDefaultNightMode(getAppCompatMode(accountPrefNightMode));
        update();
    }

    @VisibleForTesting
    @NightMode
    protected int getModeBasedOnLocationAndTime() {
        final Location location = locationInteractor.getLastKnownLocation();
        if (location == null) {
            return getDefaultMode();
        }
        return isNightTime(location,
                           TimeZone.getDefault(),
                           DateTime.now()) ? ON : OFF;
    }

    @VisibleForTesting
    protected boolean isNightTime(@NonNull final Location location,
                                  @NonNull final TimeZone timeZone,
                                  @NonNull final DateTime dateTime) {
        logEvent(String.format("location %s \n timezone %s", location, timeZone));
        final com.luckycatlabs.sunrisesunset.dto.Location libLocation = new com.luckycatlabs.sunrisesunset.dto.Location(location.getLatitude(),
                                                                                                                      location.getLongitude());
        final SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(libLocation, timeZone);

        return !DateFormatter.isBetween(dateTime,
                                       new DateTime(calculator.getOfficialSunriseCalendarForDate(Calendar.getInstance()).getTimeInMillis()),
                                       new DateTime(calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance()).getTimeInMillis()));
    }

    /**
     * @param initialConfigMode should be fetched from {@link NightModeInteractor#getConfigMode(Resources)}
     *                          during onCreate to compare after updates occur.
     * @param current resource used to derive current config mode.
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
}
