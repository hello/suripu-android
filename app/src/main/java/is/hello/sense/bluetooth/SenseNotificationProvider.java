package is.hello.sense.bluetooth;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;

import is.hello.commonsense.service.SenseService;
import is.hello.sense.R;
import is.hello.sense.ui.activities.LaunchActivity;
import is.hello.sense.ui.widget.util.Drawables;

public class SenseNotificationProvider implements SenseService.ForegroundNotificationProvider {
    private final Context context;

    public SenseNotificationProvider(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public int getId() {
        // Positive notifications are reserved for NotificationReceiver.
        return -1;
    }

    @NonNull
    @Override
    public Notification getNotification() {
        // Same effect as launching the app from the home screen. The user
        // will land wherever they were in the app before they left it.
        final Intent launch = new Intent(context, LaunchActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setLocalOnly(true)
                .setContentTitle(context.getString(R.string.device_sense))
                .setContentText(context.getString(R.string.info_connected))
                .setContentIntent(PendingIntent.getActivity(context, 0, launch,
                                                            PendingIntent.FLAG_UPDATE_CURRENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.ic_check_white);
            builder.setLargeIcon(generateLargeIcon());
            builder.setColor(ContextCompat.getColor(context, R.color.sensor_ideal));
        } else {
            builder.setSmallIcon(R.drawable.ic_stat_notify_msg);
        }

        return builder.build();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Bitmap generateLargeIcon() {
        final Resources resources = context.getResources();
        final int width = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
        final int height = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

        final Bitmap largeIcon = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(largeIcon);

        final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.WHITE);
        canvas.drawOval(0f, 0f, width, height, fillPaint);

        @SuppressWarnings("ConstantConditions")
        final Drawable badgeIcon = ResourcesCompat.getDrawable(resources,
                                                               R.drawable.ic_stat_notify_msg,
                                                               null)
                                                  .mutate();
        Drawables.setTintColor(badgeIcon, ContextCompat.getColor(context, R.color.sensor_ideal));

        final int badgeWidth = badgeIcon.getIntrinsicWidth();
        final int badgeHeight = badgeIcon.getIntrinsicHeight();
        badgeIcon.setBounds(width / 2 - badgeWidth / 2, height / 2 - badgeHeight / 2,
                            width / 2 + badgeWidth / 2, height / 2 + badgeHeight / 2);
        badgeIcon.draw(canvas);

        return largeIcon;
    }
}
