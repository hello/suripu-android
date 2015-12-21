package is.hello.sense.debug;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.DelegatingTimelineService;
import is.hello.sense.api.NonsenseTimelineService;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.util.Logger;

public class TimelineSourceActivity extends InjectionActivity
        implements AdapterView.OnItemClickListener, NsdManager.DiscoveryListener {
    @Inject DelegatingTimelineService delegatingTimelineService;
    @Inject OkHttpClient httpClient;
    @Inject Gson gson;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private NsdManager netServiceDiscovery;
    private NonsenseAdapter adapter;


    //region Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline_source);

        this.netServiceDiscovery = (NsdManager) getSystemService(Context.NSD_SERVICE);

        final ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        final TextView useApiItem =
                (TextView) getLayoutInflater().inflate(R.layout.item_simple_text, listView, false);
        useApiItem.setText(R.string.action_use_default_host);
        listView.addHeaderView(useApiItem);

        this.adapter = new NonsenseAdapter(this);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        netServiceDiscovery.discoverServices(NonsenseTimelineService.NET_SERVICE_TYPE,
                                             NsdManager.PROTOCOL_DNS_SD,
                                             this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        netServiceDiscovery.stopServiceDiscovery(this);
    }

    //endregion


    //region Changing Hosts

    public void useNonsenseHost(@NonNull NsdServiceInfo serviceInfo) {
        final String host = "http://" + serviceInfo.getHost().getHostAddress() + ":" + serviceInfo.getPort();
        Logger.debug(getClass().getSimpleName(), "useNonsenseHost(" + host + ")");

        final NonsenseTimelineService service = new NonsenseTimelineService(httpClient, gson, host);
        delegatingTimelineService.setDelegate(service);

        setResult(RESULT_OK);
        finish();
    }

    public void useApi() {
        delegatingTimelineService.reset();

        setResult(RESULT_OK);
        finish();
    }

    //endregion


    //region Service Discovery

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Logger.error(getClass().getSimpleName(), "onStartDiscoveryFailed(" + serviceType + ", " + errorCode + ")");
        handler.post(adapter::clear);
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Logger.error(getClass().getSimpleName(), "onStopDiscoveryFailed(" + serviceType + ", " + errorCode + ")");
        handler.post(adapter::clear);
    }

    @Override
    public void onDiscoveryStarted(String serviceType) {
        Logger.debug(getClass().getSimpleName(), "onDiscoveryStarted(" + serviceType + ")");
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        Logger.debug(getClass().getSimpleName(), "onDiscoveryStopped(" + serviceType + ")");
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        final String serviceName = serviceInfo.getServiceName();
        if (serviceName.contains(NonsenseTimelineService.NET_SERVICE_NAME)) {
            netServiceDiscovery.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                @Override
                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Logger.error(getClass().getSimpleName(),
                                 "onResolveFailed(" + serviceInfo + ", " + errorCode + ")");
                }

                @Override
                public void onServiceResolved(NsdServiceInfo serviceInfo) {
                    handler.post(() -> adapter.add(serviceInfo));
                }
            });
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        final String serviceName = serviceInfo.getServiceName();
        if (serviceName.contains(NonsenseTimelineService.NET_SERVICE_NAME)) {
            handler.post(() -> adapter.remove(serviceInfo));
        }
    }

    //endregion


    //region List View

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final NsdServiceInfo item = (NsdServiceInfo) parent.getItemAtPosition(position);
        if (item != null) {
            useNonsenseHost(item);
        } else {
            useApi();
        }
    }

    class NonsenseAdapter extends ArrayAdapter<NsdServiceInfo> {
        public NonsenseAdapter(@NonNull Context context) {
            super(context, R.layout.item_simple_text);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final TextView textView = (TextView) super.getView(position, convertView, parent);
            final NsdServiceInfo serviceInfo = getItem(position);
            textView.setText(serviceInfo.getHost().getHostAddress());
            return textView;
        }
    }

    //endregion
}
