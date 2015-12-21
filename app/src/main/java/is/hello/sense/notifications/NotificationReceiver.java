package is.hello.sense.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import javax.inject.Inject;

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.util.Logger;
import is.hello.sense.util.markup.MarkupProcessor;
import rx.Observable;
import rx.schedulers.Schedulers;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String EXTRA_TARGET = "target";
    public static final String EXTRA_FROM = "from";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_DETAILS = "details";
    public static final String EXTRA_SILENT_MODE = "silent_mode";
    public static final String EXTRA_COLLAPSE_KEY = "collapse_key";

    @Inject MarkupProcessor markupProcessor;

    private int notificationId = 0;

    public NotificationReceiver() {
        // Robolectric implements the application lifecycle wrong and
        // instantiates all receivers _before_ #onCreate is called.
        if (!SenseApplication.isRunningInRobolectric()) {
            SenseApplication.getInstance().inject(this);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        final String messageType = gcm.getMessageType(intent);
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
                Logger.warn(NotificationReceiver.class.getSimpleName(),
                            "Unrecognized message type: " + messageType);
                break;
        }
    }

    public void onMessageSendError(@NonNull Context context, @NonNull Intent intent) {
        Logger.error(NotificationReceiver.class.getSimpleName(),
                     "Message send error for: " + intent);
    }

    public void onMessageDeleted(@NonNull Context context, @NonNull Intent intent) {
        Logger.info(NotificationReceiver.class.getSimpleName(),
                    "Notification was deleted on server: " + intent);
    }

    public void onMessageReceived(@NonNull Context context, @NonNull Intent intent) {
        Logger.info(NotificationReceiver.class.getSimpleName(),
                    "Received message: " + intent.getExtras());
        final String rawMessage = intent.getStringExtra(EXTRA_MESSAGE);
        final Observable.OnSubscribe<CharSequence> onSubscribeRender = subscriber -> {
            try {
                final CharSequence rendered = markupProcessor.render(rawMessage);
                subscriber.onNext(rendered);
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        };
        final Observable<CharSequence> renderMessage =
                Observable.create(onSubscribeRender)
                          .subscribeOn(Schedulers.computation())
                          .observeOn(Rx.mainThreadScheduler());
        renderMessage.subscribe(message -> {
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setSmallIcon(R.drawable.ic_stat_notify_msg);
            builder.setColor(context.getResources().getColor(R.color.light_accent));
            builder.setContentTitle(context.getString(R.string.app_name));
            builder.setContentText(message);
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
            if (!intent.getBooleanExtra(EXTRA_SILENT_MODE, false)) {
                builder.setDefaults(Notification.DEFAULT_ALL);
            }

            final Intent activityIntent = new Intent(context, HomeActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activityIntent.putExtra(HomeActivity.EXTRA_NOTIFICATION_PAYLOAD, intent.getExtras());
            builder.setContentIntent(PendingIntent.getActivity(context, 0,
                                                               activityIntent,
                                                               PendingIntent.FLAG_ONE_SHOT));
            builder.setAutoCancel(true);

            final NotificationManagerCompat manager =
                    NotificationManagerCompat.from(context);
            manager.notify(notificationId++, builder.build());
        }, Functions.LOG_ERROR);
    }
}
