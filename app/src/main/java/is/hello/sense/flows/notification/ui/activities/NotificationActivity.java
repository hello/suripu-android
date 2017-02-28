package is.hello.sense.flows.notification.ui.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.flows.notification.ui.fragments.NotificationFragment;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;

public class NotificationActivity extends FragmentNavigationActivity {

    public static void startActivity(@NonNull final Context context) {
        context.startActivity(new Intent(context, NotificationActivity.class));
    }

    @Override
    protected void onCreateAction() {
        showNotificationsFragment();
    }

    @Override
    public void flowFinished(@NonNull final Fragment fragment,
                             final int responseCode,
                             @Nullable final Intent result) {
        super.flowFinished(fragment, responseCode, result);
        finish(); // as of right now only activity ok is used.
    }

    public void showNotificationsFragment() {
        pushFragment(new NotificationFragment(), null, false);
    }
}
