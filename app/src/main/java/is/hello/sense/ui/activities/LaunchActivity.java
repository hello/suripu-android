package is.hello.sense.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.ui.common.InjectionActivity;


public class LaunchActivity extends InjectionActivity {

    @Inject ApiSessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
