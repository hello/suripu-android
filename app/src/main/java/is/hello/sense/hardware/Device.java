package is.hello.sense.hardware;

import android.support.annotation.NonNull;

import java.util.UUID;

import rx.Observable;

public interface Device {
    public static final String ACTION_CONNECTED = Device.class.getName() + ".ACTION_CONNECTED";
    public static final String ACTION_DISCONNECTED = Device.class.getName() + ".ACTION_DISCONNECTED";

    @NonNull Observable<Device> connect();
    @NonNull Observable<Device> disconnect();
    boolean isConnected();

    public static final String ACTION_BONDED = Device.class.getName() + ".ACTION_BONDED";
    public static final String ACTION_UNBONDED = Device.class.getName() + ".ACTION_UNBONDED";

    @NonNull Observable<Device> bond();
    @NonNull Observable<Device> unbond();
    boolean isBonded();

    @NonNull Observable<UUID> subscribeNotification(@NonNull UUID characteristicIdentifier);
    @NonNull Observable<UUID> unsubscribeNotification(@NonNull UUID characteristicIdentifier);
}
