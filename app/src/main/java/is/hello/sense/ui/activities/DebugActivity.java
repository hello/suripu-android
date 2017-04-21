package is.hello.sense.ui.activities;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Field;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.ApiEndpoint;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.flows.expansions.ui.activities.ExpansionSettingsActivity;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.AccountPreferencesInteractor;
import is.hello.sense.interactors.PersistentPreferencesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.appcompat.InjectionActivity;
import is.hello.sense.ui.adapter.SettingsRecyclerAdapter;
import is.hello.sense.ui.adapter.SettingsRecyclerAdapter.DetailItem;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.InsetItemDecoration;
import is.hello.sense.ui.widget.WhatsNewLayout;
import is.hello.sense.util.Constants;
import is.hello.sense.util.InternalPrefManager;
import is.hello.sense.util.SessionLogger;

public class DebugActivity extends InjectionActivity {
    public static final String EXTRA_DEBUG_CHECKPOINT = "EXTRA_DEBUG_CHECKPOINT" + DebugActivity.class.getName();
    @Inject
    ApiSessionManager sessionManager;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    PersistentPreferencesInteractor persistentPreferences;
    @Inject
    LocalUsageTracker localUsageTracker;
    @Inject
    ApiEndpoint apiEndpoint;
    @Inject
    NightModeInteractor nightModeInteractor;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.static_recycler);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.static_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final int sectionPadding = getResources().getDimensionPixelSize(R.dimen.x3);
        final InsetItemDecoration decoration = new InsetItemDecoration();
        recyclerView.addItemDecoration(decoration);


        final SettingsRecyclerAdapter adapter = new SettingsRecyclerAdapter(this);

        try {
            final Class<?> activityClass = Class.forName("is.hello.sense.debug.PiruPeaActivity");

            decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
            adapter.add(new DetailItem("Piru-Pea",
                                       () -> startActivity(new Intent(this, activityClass))));
        } catch (final ClassNotFoundException ignored) {
            // Do nothing.
        }
        // todo remove when done testing
        adapter.add(new DetailItem("View Log", this::viewLog));
        adapter.add(new DetailItem("Clear Log", this::clearLog));

        adapter.add(new DetailItem("Clear UserLocation prefs", this::clearUserLocationPrefs));

        decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
        adapter.add(new DetailItem("Share Log", this::sendLog));

        adapter.add(new DetailItem("Show Room Check", this::showRoomCheck));
        adapter.add(new DetailItem("Show Onboarding Smart Alarm", this::showOnboardingSmartAlarm));
        adapter.add(new DetailItem("Show Update Pill", this::showUpdatePill));
        adapter.add(new DetailItem("Show Sense OTA Update", this::showSenseOTA));
        adapter.add(new DetailItem("Show New Sense Update", this::showNewSenseUpdate));
        adapter.add(new DetailItem("Show Sense Voice", this::showSenseVoice));
        adapter.add(new DetailItem("Show Expansions", this::showExpansion));
        decoration.addBottomInset(adapter.getItemCount(), sectionPadding);

        adapter.add(new DetailItem("Forget welcome dialogs", this::clearHandholdingSettings));
        adapter.add(new DetailItem("Forget account tutorials", this::clearTutorials));
        adapter.add(new DetailItem("Forget persistent preferences", this::clearPersistentPreferences));

        try {
            final Class<?> activityClass = Class.forName("is.hello.sense.debug.WelcomeDialogsActivity");
            decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
            adapter.add(new DetailItem("View welcome dialogs",
                                       () -> startActivity(new Intent(this, activityClass))));
        } catch (final ClassNotFoundException ignored) {
            // Do nothing.
        }
        adapter.add(new DetailItem("View What's New Card", this::viewWhatsNewCard));
        adapter.add(new DetailItem("Simulate Picasso Low Memory", this::simulatePicassoLowMemory));
        adapter.add(new DetailItem("Re-enable review prompt", this::reEnableReviewPrompt));
        adapter.add(new DetailItem("Re-enable Amazon review prompt", this::reEnableAmazonReviewPrompt));
        decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
        adapter.add(new DetailItem("Reset app usage stats", this::resetAppUsage));
        adapter.add(new DetailItem("View Room Conditions Welcome Card", this::viewRoomConditionsWelcomeCard));
        decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
        adapter.add(new DetailItem("Toggle Night Mode", this::toggleNightMode));
        adapter.add(new DetailItem("Print current session information", this::printCurrentSessionInformation));
        decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
        adapter.add(new DetailItem("Change current session account id", () -> this.update(updateSessionAccountId)));
        adapter.add(new DetailItem("Print internal pref account id", this::printCurrentInternalPrefAccountId));
        adapter.add(new DetailItem("Change current  internal pref account id", () -> this.update(updateInternalPrefAccountId)));
        adapter.add(new DetailItem("Log Out", this::logOut));

        recyclerView.setAdapter(adapter);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Sense " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
            actionBar.setSubtitle(apiEndpoint.getName());
        }
    }

    public void showRoomCheck() {
        final Intent onboarding = new Intent(this, OnboardingActivity.class);
        onboarding.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_PILL);
        startActivity(onboarding);
    }

    public void showOnboardingSmartAlarm() {
        final Intent onboarding = new Intent(this, OnboardingActivity.class);
        onboarding.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_SMART_ALARM);
        startActivity(onboarding);
    }

    public void showUpdatePill() {
        final Intent pillUpdate = new Intent(this, PillUpdateActivity.class);
        startActivity(pillUpdate);
    }

    private void showSenseOTA() {
        final Intent onboarding = new Intent(this, OnboardingActivity.class);
        onboarding.putExtra(DebugActivity.EXTRA_DEBUG_CHECKPOINT, Constants.DEBUG_CHECKPOINT_SENSE_UPDATE);
        startActivity(onboarding);
    }

    private void showSenseVoice() {
        final Intent onboarding = new Intent(this, OnboardingActivity.class);
        onboarding.putExtra(DebugActivity.EXTRA_DEBUG_CHECKPOINT, Constants.DEBUG_CHECKPOINT_SENSE_VOICE);
        startActivity(onboarding);
    }

    private void showNewSenseUpdate() {
        final Intent senseUpdate = new Intent(this, SenseUpgradeActivity.class);
        startActivity(senseUpdate);
    }

    public void viewLog() {
        startActivity(new Intent(this, SessionLogViewerActivity.class));
    }

    public void clearLog() {
        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(SessionLogger.clearLog(),
                         ignored -> LoadingDialogFragment.close(getFragmentManager()),
                         e -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             ErrorDialogFragment.presentError(this, e);
                         });
    }

    public void clearUserLocationPrefs() {
        this.persistentPreferences.clearUserLocation();
        Toast.makeText(getApplicationContext(), "Forgot UserLocation for account: " + InternalPrefManager.getAccountId(this), Toast.LENGTH_SHORT).show();
    }

    public void sendLog() {
        bindAndSubscribe(SessionLogger.flush(), ignored -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(SessionLogger.getLogFilePath(this))));
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, "Share Log"));
        }, Functions.LOG_ERROR);
    }

    public void clearHandholdingSettings() {
        WelcomeDialogFragment.clearShownStates(this);
        AccountPreferencesInteractor.newInstance(this).reset();
        Toast.makeText(getApplicationContext(), "Forgot welcome dialogs", Toast.LENGTH_SHORT).show();
    }

    public void clearTutorials() {
        Tutorial.clearTutorials(this);
        Toast.makeText(getApplicationContext(), "Forgot tutorials for account: " + InternalPrefManager.getAccountId(this), Toast.LENGTH_SHORT).show();
    }

    public void reEnableReviewPrompt() {
        preferences.edit()
                   .putBoolean(PreferencesInteractor.DISABLE_REVIEW_PROMPT, false)
                   .apply();
        localUsageTracker.reset(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
        Toast.makeText(getApplicationContext(), "Review prompt re-enabled", Toast.LENGTH_SHORT).show();
    }

    public void reEnableAmazonReviewPrompt() {
        preferences.edit()
                   .putBoolean(PreferencesInteractor.HAS_REVIEWED_ON_AMAZON, false)
                   .apply();
        localUsageTracker.reset(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
        Toast.makeText(getApplicationContext(), "Amazon Review prompt re-enabled", Toast.LENGTH_SHORT).show();
    }

    public void clearPersistentPreferences() {
        persistentPreferences.clear();
        Toast.makeText(getApplicationContext(), "Forgot persistent preferences", Toast.LENGTH_SHORT).show();
    }


    public void simulatePicassoLowMemory() {
        SenseApplication.getInstance().onTrimMemory(TRIM_MEMORY_MODERATE);
        Toast.makeText(getApplicationContext(), "Simulated", Toast.LENGTH_SHORT).show();
    }

    public void viewWhatsNewCard() {
        // WhatsNewLayout.clearState(this); todo add back when we support this.
        WhatsNewLayout.forceShow(this);
        Toast.makeText(getApplicationContext(), "Forgot What's New card", Toast.LENGTH_SHORT).show();
    }

    public void resetAppUsage() {
        localUsageTracker.resetAsync();
        Toast.makeText(getApplicationContext(), "Usage Stats Reset", Toast.LENGTH_SHORT).show();
    }

    public void viewRoomConditionsWelcomeCard() {
        preferences.edit().putInt(PreferencesInteractor.ROOM_CONDITIONS_WELCOME_CARD_TIMES_SHOWN, 1).apply();
        Toast.makeText(getApplicationContext(), "Forgot Room Conditions Welcome Card", Toast.LENGTH_SHORT).show();
    }

    public void showExpansion() {
        startActivity(new Intent(this, ExpansionSettingsActivity.class));
    }

    public void toggleNightMode() {
        final int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                nightModeInteractor.setMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                nightModeInteractor.setMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                // We don't know what mode we're in, assume notnight
                nightModeInteractor.setMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
        recreate();
    }

    public void logOut() {
        sessionManager.logOut();
        finish();
    }

    public void printCurrentSessionInformation() {
        final OAuthSession session = sessionManager.getSession();
        if (session == null) {
            Log.e(getClass().getSimpleName(), "current session is null");
            Toast.makeText(this, "current session is null", Toast.LENGTH_LONG).show();
            return;
        }
        Log.e(getClass().getSimpleName(), "current session is : " + session.toString());
        Toast.makeText(this, "current session is : " + session.toString(), Toast.LENGTH_LONG).show();
    }


    public void update(@Nullable final Update update) {
        if (update == null) {
            return;
        }
        //Layout params
        final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // EditText
        final EditText editText = new EditText(this);
        editText.setLayoutParams(layoutParams);

        // Button
        final Button button = new Button(this);
        button.setText("Save");
        button.setLayoutParams(layoutParams);

        // LinearLayout
        final LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setLayoutParams(layoutParams);
        view.addView(editText);
        view.addView(button);

        // Dialog
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .show();
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        final Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);


        button.setOnClickListener(v -> {
            update.update(editText.getText().toString());
            dialog.dismiss();
        });
    }

    private Update updateSessionAccountId = new Update() {
        @Override
        public void update(@NonNull final String text) {
            final OAuthSession session = sessionManager.getSession();
            if (session == null) {
                Log.e(getClass().getSimpleName(), "current session is null. Stopping");
                Toast.makeText(DebugActivity.this, "current session is null. Stopping", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                final Field accountId = session.getClass().getDeclaredField("accountId");
                accountId.setAccessible(true);
                accountId.set(session, text);
                sessionManager.setSession(session);
                printCurrentSessionInformation();
            } catch (final Exception e) {
                Log.e(getClass().getSimpleName(), "Failed to update session account id. Stopping");
                Toast.makeText(DebugActivity.this, "Failed to update session account id. Stopping", Toast.LENGTH_LONG).show();
            }
        }
    };


    public void printCurrentInternalPrefAccountId() {
        final String id = InternalPrefManager.getAccountId(this);
        Log.e(getClass().getSimpleName(), "current internal pref account id : " + id);
        Toast.makeText(this, "current internal pref account id : " + id, Toast.LENGTH_LONG).show();
    }


    private Update updateInternalPrefAccountId = text -> {
        InternalPrefManager.setAccountId(DebugActivity.this, text);
        printCurrentInternalPrefAccountId();
    };

    private interface Update {
        void update(@NonNull final String text);
    }

}
