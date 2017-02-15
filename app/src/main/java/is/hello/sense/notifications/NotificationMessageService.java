package is.hello.sense.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import is.hello.sense.R;
import is.hello.sense.util.Constants;

/**
 * Handles receiving downstream messages
 */

public class NotificationMessageService extends FirebaseMessagingService {
    private static final String TAG = NotificationMessageService.class.getSimpleName();

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

        final String titleText;
        final String bodyText;
        @Notification.Type
        final String type;
        final String detail;
        final Context context = getApplicationContext();
        // currently we ignore any notification object returned from remoteMessage.getNotification();
        if (remoteMessage.getData() != null) {
            final Map<String, String> dataPayload = remoteMessage.getData();
            titleText = dataPayload.get(Notification.REMOTE_TITLE);
            bodyText = dataPayload.get(Notification.REMOTE_BODY);
            type = Notification.typeFromString(dataPayload.get(Notification.REMOTE_TYPE));
            detail = dataPayload.get(Notification.REMOTE_DETAIL);
        } else {
            titleText = context.getString(R.string.app_name);
            bodyText = getString(R.string.empty);
            type = Notification.UNKNOWN;
            detail = Constants.EMPTY_STRING;
        }

        final Bundle bundle = new Bundle();
        bundle.putString(Notification.REMOTE_TITLE, titleText);
        bundle.putString(Notification.REMOTE_BODY, bodyText);
        bundle.putString(Notification.EXTRA_TYPE, type);
        bundle.putString(Notification.EXTRA_DETAILS, detail);

        final Intent broadcastIntent = NotificationMessageReceiver.getIntent(bundle);
        sendOrderedBroadcast(broadcastIntent, null);
    }

    public static void cancelShownMessages(@NonNull final Context context) {
        NotificationManagerCompat.from(context)
                                 .cancelAll();
    }
}
