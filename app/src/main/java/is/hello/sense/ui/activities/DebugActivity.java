package is.hello.sense.ui.activities;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiEnvironment;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

public class DebugActivity extends InjectionActivity implements AdapterView.OnItemClickListener {
    @Inject ApiSessionManager sessionManager;
    @Inject BuildValues buildValues;
    @Inject ApiEnvironment currentEnvironment;

    private DebugItemAdapter debugItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListView listView = new ListView(this);
        setContentView(listView);

        this.debugItems = new DebugItemAdapter(this);
        listView.setAdapter(debugItems);
        listView.setOnItemClickListener(this);

        addDescriptiveItems();
        addActions();
    }


    private void addDescriptiveItems() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            debugItems.add(new DebugItem("App Version", packageInfo.versionName));
            debugItems.add(new DebugItem("Build Number", Integer.toString(packageInfo.versionCode)));
        } catch (PackageManager.NameNotFoundException e) {
            Logger.debug(Logger.tagFromClass(DebugActivity.class), "Could not look up app version", e);
        }
        debugItems.add(new DebugItem("Build Type", buildValues.type));
        debugItems.add(new DebugItem("Access Token", sessionManager.getAccessToken()));
        debugItems.add(new DebugItem("Host", currentEnvironment.baseUrl));
        debugItems.add(new DebugItem("Client ID", currentEnvironment.clientId));
    }

    private void addActions() {
        debugItems.add(new DebugItem("Environment", currentEnvironment.toString(), this::changeEnvironment));
        debugItems.add(new DebugItem(getString(R.string.action_log_out), null, this::clearSession));
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
            launchOnBoarding();
        });
        builder.setCancelable(true);
        builder.create().show();
    }

    public void clearSession() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title_log_out);
        builder.setMessage(R.string.dialog_message_log_out);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            sessionManager.logOut(this);
            launchOnBoarding();
        });
        builder.create().show();
    }

    public void launchOnBoarding() {
        startActivity(new Intent(this, OnboardingActivity.class));
        finish();
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        DebugItem item = debugItems.getItem(position);
        if (item.action != null) {
            item.action.run();
        } else {
            String value = item.title + ": " + item.value;
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText(item.title, value));
            Toast.makeText(getApplicationContext(), "Copied to Clipboard", Toast.LENGTH_SHORT).show();
        }
    }


    private class DebugItem {
        public final String title;
        public final String value;
        public final Runnable action;

        private DebugItem(@NonNull String title, @Nullable String value, @Nullable Runnable action) {
            this.title = title;
            this.value = value;
            this.action = action;
        }

        private DebugItem(@NonNull String title, @Nullable String value) {
            this(title, value, null);
        }
    }


    private static class DebugItemAdapter extends ArrayAdapter<DebugItem> {
        private final LayoutInflater layoutInflater;

        private DebugItemAdapter(Context context) {
            super(context, R.layout.list_horizontal_item);

            this.layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = layoutInflater.inflate(R.layout.list_horizontal_item, parent, false);
                view.setTag(new ViewHolder(view));
            }

            DebugItem item = getItem(position);
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.title.setText(item.title);
            holder.detail.setText(item.value);

            return view;
        }

        private class ViewHolder {
            public final TextView title;
            public final TextView detail;

            public ViewHolder(@NonNull View view) {
                this.title = (TextView) view.findViewById(R.id.list_horizontal_item_title);
                this.detail = (TextView) view.findViewById(R.id.list_horizontal_item_detail);
            }
        }
    }
}
