package is.hello.sense.ui.activities;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;

import com.segment.analytics.Properties;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;


public class LaunchActivity extends InjectionActivity {
    @Inject ApiSessionManager sessionManager;
    @Inject PreferencesPresenter preferences;
    @Inject LocalUsageTracker localUsageTracker;

    /**
     * Included to force {@link ApiService} to be initialized before
     * {@link TimelineFragment}, giving its asynchronously initialized
     * file system cache more time to complete set up.
     */
    @SuppressWarnings("unused")
    @Inject ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.APP_LAUNCHED);
            apiService.getAccount().subscribe(account -> {
                final com.segment.analytics.Properties properties = new Properties();
                properties.put(Analytics.Global.GLOBAL_PROP_ACCOUNT_EMAIL, account.getEmail());
                properties.put(Analytics.Global.GLOBAL_PROP_ACCOUNT_NAME, account.getName());
                Analytics.trackEvent(Analytics.Global.APP_LAUNCHED, properties);
            }, e -> {
                Logger.error(getClass().getSimpleName(), "Could not load user info", e);
                Analytics.trackEvent(Analytics.Global.APP_LAUNCHED, null);
            });

        }

        if (sessionManager.getSession() != null) {
            String accountId = sessionManager.getSession().getAccountId();
            Analytics.trackUserIdentifier(this, accountId, true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            unsupported();
        } else {
            bounce();
        }
    }


    private void showHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (AlarmClock.ACTION_SHOW_ALARMS.equals(getIntent().getAction())) {
                intent.setAction(AlarmClock.ACTION_SHOW_ALARMS);
            }
        }
        startActivity(intent);
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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void unsupported() {
        final SenseAlertDialog dialog = new SenseAlertDialog(this);
        dialog.setTitle(R.string.dialog_title_unsupported_os);
        dialog.setMessage(R.string.dialog_message_unsupported_os);
        dialog.setPositiveButton(R.string.action_exit, (ignored, which) -> finish());
        dialog.setOnCancelListener(ignored -> finish());
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}