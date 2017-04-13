package is.hello.sense.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import net.hockeyapp.android.UpdateManager;

import is.hello.sense.BuildConfig;
import is.hello.sense.ui.activities.DebugActivity;

class DistributionDelegateImpl implements DistributionDelegate {

    @Override
    public void checkForUpdates(@NonNull final Activity activity) {
        UpdateManager.register(activity, BuildConfig.HOCKEY_API_KEY);
    }

    @Override
    public void startDebugActivity(@NonNull final Activity from) {
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            final Intent intent = new Intent(from, DebugActivity.class);
            from.startActivity(intent);
        }
    }

    @Override
    public void showDebugEnvironment(@NonNull final Context context) {
        try {
            context.startActivity(new Intent(context, Class.forName("is.hello.sense.debug_menu_ui.EnvironmentActivity")));
        } catch (final ClassNotFoundException e) {
            Log.e(DistributionDelegateImpl.class.getSimpleName(), "Could not find environment activity", e);
        }
    }
}
