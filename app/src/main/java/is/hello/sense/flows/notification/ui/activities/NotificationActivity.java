package is.hello.sense.flows.notification.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.flows.notification.ui.fragments.NotificationFragment;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;

public class NotificationActivity extends FragmentNavigationActivity {

    public static void startActivity(@NonNull final Context context) {
        context.startActivity(new Intent(context, NotificationActivity.class));
    }

    @Override
    protected int getHomeAsUpIndicator() {
        return R.drawable.app_style_ab_cancel;
    }

    @Override
    protected void onCreateAction() {
        showNotificationsFragment();
    }

    public void showNotificationsFragment() {
        pushFragment(new NotificationFragment(), null, false);
    }
}
