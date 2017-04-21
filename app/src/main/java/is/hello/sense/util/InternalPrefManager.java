package is.hello.sense.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public class InternalPrefManager {
    private static final String EMPTY = "";

    public static SharedPreferences getInternalPrefs(@NonNull final Context context) {
        return context.getSharedPreferences(Constants.INTERNAL_PREFS, Context.MODE_PRIVATE);
    }

    public static void clearPrefs(@NonNull final Context context) {
        getInternalPrefs(context)
                .edit()
                .clear()
                .apply();
    }

    public static void setAccountId(@NonNull final Context context, @Nullable final String id) {
        //  Log.e(InternalPrefManager.class.getSimpleName(), "setAccountId( " + id + " )"); // useful for debugging
        getInternalPrefs(context)
                .edit()
                .putString(Constants.INTERNAL_PREF_ACCOUNT_ID, id == null ? EMPTY : id)
                .apply();
    }

    @NonNull
    public static String getAccountId(@NonNull final Context context) {
        final String id = getInternalPrefs(context)
                .getString(Constants.INTERNAL_PREF_ACCOUNT_ID, EMPTY);
        //  Log.e(InternalPrefManager.class.getSimpleName(), "getAccountId() = " + id); // useful for debugging
        return id;
    }

    public static boolean hasAccountId(@NonNull final Context context) {
        final String accountId = getAccountId(context);
        return !EMPTY.equals(accountId);
    }


}
