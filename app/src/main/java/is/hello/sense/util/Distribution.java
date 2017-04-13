package is.hello.sense.util;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

public class Distribution {
    /*Fetch instance of delegate interface based on built variant*/
    private final static DistributionDelegate delegate = new DistributionDelegateImpl();

    public static void checkForUpdates(@NonNull final Activity activity) {
        delegate.checkForUpdates(activity);
    }

    public static void startDebugActivity(@NonNull final Activity from) {
        delegate.startDebugActivity(from);
    }

    public static void showDebugEnvironment(@NonNull final Context context) {
        delegate.showDebugEnvironment(context);
    }
}
