package is.hello.sense.ui.widget.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.support.annotation.NonNull;

public class Dialogs {
    /**
     * This is a hack! But I currently don't have a real solution for long-lived dialogs
     * being displayed on the {@link is.hello.sense.ui.fragments.TimelineFragment} class.
     * Target fragments currently cannot be used with the timeline due to fragments not
     * necessarily surviving state restoration (yay date fast forwarding). So. Punting on
     * the problem until I can figure out a real solution and apply it everywhere. -km
     */
    public static void disableOrientationChangesUntilDismissed(@NonNull Dialog dialog, @NonNull Activity onActivity) {
        int oldOrientation = onActivity.getRequestedOrientation();
        onActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        dialog.setOnDismissListener(ignored -> onActivity.setRequestedOrientation(oldOrientation));
    }
}
