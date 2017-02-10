package is.hello.sense.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

public class NotificationRegistrationBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_FILTER = NotificationRegistrationBroadcastReceiver.class.getSimpleName() + ".ACTION";
    static final String EXTRA_TOKEN = NotificationRegistrationBroadcastReceiver.class.getSimpleName()+ ".TOKEN";

    public static Intent getIntent(@Nullable final String token) {
        return new Intent(ACTION_FILTER)
                .putExtra(EXTRA_TOKEN, token);
    }

    @Override
    public void onReceive(@NonNull final Context context,
                          @NonNull final Intent intent) {
        if(intent.getAction().equals(ACTION_FILTER)) {
            String token = intent.getStringExtra(EXTRA_TOKEN);
            if(token == null) {
                token = FirebaseInstanceId.getInstance().getToken();
            }

            if(token != null) {
                sendRegistrationToServer(token, context);
            } else  {
                Log.e(NotificationRegistrationBroadcastReceiver.class.getSimpleName(),
                      "no Firebase token available. Failed to register to servers.");
            }
        }
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(@NonNull final String token,
                                          @NonNull final Context context) {
        final NotificationRegistration notificationRegistration = new NotificationRegistration(context);
        notificationRegistration.register(token);
    }
}
