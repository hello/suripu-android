package is.hello.sense.util;

import android.app.Activity;
import android.support.annotation.NonNull;

import net.hockeyapp.android.UpdateManager;

import is.hello.sense.BuildConfig;

public class Distribution {
    public static void checkForUpdates(@NonNull Activity activity) {
        UpdateManager.register(activity, BuildConfig.HOCKEY_API_KEY);
    }
}
