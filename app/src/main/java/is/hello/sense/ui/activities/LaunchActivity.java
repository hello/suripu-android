package is.hello.sense.ui.activities;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.fragments.TimelineFragment;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.notifications.NotificationPressedInterceptorCounter;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.appcompat.InjectionActivity;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.InternalPrefManager;
import is.hello.sense.util.Logger;


public class LaunchActivity extends InjectionActivity {
    @Inject
    ApiSessionManager sessionManager;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    LocalUsageTracker localUsageTracker;
    @Inject
    NotificationPressedInterceptorCounter notificationPressedInterceptorCounter;
    @Inject
    NightModeInteractor nightModeInteractor;

    /**
     * Included to force {@link ApiService} to be initialized before
     * {@link TimelineFragment}, giving its asynchronously initialized
     * file system cache more time to complete set up.
     */
    @SuppressWarnings("unused")
    @Inject
    ApiService apiService;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.APP_LAUNCHED);
            if (sessionManager.hasSession()) {
                apiService.getAccount(false).subscribe(account -> {
                    InternalPrefManager.setAccountId(this, account.getId());
                    Analytics.backFillUserInfo(account.getFullName(), account.getEmail());
                    Analytics.trackEvent(Analytics.Global.APP_LAUNCHED, null);
                }, e -> {
                    Logger.error(getClass().getSimpleName(), "Could not load user info", e);
                    Analytics.trackEvent(Analytics.Global.APP_LAUNCHED, null);
                });
            } else {
                Analytics.trackEvent(Analytics.Global.APP_LAUNCHED, null);
            }
        }

        if (sessionManager.getSession() != null) {
            final String accountId = sessionManager.getSession().getAccountId();
            InternalPrefManager.setAccountId(this, accountId);
            Analytics.trackUserIdentifier(accountId, true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            unsupported();
        } else {
            bounce();
        }
    }


    private void showHomeActivity() {
        final Intent intent = new Intent(this, HomeActivity.class);
        if (AlarmClock.ACTION_SHOW_ALARMS.equals(getIntent().getAction())) {
            intent.setAction(AlarmClock.ACTION_SHOW_ALARMS);
        } else {
            addNotificationFlagsIfNeeded(intent, true);
        }
        startActivity(intent);
    }

    private void showOnboardingActivity() {
        final Intent intent = new Intent(this, OnboardingActivity.class);
        addNotificationFlagsIfNeeded(intent, false);
        startActivity(intent);
    }

    private void addNotificationFlagsIfNeeded(@NonNull final Intent intent,
                                              final boolean includePayload) {
        if (getIntent().hasExtra(HomeActivity.EXTRA_NOTIFICATION_PAYLOAD)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (includePayload) {
                intent.putExtra(HomeActivity.EXTRA_NOTIFICATION_PAYLOAD,
                                getIntent().getBundleExtra(HomeActivity.EXTRA_NOTIFICATION_PAYLOAD));
            }
        }
    }

    private void bounce() {
        if (sessionManager.hasSession() && preferences.getBoolean(PreferencesInteractor.ONBOARDING_COMPLETED, false)) {
            nightModeInteractor.updateToMatchPrefAndSession();
            if (!shouldFinish()) {
                showHomeActivity();
            }
        } else {
            if (!sessionManager.hasSession()) {
                preferences
                        .edit()
                        .putBoolean(PreferencesInteractor.ONBOARDING_COMPLETED, false)
                        .putInt(PreferencesInteractor.LAST_ONBOARDING_CHECK_POINT,
                                Constants.ONBOARDING_CHECKPOINT_NONE)
                        .apply();
            }

            showOnboardingActivity();
        }

        finish();
    }

    private boolean shouldFinish() {
        return notificationPressedInterceptorCounter != null
                && notificationPressedInterceptorCounter.hasActiveInterceptors();
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