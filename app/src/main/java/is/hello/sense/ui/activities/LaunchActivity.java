package is.hello.sense.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;

import javax.inject.Inject;

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;


public class LaunchActivity extends InjectionActivity {
    @Inject ApiSessionManager sessionManager;
    @Inject PreferencesPresenter preferences;
    @Inject BuildValues buildValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!buildValues.isDebugBuild()) {
            Crashlytics.start(this);
        }

        if (sessionManager.getSession() != null) {
            String accountId = sessionManager.getSession().getAccountId();
            Analytics.identify(accountId);
            Logger.info(Analytics.LOG_TAG, "Began session for " + accountId);
        }
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
        if (sessionManager.hasSession() && preferences.getBoolean(PreferencesPresenter.ONBOARDING_COMPLETED, false)) {
            showHomeActivity();
        } else {
            if (!sessionManager.hasSession()) {
                preferences
                        .edit()
                        .putBoolean(PreferencesPresenter.ONBOARDING_COMPLETED, false)
                        .putInt(PreferencesPresenter.LAST_ONBOARDING_CHECK_POINT, Constants.ONBOARDING_CHECKPOINT_NONE)
                        .apply();
            }

            showOnboardingActivity();
        }

        finish();
    }
}
