package is.hello.sense.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.PushRegistration;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

public final class NotificationRegistration {
    private static final String PREF_APP_VERSION = "app_version";
    private static final String PREF_REGISTRATION_ID = "registration_id";

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
        return (preferences.getInt(PREF_APP_VERSION, -1) != versionCode ||
                preferences.getString(PREF_REGISTRATION_ID, null) == null);
    }

    public NotificationRegistration(@NonNull Activity activity) {
        this.activity = activity;
        SenseApplication.getInstance().inject(this);
    }

    public void register() {
        if (!checkPlayServices())
            return;

        registerInBackground();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity, 0x00).show();
            } else {
                Logger.warn(NotificationRegistration.class.getSimpleName(), "Google play services fatal error: " + resultCode);
            }
            return false;
        }
        return true;
    }

    private void saveRegistrationId(@NonNull String registrationId) {
        SharedPreferences preferences = getNotificationPreferences(activity);
        int versionCode = getPackageVersionCode(activity);
        preferences.edit()
                .putString(PREF_REGISTRATION_ID, registrationId)
                .putInt(PREF_APP_VERSION, versionCode)
                .apply();
    }

    private void registerInBackground() {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(activity);
        String senderId = activity.getString(R.string.build_gcm_id);

        Logger.info(NotificationRegistration.class.getSimpleName(), "Registering for notifications.");

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String registrationId = gcm.register(senderId);
                    saveRegistrationId(registrationId);
                    return registrationId;
                } catch (IOException e) {
                    Logger.error(NotificationRegistration.class.getSimpleName(), "Could not register for GCM.", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String registrationId) {
                if (!TextUtils.isEmpty(registrationId)) {
                    Logger.info(NotificationRegistration.class.getSimpleName(), "Registered with GCM: " + registrationId);
                    registerWithBackend(registrationId);
                }
            }
        }.execute();
    }

    private void registerWithBackend(@NonNull String registrationId) {
        PushRegistration registration = new PushRegistration(getPackageVersionName(activity), registrationId);
        apiService.registerForNotifications(registration)
                  .subscribe(ignored -> Logger.info(NotificationRegistration.class.getSimpleName(), "Registered with backend for notifications."),
                             error -> Logger.error(NotificationRegistration.class.getSimpleName(), "Could not register with backend for notifications.", error));
    }
}
