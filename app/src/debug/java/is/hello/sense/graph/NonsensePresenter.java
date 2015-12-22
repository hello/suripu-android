package is.hello.sense.graph;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.annotation.NonNull;

import java.net.InetAddress;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.debug.NamedApiEndpoint;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.util.Logger;

public class NonsensePresenter extends Presenter implements NsdManager.DiscoveryListener {
    public static final String NET_SERVICE_NAME = "nonsense-server";
    public static final String NET_SERVICE_TYPE = "_http._tcp.";

    private final NsdManager netServiceDiscovery;

    public final PresenterSubject<Event> events = PresenterSubject.create();

    @Inject
    public NonsensePresenter(@NonNull Context context) {
        this.netServiceDiscovery = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
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
        events.onError(new RuntimeException("Could not start nonsense discovery: " + errorCode));
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        logEvent("onStopDiscoveryFailed(" + serviceType + ", " + errorCode + ")");
        events.onError(new RuntimeException("Could not stop nonsense discovery: " + errorCode));
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
                    events.onNext(new Event(Event.Type.FOUND, endpointFromServiceInfo(serviceInfo)));
                }
            });
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        final String serviceName = serviceInfo.getServiceName();
        if (serviceName.contains(NET_SERVICE_NAME) && serviceInfo.getHost() != null) {
            events.onNext(new Event(Event.Type.LOST, endpointFromServiceInfo(serviceInfo)));
        }
    }

    private static NamedApiEndpoint endpointFromServiceInfo(@NonNull NsdServiceInfo serviceInfo) {
        final InetAddress host = serviceInfo.getHost();
        return new NamedApiEndpoint(BuildConfig.CLIENT_ID, BuildConfig.CLIENT_SECRET,
                                    "http://" + host.getHostAddress() + ":" + serviceInfo.getPort(),
                                    serviceInfo.getServiceName());
    }


    public static class Event {
        @NonNull
        public final Type type;

        @NonNull
        public final NamedApiEndpoint endpoint;

        Event(@NonNull Type type, @NonNull NamedApiEndpoint endpoint) {
            this.type = type;
            this.endpoint = endpoint;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "type=" + type +
                    ", endpoint=" + endpoint +
                    '}';
        }

        public enum Type {
            FOUND,
            LOST,
        }
    }
}
