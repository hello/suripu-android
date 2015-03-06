package is.hello.sense.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.squareup.okhttp.Cache;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import is.hello.sense.util.SessionLogger;

public class DebugActivity extends InjectionActivity implements AdapterView.OnItemClickListener {
    @Inject ApiSessionManager sessionManager;
    @Inject Cache httpCache;
    @Inject BluetoothStack bluetoothStack;

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


        SelectorLinearLayout selector = (SelectorLinearLayout) findViewById(R.id.activity_debug_modes);
        selector.setButtonTags(debugActionItems, buildInfoItems);
        selector.setSelectedIndex(0);
        selector.setOnSelectionChangedListener(index -> listView.setAdapter((StaticItemAdapter) selector.getButtonTag(index)));
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
        buildInfoItems.addTextItem("BLE Device Support", bluetoothStack.getDeviceSupportLevel().toString());
        buildInfoItems.addTextItem("BLE Stack Traits", TextUtils.join(", ", bluetoothStack.getTraits()));
        buildInfoItems.addTextItem("Access Token", sessionManager.getAccessToken());
        buildInfoItems.addTextItem("GCM ID", getSharedPreferences(Constants.NOTIFICATION_PREFS, 0).getString(Constants.NOTIFICATION_PREF_REGISTRATION_ID, "<none>"));
        buildInfoItems.addTextItem("Host", BuildConfig.BASE_URL);
        buildInfoItems.addTextItem("Client ID", BuildConfig.CLIENT_ID);
    }

    private void populateDebugActionItems() {
        debugActionItems.addTextItem("Piru-Pea", null, () -> {
            try {
                startActivity(new Intent(this, Class.forName("is.hello.sense.debug.PiruPeaActivity")));
            } catch (ClassNotFoundException e) {
                MessageDialogFragment dialog = MessageDialogFragment.newInstance("Piru-Pea Unavailable", "Bluetooth debugging is only available in internal builds.");
                dialog.show(getFragmentManager(), MessageDialogFragment.TAG);
            }
        });
        debugActionItems.addTextItem("View Log", null, this::viewLog);
        debugActionItems.addTextItem("Clear Log", null, this::clearLog);
        debugActionItems.addTextItem("Share Log", null, this::sendLog);
        debugActionItems.addTextItem("Show Room Check", null, this::showRoomCheck);
        debugActionItems.addTextItem("Forget welcome dialogs", null, this::clearHandholdingSettings);
        debugActionItems.addTextItem("Clear Http Cache", null, this::clearHttpCache);
        debugActionItems.addTextItem("Clear OAuth Session", null, this::clearOAuthSession);
    }


    public void showRoomCheck() {
        Intent onboarding = new Intent(this, OnboardingActivity.class);
        onboarding.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_PILL);
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
                             ErrorDialogFragment.presentError(getFragmentManager(), e);
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

    public void clearHttpCache() {
        try {
            httpCache.evictAll();
            Toast.makeText(getApplicationContext(), "Cache Cleared", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            ErrorDialogFragment.presentError(getFragmentManager(), e);
        }
    }

    public void clearOAuthSession() {
        sessionManager.setSession(null);
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        StaticItemAdapter.TextItem item = (StaticItemAdapter.TextItem) adapterView.getItemAtPosition(position);
        if (item.getAction() != null) {
            item.getAction().run();
        } else {
            String value = item.getTitle() + ": " + item.getDetail();
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText(item.getTitle(), value));
            Toast.makeText(getApplicationContext(), "Copied to Clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}
