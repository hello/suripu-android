package is.hello.sense.ui.activities;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import net.hockeyapp.android.FeedbackManager;

import javax.inject.Inject;

import is.hello.sense.api.ApiEnvironment;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

public class DebugActivity extends InjectionActivity implements AdapterView.OnItemClickListener {
    @Inject ApiSessionManager sessionManager;
    @Inject BuildValues buildValues;
    @Inject ApiEnvironment currentEnvironment;

    private StaticItemAdapter debugItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListView listView = new ListView(this);
        setContentView(listView);

        this.debugItems = new StaticItemAdapter(this);
        debugItems.setValueMaxLength(30);
        listView.setAdapter(debugItems);
        listView.setOnItemClickListener(this);

        addDescriptiveItems();
        addActions();
    }


    private void addDescriptiveItems() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            debugItems.addItem("App Version", packageInfo.versionName);
            debugItems.addItem("Build Number", Integer.toString(packageInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            Logger.debug(DebugActivity.class.getSimpleName(), "Could not look up app version", e);
        }
        debugItems.addItem("Build Type", buildValues.type);
        debugItems.addItem("Access Token", sessionManager.getAccessToken());
        debugItems.addItem("GCM ID", getSharedPreferences(Constants.NOTIFICATION_PREFS, 0).getString(Constants.NOTIFICATION_PREF_REGISTRATION_ID, "<none>"));
        debugItems.addItem("Host", currentEnvironment.baseUrl);
        debugItems.addItem("Client ID", currentEnvironment.clientId);
    }

    private void addActions() {
        debugItems.addItem("Environment", currentEnvironment.toString(), this::changeEnvironment);
        debugItems.addItem("Feedback", null, this::sendFeedback);
    }


    public void changeEnvironment() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ArrayAdapter<ApiEnvironment> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, ApiEnvironment.values());
        builder.setAdapter(adapter, (dialog, which) -> {
            ApiEnvironment newEnvironment = adapter.getItem(which);
            if (newEnvironment == currentEnvironment)
                return;

            SharedPreferences internalPreferences = getSharedPreferences(Constants.INTERNAL_PREFS, 0);
            internalPreferences.edit()
                    .putString(Constants.INTERNAL_PREF_API_ENV_NAME, newEnvironment.toString())
                    .apply();

            sessionManager.logOut(this);
        });
        builder.setCancelable(true);
        builder.create().show();
    }

    public void sendFeedback() {
        FeedbackManager.register(this, Constants.HOCKEY_APP_ID, null);
        FeedbackManager.showFeedbackActivity(this);
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        StaticItemAdapter.Item item = debugItems.getItem(position);
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
