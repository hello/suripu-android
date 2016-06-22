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
import com.google.android.gms.iid.InstanceID;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
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

    private static @NonNull SharedPreferences getNotificationPreferences(@NonNull final Context context) {
        return context.getSharedPreferences(Constants.NOTIFICATION_PREFS, 0);
    }

    private static int getPackageVersionCode(@NonNull final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name.", e);
        }
    }

    private static String getPackageVersionName(@NonNull final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name.", e);
        }
    }

    public static boolean shouldRegister(@NonNull final Context context) {
        final SharedPreferences preferences = getNotificationPreferences(context);
        final int versionCode = getPackageVersionCode(context);
        return (preferences.getInt(Constants.NOTIFICATION_PREF_APP_VERSION, -1) != versionCode ||
                preferences.getString(Constants.NOTIFICATION_PREF_REGISTRATION_ID, null) == null);
    }

    public static void resetAppVersion(@NonNull final Context context){
        getNotificationPreferences(context).edit()
                .putInt(Constants.NOTIFICATION_PREF_APP_VERSION, 0)
                .apply();

    }

    public NotificationRegistration(@NonNull final Activity activity) {
        this.activity = activity;
        SenseApplication.getInstance().inject(this);
    }

    public void register() {
        if (!checkPlayServices())
            return;

        final String preexistingRegistrationId = retrieveRegistrationId();
        if (preexistingRegistrationId != null) {
            registerWithBackend(preexistingRegistrationId);
        } else {
            registerWithGCM().subscribe(this::registerWithBackend,
                    e -> Logger.error(NotificationRegistration.class.getSimpleName(), "Could not register with GCM.", e));
        }
    }

    private boolean checkPlayServices() {
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
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

    private void saveRegistrationId(@NonNull final String registrationId) {
        final SharedPreferences preferences = getNotificationPreferences(activity);
        preferences.edit()
                .putString(Constants.NOTIFICATION_PREF_REGISTRATION_ID, registrationId)
                .apply();
    }

    private void saveAppVersionCode() {
        SharedPreferences preferences = getNotificationPreferences(activity);
        final int versionCode = getPackageVersionCode(activity);
        preferences.edit()
                .putInt(Constants.NOTIFICATION_PREF_APP_VERSION, versionCode)
                .apply();
    }

    private Observable<String> registerWithGCM() {
        return Observable.create((Observable.OnSubscribe<String>) subscriber -> {
            Logger.info(NotificationRegistration.class.getSimpleName(), "Registering for notifications.");

            try {
                final String registrationId = InstanceID.getInstance(activity)
                                                  .getToken(BuildConfig.GCM_AUTH_TOKEN_IDS,
                                                            GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                saveRegistrationId(registrationId);
                Logger.info(NotificationRegistration.class.getSimpleName(), "Registered with GCM: " + registrationId);

                subscriber.onNext(registrationId);
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }).subscribeOn(Schedulers.newThread());
    }

    private void registerWithBackend(@NonNull final String registrationId) {
        final PushRegistration registration = new PushRegistration(getPackageVersionName(activity), registrationId);
        apiService.registerForNotifications(registration)
                  .subscribe(ignored -> {
                              Logger.info(NotificationRegistration.class.getSimpleName(), "Registered with backend.");
                              saveAppVersionCode();
                          }, e -> Logger.error(NotificationRegistration.class.getSimpleName(), "Could not register with API.", e));
    }
}
