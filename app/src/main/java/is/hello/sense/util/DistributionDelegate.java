package is.hello.sense.util;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

interface DistributionDelegate {
    void checkForUpdates(@NonNull final Activity activity);

    void startDebugActivity(@NonNull final Activity from);

    void showDebugEnvironment(@NonNull final Context context);
}
