package is.hello.sense.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import is.hello.sense.SenseApplication;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.PushRegistration;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

public final class NotificationRegistration {
    private final Context context;
    @Inject ApiService apiService;

    //todo use for notification enable prefs else remove
    private static @NonNull SharedPreferences getNotificationPreferences(@NonNull final Context context) {
        return context.getSharedPreferences(Constants.NOTIFICATION_PREFS, Context.MODE_PRIVATE);
    }

    public NotificationRegistration(@NonNull final Context context) {
        this.context = context;
        SenseApplication.getInstance().inject(this);
    }

    private boolean checkPlayServices() {
        /* todo determine if worth checking for update if needed firebase doesn't include utils
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity, 0).show();
            } else {
                Logger.warn(NotificationRegistration.class.getSimpleName(), "Google play services fatal error: " + resultCode);
            }
            return false;
        }*/
        return true;
    }

    void register(@NonNull final String registrationId) {
        final PushRegistration registration = new PushRegistration(registrationId);
        apiService.registerForNotifications(registration)
                  .subscribe(ignored -> {
                              Logger.info(NotificationRegistration.class.getSimpleName(), "Registered with backend.");
                          }, e -> Logger.error(NotificationRegistration.class.getSimpleName(), "Could not register with API.", e));
    }
}
