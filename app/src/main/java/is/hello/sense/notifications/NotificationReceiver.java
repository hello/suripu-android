package is.hello.sense.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import rx.android.schedulers.AndroidSchedulers;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String EXTRA_TARGET = "target";
    public static final String EXTRA_FROM = "from";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_DETAILS = "details";
    public static final String EXTRA_SILENT_MODE = "silent_mode";
    public static final String EXTRA_COLLAPSE_KEY = "collapse_key";

    @Inject Markdown markdown;
    private int notificationId = 0;

    public NotificationReceiver() {
        SenseApplication.getInstance().inject(this);
    }

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
                Logger.warn(NotificationReceiver.class.getSimpleName(), "Unrecognized message type: " + messageType);
                break;
        }
    }

    public void onMessageSendError(@NonNull Context context, @NonNull Intent intent) {
        Logger.error(NotificationReceiver.class.getSimpleName(), "Message send error for: " + intent);
    }

    public void onMessageDeleted(@NonNull Context context, @NonNull Intent intent) {
        Logger.info(NotificationReceiver.class.getSimpleName(), "Notification was deleted on server: " + intent);
    }

    public void onMessageReceived(@NonNull Context context, @NonNull Intent intent) {
        Logger.info(NotificationReceiver.class.getSimpleName(), "Received message: " + intent.getExtras());
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        if (!TextUtils.isEmpty(message)) {
            markdown.render(message)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(richMessage -> {
                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        Notification.Builder builder = new Notification.Builder(context);
                        builder.setSmallIcon(R.drawable.ic_launcher);
                        builder.setContentTitle(context.getString(R.string.app_name));
                        builder.setContentText(richMessage);
                        builder.setStyle(new Notification.BigTextStyle().bigText(richMessage));
                        if (!intent.getBooleanExtra(EXTRA_SILENT_MODE, false)) {
                            builder.setDefaults(Notification.DEFAULT_ALL);
                        }

                        Intent activityIntent = new Intent(context, HomeActivity.class);
                        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activityIntent.putExtra(HomeActivity.EXTRA_NOTIFICATION_PAYLOAD, intent.getExtras());
                        builder.setContentIntent(PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_ONE_SHOT));
                        builder.setAutoCancel(true);

                        notificationManager.notify(notificationId++, builder.build());
                    });
        }
    }
}
