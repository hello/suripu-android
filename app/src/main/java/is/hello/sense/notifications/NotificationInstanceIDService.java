package is.hello.sense.notifications;

import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class NotificationInstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = NotificationInstanceIDService.class.getSimpleName();

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        final String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        if(refreshedToken != null) {
            LocalBroadcastManager.getInstance(this)
                                 .sendBroadcast(NotificationRegistrationBroadcastReceiver.getRegisterIntent(refreshedToken));
        }
    }
}
