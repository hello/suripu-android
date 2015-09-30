package is.hello.sense.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.io.File;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiEndpoint;
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
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import is.hello.sense.util.SessionLogger;

public class DebugActivity extends InjectionActivity implements AdapterView.OnItemClickListener {
    @Inject ApiSessionManager sessionManager;
    @Inject ApiEndpoint apiEndpoint;
    @Inject BluetoothStack bluetoothStack;
    @Inject PreferencesPresenter preferences;
    @Inject LocalUsageTracker localUsageTracker;

    private StaticItemAdapter debugActionItems;
    private StaticItemAdapter buildInfoItems;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);


        this.buildInfoItems = new StaticItemAdapter(this);
        populateBuildInfoItems();

        this.debugActionItems = new StaticItemAdapter(this);
        populateDebugActionItems();


        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(debugActionItems);
        listView.setOnItemClickListener(this);


        SelectorView selector = (SelectorView) findViewById(R.id.activity_debug_modes);
        selector.setButtonTags(debugActionItems, buildInfoItems);
        selector.setSelectedIndex(0);
        selector.setOnSelectionChangedListener(index -> listView.setAdapter((StaticItemAdapter) selector.getButtonTagAt(index)));
        selector.setBackground(new TabsBackgroundDrawable(getResources(), TabsBackgroundDrawable.Style.INLINE));
    }


    private void populateBuildInfoItems() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            buildInfoItems.addTextItem("App Version", packageInfo.versionName);
            buildInfoItems.addTextItem("Build Number", Integer.toString(packageInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            Logger.debug(DebugActivity.class.getSimpleName(), "Could not look up app version", e);
        }
        buildInfoItems.addTextItem("Build Type", BuildConfig.BUILD_TYPE);
        buildInfoItems.addTextItem("Device Model", Build.MODEL);
        buildInfoItems.addTextItem("Device Manufacturer", Build.MANUFACTURER);
        buildInfoItems.addTextItem("Device Brand", Build.BRAND);
        buildInfoItems.addTextItem("BLE Device Support", bluetoothStack.getDeviceSupportLevel().toString());
        buildInfoItems.addTextItem("Access Token", sessionManager.getAccessToken());
        buildInfoItems.addTextItem("GCM ID", getSharedPreferences(Constants.NOTIFICATION_PREFS, 0).getString(Constants.NOTIFICATION_PREF_REGISTRATION_ID, "<none>"));
        buildInfoItems.addTextItem("Host", apiEndpoint.getUrl());
        buildInfoItems.addTextItem("Client ID", apiEndpoint.getClientId());
    }

    private void populateDebugActionItems() {
        debugActionItems.addTextItem("Piru-Pea", null, ignored -> {
            try {
                startActivity(new Intent(this, Class.forName("is.hello.sense.debug.PiruPeaActivity")));
            } catch (ClassNotFoundException e) {
                MessageDialogFragment dialog = MessageDialogFragment.newInstance("Piru-Pea Unavailable", "Bluetooth debugging is only available in internal builds.");
                dialog.showAllowingStateLoss(getFragmentManager(), MessageDialogFragment.TAG);
            }
        });
        debugActionItems.addTextItem("View Log", null, this::viewLog);
        debugActionItems.addTextItem("Clear Log", null, this::clearLog);
        debugActionItems.addTextItem("Share Log", null, this::sendLog);
        debugActionItems.addTextItem("Show Room Check", null, this::showRoomCheck);
        debugActionItems.addTextItem("Forget welcome dialogs", null, this::clearHandholdingSettings);
        debugActionItems.addTextItem("Re-enable review prompt", null, this::reEnableReviewPrompt);
        debugActionItems.addTextItem("Reset app usage stats", null, this::resetAppUsage);
        debugActionItems.addTextItem("Log Out", null, this::logOut);

        if (Crashlytics.getInstance().isInitialized()) {
            debugActionItems.addTextItem("Crash", null, this::crash);
        }
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

    public void crash(@NonNull StaticItemAdapter.TextItem item) {
        SenseAlertDialog confirm = new SenseAlertDialog(this);
        confirm.setTitle("Are you sure?");
        confirm.setMessage("A report of this crash will be uploaded to Crashlytics.");
        confirm.setPositiveButton("Crash", (dialog, which) -> {
            Crashlytics.log("Simulating crash");
            throw new RuntimeException("Simulated crash");
        });
        confirm.setNegativeButton(android.R.string.cancel, null);
        confirm.setButtonDestructive(SenseAlertDialog.BUTTON_POSITIVE, true);
        confirm.show();
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
        } else {
            String value = item.getTitle() + ": " + item.getDetail();
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText(item.getTitle(), value));
            Toast.makeText(getApplicationContext(), "Copied to Clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}
