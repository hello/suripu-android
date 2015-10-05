package is.hello.sense.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.util.Constants;
import is.hello.sense.util.SessionLogger;

public class DebugActivity extends InjectionActivity implements AdapterView.OnItemClickListener {
    @Inject ApiSessionManager sessionManager;
    @Inject PreferencesPresenter preferences;
    @Inject LocalUsageTracker localUsageTracker;

    private StaticItemAdapter debugActionItems;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view_static);

        this.debugActionItems = new StaticItemAdapter(this);
        populateDebugActionItems();


        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(debugActionItems);
        listView.setOnItemClickListener(this);
    }


    private void populateDebugActionItems() {
        debugActionItems.addTextItem("Piru-Pea", ignored -> {
            try {
                startActivity(new Intent(this, Class.forName("is.hello.sense.debug.PiruPeaActivity")));
            } catch (ClassNotFoundException e) {
                MessageDialogFragment dialog = MessageDialogFragment.newInstance("Piru-Pea Unavailable", "Bluetooth debugging is only available in internal builds.");
                dialog.showAllowingStateLoss(getFragmentManager(), MessageDialogFragment.TAG);
            }
        });
        debugActionItems.addTextItem("View Log", this::viewLog);
        debugActionItems.addTextItem("Clear Log", this::clearLog);
        debugActionItems.addTextItem("Share Log", this::sendLog);
        debugActionItems.addTextItem("Show Room Check", this::showRoomCheck);
        debugActionItems.addTextItem("Forget welcome dialogs", this::clearHandholdingSettings);
        debugActionItems.addTextItem("Re-enable review prompt", this::reEnableReviewPrompt);
        debugActionItems.addTextItem("Reset app usage stats", this::resetAppUsage);
        debugActionItems.addTextItem("Log Out", this::logOut);
    }


    public void showRoomCheck(@NonNull StaticItemAdapter.TextItem item) {
        Intent onboarding = new Intent(this, OnboardingActivity.class);
        onboarding.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_PILL);
        startActivity(onboarding);
    }

    public void viewLog(@NonNull StaticItemAdapter.TextItem item) {
        startActivity(new Intent(this, SessionLogViewerActivity.class));
    }

    public void clearLog(@NonNull StaticItemAdapter.TextItem item) {
        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(SessionLogger.clearLog(),
                         ignored -> LoadingDialogFragment.close(getFragmentManager()),
                         e -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             ErrorDialogFragment.presentError(this, e);
                         });
    }

    public void sendLog(@NonNull StaticItemAdapter.TextItem item) {
        bindAndSubscribe(SessionLogger.flush(), ignored -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(SessionLogger.getLogFilePath(this))));
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, "Share Log"));
        }, Functions.LOG_ERROR);
    }

    public void clearHandholdingSettings(@NonNull StaticItemAdapter.TextItem item) {
        WelcomeDialogFragment.clearShownStates(this);
        Toast.makeText(getApplicationContext(), "Forgot welcome dialogs", Toast.LENGTH_SHORT).show();
    }

    public void reEnableReviewPrompt(@NonNull StaticItemAdapter.TextItem item) {
        preferences.edit()
                   .putBoolean(PreferencesPresenter.DISABLE_REVIEW_PROMPT, false)
                   .apply();
        localUsageTracker.reset(LocalUsageTracker.Identifier.SKIP_REVIEW_PROMPT);
        Toast.makeText(getApplicationContext(), "Review prompt re-enabled", Toast.LENGTH_SHORT).show();
    }

    public void resetAppUsage(@NonNull StaticItemAdapter.TextItem item) {
        localUsageTracker.resetAsync();
        Toast.makeText(getApplicationContext(), "Usage Stats Reset", Toast.LENGTH_SHORT).show();
    }

    public void logOut(@NonNull StaticItemAdapter.TextItem item) {
        sessionManager.logOut();
        finish();
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        StaticItemAdapter.TextItem item = (StaticItemAdapter.TextItem) adapterView.getItemAtPosition(position);
        if (item.getOnClick() != null) {
            StaticItemAdapter adapter = (StaticItemAdapter) adapterView.getAdapter();
            adapter.onItemClick(adapterView, view, position, id);
        }
    }
}
