package is.hello.sense.ui.activities;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import java.io.File;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.ApiEndpoint;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.adapter.SettingsRecyclerAdapter;
import is.hello.sense.ui.adapter.SettingsRecyclerAdapter.DetailItem;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.InsetItemDecoration;
import is.hello.sense.ui.widget.WhatsNewLayout;
import is.hello.sense.util.Constants;
import is.hello.sense.util.SessionLogger;

public class DebugActivity extends InjectionActivity {
    @Inject ApiSessionManager sessionManager;
    @Inject PreferencesPresenter preferences;
    @Inject LocalUsageTracker localUsageTracker;
    @Inject ApiEndpoint apiEndpoint;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.static_recycler);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.static_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final int sectionPadding = getResources().getDimensionPixelSize(R.dimen.gap_outer);
        final InsetItemDecoration decoration = new InsetItemDecoration();
        recyclerView.addItemDecoration(decoration);


        final SettingsRecyclerAdapter adapter = new SettingsRecyclerAdapter(this);

        try {
            final Class<?> activityClass = Class.forName("is.hello.sense.debug.PiruPeaActivity");

            decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
            adapter.add(new DetailItem("Piru-Pea",
                                       () -> startActivity(new Intent(this, activityClass))));
        } catch (ClassNotFoundException ignored) {
            // Do nothing.
        }

        adapter.add(new DetailItem("View Log", this::viewLog));
        adapter.add(new DetailItem("Clear Log", this::clearLog));

        decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
        adapter.add(new DetailItem("Share Log", this::sendLog));

        adapter.add(new DetailItem("Show Room Check", this::showRoomCheck));
        adapter.add(new DetailItem("Show Onboarding Smart Alarm", this::showOnboardingSmartAlarm));
        decoration.addBottomInset(adapter.getItemCount(), sectionPadding);

        adapter.add(new DetailItem("Forget welcome dialogs", this::clearHandholdingSettings));

        try {
            final Class<?> activityClass = Class.forName("is.hello.sense.debug.WelcomeDialogsActivity");
            decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
            adapter.add(new DetailItem("View welcome dialogs",
                                       () -> startActivity(new Intent(this, activityClass))));
        } catch (ClassNotFoundException ignored) {
            // Do nothing.
        }
        adapter.add(new DetailItem("View What's New Card", this::viewWhatsNewCard));
        adapter.add(new DetailItem("Simulate Picasso Low Memory", this::simulatePicassoLowMemory));
        adapter.add(new DetailItem("Re-enable review prompt", this::reEnableReviewPrompt));

        decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
        adapter.add(new DetailItem("Reset app usage stats", this::resetAppUsage));

        adapter.add(new DetailItem("Log Out", this::logOut));

        recyclerView.setAdapter(adapter);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Sense " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
            actionBar.setSubtitle(apiEndpoint.getName());
        }
    }

    public void showRoomCheck() {
        Intent onboarding = new Intent(this, OnboardingActivity.class);
        onboarding.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_PILL);
        startActivity(onboarding);
    }

    public void showOnboardingSmartAlarm() {
        Intent onboarding = new Intent(this, OnboardingActivity.class);
        onboarding.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_SMART_ALARM);
        startActivity(onboarding);
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
        Toast.makeText(getApplicationContext(), "Forgot welcome dialogs", Toast.LENGTH_SHORT).show();
    }

    public void reEnableReviewPrompt() {
        preferences.edit()
                   .putBoolean(PreferencesPresenter.DISABLE_REVIEW_PROMPT, false)
                   .apply();
        localUsageTracker.reset(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
        Toast.makeText(getApplicationContext(), "Review prompt re-enabled", Toast.LENGTH_SHORT).show();
    }

    public void simulatePicassoLowMemory() {
        SenseApplication.getInstance().onTrimMemory(TRIM_MEMORY_MODERATE);
        Toast.makeText(getApplicationContext(), "Simulated", Toast.LENGTH_SHORT).show();
    }

    public void viewWhatsNewCard() {
        WhatsNewLayout.clearState(this);
        Toast.makeText(getApplicationContext(), "Forgot What's New card", Toast.LENGTH_SHORT).show();
    }

    public void resetAppUsage() {
        localUsageTracker.resetAsync();
        Toast.makeText(getApplicationContext(), "Usage Stats Reset", Toast.LENGTH_SHORT).show();
    }

    public void logOut() {
        sessionManager.logOut();
        finish();
    }


}
