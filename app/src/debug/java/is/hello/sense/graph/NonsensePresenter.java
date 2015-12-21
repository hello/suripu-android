package is.hello.sense.graph;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

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
        events.onNext(new Event(Event.Type.FAILED, null));
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        logEvent("onStopDiscoveryFailed(" + serviceType + ", " + errorCode + ")");
        events.onNext(new Event(Event.Type.FAILED, null));
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
                    events.onNext(new Event(Event.Type.FOUND, serviceInfo));
                }
            });
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        final String serviceName = serviceInfo.getServiceName();
        if (serviceName.contains(NET_SERVICE_NAME)) {
            events.onNext(new Event(Event.Type.LOST, serviceInfo));
        }
    }


    public static class Event {
        @NonNull
        public final Type type;

        @Nullable
        public final NsdServiceInfo serviceInfo;

        Event(@NonNull Type type, @Nullable NsdServiceInfo serviceInfo) {
            this.type = type;
            this.serviceInfo = serviceInfo;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "type=" + type +
                    ", serviceInfo=" + serviceInfo +
                    '}';
        }

        public enum Type {
            FAILED,
            FOUND,
            LOST,
        }
    }
}
