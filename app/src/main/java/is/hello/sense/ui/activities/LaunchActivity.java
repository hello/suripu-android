package is.hello.sense.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;

import javax.inject.Inject;

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Constants;


public class LaunchActivity extends InjectionActivity {

    @Inject ApiSessionManager sessionManager;
    @Inject BuildValues buildValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!buildValues.isDebugBuild())
            Crashlytics.start(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        bounce();
    }


    private void showHomeActivity() {
        startActivity(new Intent(this, HomeActivity.class));
    }

    private void showOnboardingActivity() {
        startActivity(new Intent(this, OnboardingActivity.class));
    }

    private void bounce() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sessionManager.hasSession() && sharedPreferences.getBoolean(Constants.GLOBAL_PREF_ONBOARDING_COMPLETED, false)) {
            showHomeActivity();
        } else {
            showOnboardingActivity();
        }

        finish();
    }
}
