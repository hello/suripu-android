package is.hello.sense.graph;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.api.ApiEndpoint;
import is.hello.sense.debug.NamedApiEndpoint;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.util.Logger;

public class NonsensePresenter extends Presenter implements NsdManager.DiscoveryListener {
    public static final String NET_SERVICE_NAME = "nonsense-server";
    public static final String NET_SERVICE_TYPE = "_http._tcp.";

    private final NsdManager netServiceDiscovery;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<NsdServiceInfo> discovered = new ArrayList<>();

    public final PresenterSubject<List<? extends ApiEndpoint>> endpoints = PresenterSubject.create();

    @Inject
    public NonsensePresenter(@NonNull Context context) {
        this.netServiceDiscovery = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        endpoints.onNext(Collections.<ApiEndpoint>emptyList());
    }

    public void start() {
        netServiceDiscovery.discoverServices(NET_SERVICE_TYPE,
                                             NsdManager.PROTOCOL_DNS_SD,
                                             this);
    }

    public void stop() {
        netServiceDiscovery.stopServiceDiscovery(this);
    }


    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        logEvent("onStartDiscoveryFailed(" + serviceType + ", " + errorCode + ")");
        endpoints.onError(new RuntimeException("Could not start nonsense discovery: " + errorCode));
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        logEvent("onStopDiscoveryFailed(" + serviceType + ", " + errorCode + ")");
        endpoints.onError(new RuntimeException("Could not stop nonsense discovery: " + errorCode));
    }

    @Override
    public void onDiscoveryStarted(String serviceType) {
        logEvent("onDiscoveryStarted(" + serviceType + ")");
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        logEvent("onDiscoveryStopped(" + serviceType + ")");
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        logEvent("onServiceFound(" + serviceInfo + ")");

        final String serviceName = serviceInfo.getServiceName();
        if (serviceName.contains(NET_SERVICE_NAME)) {
            netServiceDiscovery.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                @Override
                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Logger.error(getClass().getSimpleName(),
                                 "onResolveFailed(" + serviceInfo + ", " + errorCode + ")");
                }

                @Override
                public void onServiceResolved(NsdServiceInfo serviceInfo) {
                    logEvent("onServiceResolved(" + serviceInfo + ")");

                    handler.post(() -> {
                        discovered.add(serviceInfo);
                        propagateUpdate();
                    });
                }
            });
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        logEvent("onServiceLost(" + serviceInfo + ")");

        final String serviceName = serviceInfo.getServiceName();
        if (serviceName.contains(NET_SERVICE_NAME)) {
            handler.post(() -> {
                // Unfortunately, the system doesn't retain resolved service info, so there's
                // absolutely no way to match up the 'lost' service with one in our discovered
                // List. So we just clear the discovered list and hope for the best.
                discovered.clear();
                propagateUpdate();
            });
        }
    }

    private void propagateUpdate() {
        this.endpoints.onNext(Lists.map(discovered, service -> {
            final String host = service.getHost().getHostAddress();
            final String url = "http://" + host + ":" + service.getPort();
            final String name = service.getServiceName() + " - " + host;
            return new NamedApiEndpoint(BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET,
                                        url, name);
        }));
    }
}
