package is.hello.sense.debug;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiEndpoint;
import is.hello.sense.api.DynamicApiEndpoint;
import is.hello.sense.graph.NonsensePresenter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.widget.SenseAlertDialog;

public class EnvironmentActivity extends InjectionActivity
        implements AdapterView.OnItemClickListener {
    private static final String OTHER_ITEM = "other";

    @Inject NonsensePresenter nonsensePresenter;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_environment);

        this.preferences = PreferenceManager.getDefaultSharedPreferences(this);


        final ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        final TextView other = (TextView) getLayoutInflater().inflate(R.layout.item_simple_text,
                                                                      listView, false);
        other.setBackgroundResource(R.drawable.selectable_dark_bounded);
        other.setText(R.string.label_environment_other);
        listView.addFooterView(other, OTHER_ITEM, true);

        final Adapter adapter = new Adapter(this,
            new NamedApiEndpoint("android_dev", "99999secret",
                                 "https://dev-api.hello.is", "Dev"),
            new NamedApiEndpoint("8d3c1664-05ae-47e4-bcdb-477489590aa4",
                                 "4f771f6f-5c10-4104-bbc6-3333f5b11bf9",
                                 "https://canary-api.hello.is", "Canary"),
            new NamedApiEndpoint("8d3c1664-05ae-47e4-bcdb-477489590aa4",
                                 "4f771f6f-5c10-4104-bbc6-3333f5b11bf9",
                                 "https://api.hello.is", "Production")
        );
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        nonsensePresenter.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        nonsensePresenter.stop();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object item = parent.getItemAtPosition(position);
        if (item == OTHER_ITEM) {
            createCustomApiEndpoint();
        } else {
            ApiEndpoint endpoint = (ApiEndpoint) item;
            selectEndpoint(endpoint);
            finish();
        }
    }

    private void selectEndpoint(@NonNull ApiEndpoint endpoint) {
        preferences.edit()
                   .putString(DynamicApiEndpoint.PREF_CLIENT_ID_OVERRIDE, endpoint.getClientId())
                   .putString(DynamicApiEndpoint.PREF_CLIENT_SECRET_OVERRIDE, endpoint.getClientSecret())
                   .putString(DynamicApiEndpoint.PREF_API_ENDPOINT_OVERRIDE, endpoint.getUrl())
                   .apply();
    }

    private void createCustomApiEndpoint() {
        final SenseAlertDialog endpointDialog = new SenseAlertDialog(this);
        endpointDialog.setView(R.layout.dialog_custom_endpoint);

        final TextView urlText = (TextView) endpointDialog.findViewById(R.id.dialog_custom_endpoint_url);
        urlText.setText(BuildConfig.BASE_URL);

        final TextView idText = (TextView) endpointDialog.findViewById(R.id.dialog_custom_endpoint_client_id);
        idText.setText(BuildConfig.CLIENT_ID);

        final TextView secretText = (TextView) endpointDialog.findViewById(R.id.dialog_custom_endpoint_client_secret);
        secretText.setText(BuildConfig.CLIENT_SECRET);

        endpointDialog.setOnDismissListener(ignored -> {
            final Context context = endpointDialog.getContext();
            final InputMethodManager imm =
                    (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isActive()) {
                final View decorView = endpointDialog.getWindow().getDecorView();
                imm.hideSoftInputFromWindow(decorView.getWindowToken(), 0);
            }
        });
        endpointDialog.setNegativeButton(android.R.string.cancel, null);
        endpointDialog.setPositiveButton(android.R.string.ok, (ignored, which) -> {
            final String url = urlText.getText().toString(),
                         id = idText.getText().toString(),
                         secret = secretText.getText().toString();

            if (TextUtils.isEmpty(url) ||
                    TextUtils.isEmpty(id) ||
                    TextUtils.isEmpty(secret)) {
                return;
            }

            final ApiEndpoint endpoint = new ApiEndpoint(id, secret, url);
            selectEndpoint(endpoint);

            // Soft keyboard won't dismiss otherwise.
            new Handler().post(this::finish);
        });
        endpointDialog.show();
    }


    private static class Adapter extends ArrayAdapter<ApiEndpoint> {
        public Adapter(@NonNull Context context, @NonNull ApiEndpoint... endpoints) {
            super(context, R.layout.item_simple_text, endpoints);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text = (TextView) super.getView(position, convertView, parent);

            ApiEndpoint endpoint = getItem(position);
            text.setText(endpoint.getName());

            return text;
        }
    }

    static class NamedApiEndpoint extends ApiEndpoint {
        private final String name;

        public NamedApiEndpoint(@NonNull String clientId,
                                @NonNull String clientSecret,
                                @NonNull String url,
                                @NonNull String name) {
            super(clientId, clientSecret, url);

            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
