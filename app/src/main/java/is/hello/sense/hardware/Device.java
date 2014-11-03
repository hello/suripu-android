package is.hello.sense.hardware;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.UUID;

import rx.Observable;

public interface Device {
    //region Signal Strength

    int getScannedRssi();

    //endregion


    //region Connectivity

    @NonNull Observable<Device> connect(@NonNull UUID targetService);
    @NonNull Observable<Device> disconnect();
    boolean isConnected();

    //endregion


    //region Bonding

    @NonNull Observable<Device> bond();
    @NonNull Observable<Device> unbond();
    boolean isBonded();

    //endregion


    //region Discovering Services

    @NonNull Observable<List<Service>> discoverServices();
    @Nullable Service getService(@NonNull UUID serviceIdentifier);

    //endregion


    //region Characteristics

    @NonNull Observable<UUID> subscribeNotification(@NonNull Service onService,
                                                    @NonNull UUID characteristicIdentifier,
                                                    @NonNull UUID descriptorIdentifier);
    @NonNull Observable<UUID> unsubscribeNotification(@NonNull Service onService,
                                                      @NonNull UUID characteristicIdentifier,
                                                      @NonNull UUID descriptorIdentifier);

    @NonNull Observable<Void> writeCommand(@NonNull Service onService, @NonNull Command command);
    @NonNull Observable<Command> incomingPackets();

    //endregion
}
