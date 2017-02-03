package is.hello.sense.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.activities.HomeActivity;

/**
 * Handles receiving downstream messages
 */

public class NotificationMessageService extends FirebaseMessagingService {
    private static final String TAG = NotificationMessageService.class.getSimpleName();
    private int notificationId = 0; //todo should reset?

    @Override
    public void onMessageReceived(@NonNull final RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options


        Log.d(TAG, "FCM from: " + remoteMessage.getFrom());
        Log.d(TAG, "FCM Message Id: " + remoteMessage.getMessageId());
        Log.d(TAG, "FCM Notification Message: " + remoteMessage.getNotification());
        Log.d(TAG, "FCM Data Message: " + remoteMessage.getData());

        sendNotification(remoteMessage);
    }

    private void sendNotification(final RemoteMessage remoteMessage) {

        final CharSequence messageText;
        if(remoteMessage.getNotification() != null) {
            messageText = remoteMessage.getNotification().getBody();
        } else {
            //todo better default
            messageText = getString(R.string.lorem_ipsum);
        }

        final Context context = getApplicationContext();

        final Intent activityIntent = new Intent(context, HomeActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        //activityIntent.putExtra(HomeActivity.EXTRA_NOTIFICATION_PAYLOAD, intent.getExtras());

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_notify_msg);
        builder.setColor(ContextCompat.getColor(context, R.color.light_accent));
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setContentText(messageText);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(messageText));
        builder.setContentIntent(PendingIntent.getActivity(context, 0,
                                                           activityIntent,
                                                           PendingIntent.FLAG_ONE_SHOT));
        builder.setAutoCancel(true);

        final NotificationManagerCompat manager =
                NotificationManagerCompat.from(context);
        manager.notify(notificationId++, builder.build());
    }
}
