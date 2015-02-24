package is.hello.sense.util;

import android.app.Activity;
import android.support.annotation.NonNull;

import net.hockeyapp.android.UpdateManager;

import is.hello.sense.R;

public class Distribution {
    public static void checkForUpdates(@NonNull Activity activity) {
        UpdateManager.register(activity, activity.getString(R.string.build_hockey_id));
    }
}
