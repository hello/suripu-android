package is.hello.sense.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;

import javax.inject.Inject;

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.util.BuildValues;


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
        if (sessionManager.hasSession()) {
            showHomeActivity();
        } else {
            showOnboardingActivity();
        }

        finish();
    }
}
