package is.hello.sense.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import javax.inject.Inject;

import is.hello.sense.SenseApplication;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.PushRegistration;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.schedulers.Schedulers;

public final class NotificationRegistration {
    private final Activity activity;
    @Inject ApiService apiService;

    private static @NonNull SharedPreferences getNotificationPreferences(@NonNull Context context) {
        return context.getSharedPreferences(Constants.NOTIFICATION_PREFS, 0);
    }

    private static int getPackageVersionCode(@NonNull Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name.", e);
        }
    }

    private static String getPackageVersionName(@NonNull Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name.", e);
        }
    }

    public static boolean shouldRegister(@NonNull Context context) {
        SharedPreferences preferences = getNotificationPreferences(context);
        int versionCode = getPackageVersionCode(context);
        return (preferences.getInt(Constants.NOTIFICATION_PREF_APP_VERSION, -1) != versionCode ||
                preferences.getString(Constants.NOTIFICATION_PREF_REGISTRATION_ID, null) == null);
    }

    public static void resetAppVersion(@NonNull Context context){
        getNotificationPreferences(context).edit()
                .putInt(Constants.NOTIFICATION_PREF_APP_VERSION, 0)
                .apply();

    }

    public NotificationRegistration(@NonNull Activity activity) {
        this.activity = activity;
        SenseApplication.getInstance().inject(this);
    }

    public void register() {
        if (!checkPlayServices())
            return;

        String preexistingRegistrationId = retrieveRegistrationId();
        if (preexistingRegistrationId != null) {
            registerWithBackend(preexistingRegistrationId);
        } else {
            registerWithGCM().subscribe(this::registerWithBackend,
                    e -> Logger.error(NotificationRegistration.class.getSimpleName(), "Could not register with GCM.", e));
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity, 0).show();
            } else {
                Logger.warn(NotificationRegistration.class.getSimpleName(), "Google play services fatal error: " + resultCode);
            }
            return false;
        }
        return true;
    }

    private @Nullable String retrieveRegistrationId() {
        return getNotificationPreferences(activity).getString(Constants.NOTIFICATION_PREF_REGISTRATION_ID, null);
    }

    private void saveRegistrationId(@NonNull String registrationId) {
        SharedPreferences preferences = getNotificationPreferences(activity);
        preferences.edit()
                .putString(Constants.NOTIFICATION_PREF_REGISTRATION_ID, registrationId)
                .apply();
    }

    private void saveAppVersionCode() {
        SharedPreferences preferences = getNotificationPreferences(activity);
        int versionCode = getPackageVersionCode(activity);
        preferences.edit()
                .putInt(Constants.NOTIFICATION_PREF_APP_VERSION, versionCode)
                .apply();
    }

    private Observable<String> registerWithGCM() {
        return Observable.create((Observable.OnSubscribe<String>) subscriber -> {
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(activity);
            Logger.info(NotificationRegistration.class.getSimpleName(), "Registering for notifications.");

            try {
                String registrationId = gcm.register("88512303154", "222711285636");
                saveRegistrationId(registrationId);
                Logger.info(NotificationRegistration.class.getSimpleName(), "Registered with GCM: " + registrationId);

                subscriber.onNext(registrationId);
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }).subscribeOn(Schedulers.newThread());
    }

    private void registerWithBackend(@NonNull String registrationId) {
        PushRegistration registration = new PushRegistration(getPackageVersionName(activity), registrationId);
        apiService.registerForNotifications(registration)
                  .subscribe(ignored -> {
                              Logger.info(NotificationRegistration.class.getSimpleName(), "Registered with backend.");
                              saveAppVersionCode();
                          }, e -> Logger.error(NotificationRegistration.class.getSimpleName(), "Could not register with API.", e));
    }
}
