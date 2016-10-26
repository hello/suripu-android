package is.hello.sense.interactors;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import is.hello.sense.util.Constants;
import is.hello.sense.util.InternalPrefManager;

/**
 * Don't inject this.
 * <p>
 * This class is for tracking account specific preferences that are intended to persist after
 * logging out.
 * <p>
 * When clearing data for testing purposes its important to use {@link #reset()} so any subscribers
 * are alerted of the changes. Calling {@link #edit()} and {@link SharedPreferences.Editor#clear()}
 * will not tell the subscribers to update.
 */
public class AccountPreferencesInteractor extends BasePreferencesInteractor {
    private static final String ACCOUNT_PREFS = "account_prefs";

    public static final String VOICE_WELCOME_CARD = "VOICE_WELCOME_CARD";

    /**
     * Instantiate with this method so the shared preferences used are unique to the account.
     *
     * @param context needed to get a {@link SharedPreferences} object.
     * @return using a unique {@link SharedPreferences} object for the current user.
     */
    public static AccountPreferencesInteractor newInstance(@NonNull final Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(InternalPrefManager.getAccountId(context) + ACCOUNT_PREFS, Context.MODE_PRIVATE);
        return new AccountPreferencesInteractor(context, sharedPreferences);
    }

    private AccountPreferencesInteractor(@NonNull final Context context,
                                         @NonNull final SharedPreferences sharedPreferences) {
        super(context, sharedPreferences);
    }

    /**
     * Manually reset any keys added to this class. This means any additional keys that are added
     * should have a default value they're reset to in this method.
     */
    public void reset() {
        edit().putBoolean(VOICE_WELCOME_CARD, false)
              .apply();
    }


}
