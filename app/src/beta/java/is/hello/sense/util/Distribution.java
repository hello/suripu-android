package is.hello.sense.util;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import is.hello.sense.BuildConfig;
import is.hello.sense.ui.activities.DebugActivity;

@SuppressWarnings("UnusedParameters")
public class Distribution {
    public static void checkForUpdates(@NonNull Activity activity) {
    }

    public static void startDebugActivity(@NonNull Activity from) {
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            Intent intent = new Intent(from, DebugActivity.class);
            from.startActivity(intent);
        }
    }
}
