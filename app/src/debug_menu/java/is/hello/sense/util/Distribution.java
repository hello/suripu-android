package is.hello.sense.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import net.hockeyapp.android.UpdateManager;

import is.hello.sense.BuildConfig;
import is.hello.sense.ui.activities.DebugActivity;

public class Distribution {
    public static void checkForUpdates(@NonNull Activity activity) {
        UpdateManager.register(activity, BuildConfig.HOCKEY_API_KEY);
    }

    public static void startDebugActivity(@NonNull Activity from) {
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            Intent intent = new Intent(from, DebugActivity.class);
            from.startActivity(intent);
        }
    }

    public static void showDebugEnvironment(@NonNull final Context context) {
        try {
            context.startActivity(new Intent(context, Class.forName("is.hello.sense.debug_menu_ui.EnvironmentActivity")));
        } catch (final ClassNotFoundException e) {
            Log.e(Distribution.class.getSimpleName(), "Could not find environment activity", e);
        }
    }
}
