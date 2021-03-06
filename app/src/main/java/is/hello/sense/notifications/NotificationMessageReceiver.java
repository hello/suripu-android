package is.hello.sense.notifications;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.LaunchActivity;

/**
 * Handle display of notifications received
 */
public class NotificationMessageReceiver extends BroadcastReceiver {
    private static final String ACTION_FILTER = "is.hello.sense.notifications.MESSAGE_RECEIVED";
    private static final String EXTRA_NOTIFICATION_PAYLOAD = NotificationMessageReceiver.class.getName() + ".EXTRA_NOTIFICATION_PAYLOAD";
    private final boolean abortBroadcast;

    static Intent getIntent(@NonNull final Bundle bundle) {
        final Intent intent = new Intent(ACTION_FILTER);
        intent.putExtra(EXTRA_NOTIFICATION_PAYLOAD, bundle);
        return intent;
    }

    public static IntentFilter getMainPriorityFilter() {
        final IntentFilter filter = new IntentFilter(ACTION_FILTER);
        filter.setPriority(2);
        return filter;
    }

    public static IntentFilter getBackgroundPriorityFilter() {
        final IntentFilter filter = new IntentFilter(ACTION_FILTER);
        filter.setPriority(1);
        return filter;
    }

    public static void cancelShownMessages(@NonNull final Context context) {
        NotificationManagerCompat.from(context)
                                 .cancelAll();
    }

    /**
     * @param abortBroadcast if true will not handle message received and prevent
     *                       other receivers from handling message.
     */
    public NotificationMessageReceiver(final boolean abortBroadcast) {
        this.abortBroadcast = abortBroadcast;
    }

    public NotificationMessageReceiver() {
        this(false);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

        if(ACTION_FILTER.equals(intent.getAction())
                && intent.hasExtra(EXTRA_NOTIFICATION_PAYLOAD)) {

            if(abortBroadcast) {
                abortBroadcast();
                return;
            }

            onMessageReceived(context, intent);
        }
    }

    public void onMessageReceived(@NonNull final Context context, @NonNull final Intent intent) {
        final Bundle bundle = intent.getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD);
        final CharSequence titleText = bundle.getString(is.hello.sense.notifications.Notification.REMOTE_TITLE);
        final CharSequence bodyText = bundle.getString(is.hello.sense.notifications.Notification.REMOTE_BODY);
        @is.hello.sense.notifications.Notification.Type
        final String type = Notification.typeFromBundle(bundle);
        final Intent activityIntent = new Intent(context, LaunchActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.putExtra(HomeActivity.EXTRA_NOTIFICATION_PAYLOAD, bundle);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.icon_sense_24_white);
        builder.setColor(ContextCompat.getColor(context, R.color.notification_text));
        builder.setContentTitle(titleText);
        builder.setContentText(bodyText);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bodyText));
        builder.setContentIntent(PendingIntent.getActivity(context, 0,
                                                           activityIntent,
                                                           PendingIntent.FLAG_ONE_SHOT));
        builder.setAutoCancel(true);

        final NotificationManagerCompat manager =
                NotificationManagerCompat.from(context);
        manager.notify(type.hashCode(), builder.build());
    }
}
