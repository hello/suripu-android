package is.hello.sense.debug;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.ApiEndpoint;
import is.hello.sense.api.DynamicApiEndpoint;
import is.hello.sense.ui.activities.SenseActivity;

public class EnvironmentActivity extends SenseActivity implements AdapterView.OnItemClickListener {
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_environment);

        this.preferences = PreferenceManager.getDefaultSharedPreferences(this);


        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        Adapter adapter = new Adapter(this,
            new NamedApiEndpoint("android_dev", "99999secret", "https://dev-api.hello.is", "Dev"),
            new NamedApiEndpoint("8d3c1664-05ae-47e4-bcdb-477489590aa4", "4f771f6f-5c10-4104-bbc6-3333f5b11bf9", "https://api.hello.is", "Production")
        );
        listView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ApiEndpoint endpoint = (ApiEndpoint) parent.getItemAtPosition(position);

        preferences.edit()
                .putString(DynamicApiEndpoint.PREF_CLIENT_ID_OVERRIDE, endpoint.getClientId())
                .putString(DynamicApiEndpoint.PREF_CLIENT_SECRET_OVERRIDE, endpoint.getClientSecret())
                .putString(DynamicApiEndpoint.PREF_API_ENDPOINT_OVERRIDE, endpoint.getUrl())
                .apply();

        finish();
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
