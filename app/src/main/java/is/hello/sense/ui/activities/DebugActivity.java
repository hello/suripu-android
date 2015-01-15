package is.hello.sense.ui.activities;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.squareup.okhttp.Cache;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiEnvironment;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import is.hello.sense.util.SessionLogger;

public class DebugActivity extends InjectionActivity implements AdapterView.OnItemClickListener {
    @Inject ApiSessionManager sessionManager;
    @Inject Cache httpCache;
    @Inject BuildValues buildValues;
    @Inject ApiEnvironment currentEnvironment;
    @Inject BluetoothStack bluetoothStack;

    private StaticItemAdapter debugActionItems;
    private StaticItemAdapter buildInfoItems;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);


        this.buildInfoItems = new StaticItemAdapter(this);
        buildInfoItems.setValueMaxLength(30);
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
            buildInfoItems.addItem("App Version", packageInfo.versionName);
            buildInfoItems.addItem("Build Number", Integer.toString(packageInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            Logger.debug(DebugActivity.class.getSimpleName(), "Could not look up app version", e);
        }
        buildInfoItems.addItem("Build Type", buildValues.type);
        buildInfoItems.addItem("Device Model", Build.MODEL);
        buildInfoItems.addItem("BLE Device Support", bluetoothStack.getDeviceSupportLevel().toString());
        buildInfoItems.addItem("BLE Stack Traits", TextUtils.join(", ", bluetoothStack.getTraits()));
        buildInfoItems.addItem("Access Token", sessionManager.getAccessToken());
        buildInfoItems.addItem("GCM ID", getSharedPreferences(Constants.NOTIFICATION_PREFS, 0).getString(Constants.NOTIFICATION_PREF_REGISTRATION_ID, "<none>"));
        buildInfoItems.addItem("Host", currentEnvironment.baseUrl);
        buildInfoItems.addItem("Client ID", currentEnvironment.clientId);
    }

    private void populateDebugActionItems() {
        debugActionItems.addItem("Piru-Pea", null, () -> {
            try {
                startActivity(new Intent(this, Class.forName("is.hello.sense.debug.PiruPeaActivity")));
            } catch (ClassNotFoundException e) {
                MessageDialogFragment dialog = MessageDialogFragment.newInstance("Piru-Pea Unavailable", "Bluetooth debugging is only available in internal builds.");
                dialog.show(getFragmentManager(), MessageDialogFragment.TAG);
            }
        });
        debugActionItems.addItem("Set Environment", currentEnvironment.toString(), this::changeEnvironment);
        debugActionItems.addItem("View Log", null, this::viewLog);
        debugActionItems.addItem("Clear Log", null, this::clearLog);
        debugActionItems.addItem("Share Log", null, this::sendLog);
        debugActionItems.addItem("Clear Http Cache", null, this::clearHttpCache);
        debugActionItems.addItem("Clear OAuth Session", null, this::clearOAuthSession);
    }


    public void changeEnvironment() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ArrayAdapter<ApiEnvironment> adapter = new ArrayAdapter<>(this, R.layout.item_simple_text, ApiEnvironment.values());
        builder.setAdapter(adapter, (dialog, which) -> {
            ApiEnvironment newEnvironment = adapter.getItem(which);
            if (newEnvironment == currentEnvironment)
                return;

            SharedPreferences internalPreferences = getSharedPreferences(Constants.INTERNAL_PREFS, 0);
            internalPreferences.edit()
                    .putString(Constants.INTERNAL_PREF_API_ENV_NAME, newEnvironment.toString())
                    .apply();

            sessionManager.logOut();
        });
        builder.setCancelable(true);
        builder.create().show();
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
        StaticItemAdapter.Item item = debugActionItems.getItem(position);
        if (item.getAction() != null) {
            item.getAction().run();
        } else {
            String value = item.getTitle() + ": " + item.getValue();
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText(item.getTitle(), value));
            Toast.makeText(getApplicationContext(), "Copied to Clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}
