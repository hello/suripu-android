package is.hello.sense.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import is.hello.sense.R;
import is.hello.sense.util.Logger;

public class NotificationsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        String messageType = gcm.getMessageType(intent);
        switch (messageType) {
            case GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR:
                onMessageSendError(context, intent);
                break;

            case GoogleCloudMessaging.MESSAGE_TYPE_DELETED:
                onMessageDeleted(context, intent);
                break;

            case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE:
                onMessageReceived(context, intent);
                break;

            default:
                Logger.warn(NotificationsReceiver.class.getSimpleName(), "Unrecognized message type: " + messageType);
                break;
        }
    }

    public void onMessageSendError(@NonNull Context context, @NonNull Intent intent) {
        Logger.error(NotificationsReceiver.class.getSimpleName(), "Message send error for: " + intent);
    }

    public void onMessageDeleted(@NonNull Context context, @NonNull Intent intent) {
        Logger.info(NotificationsReceiver.class.getSimpleName(), "Notification was deleted on server: " + intent);
    }

    public void onMessageReceived(@NonNull Context context, @NonNull Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setStyle(new Notification.BigTextStyle().bigText(intent.toString()));
        builder.setContentText(intent.toString());
        notificationManager.notify(0, builder.build());
    }
}
