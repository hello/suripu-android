package is.hello.sense.bluetooth.stacks.transmission;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.UUID;

import is.hello.sense.bluetooth.errors.BluetoothConnectionLostError;
import is.hello.sense.util.Logger;
import rx.functions.Action1;

public abstract class PacketParser<T> {
    public static final String LOG_TAG = "Bluetooth/" + PacketParser.class.getSimpleName();

    //region Primitive Methods

    public abstract boolean shouldProcessCharacteristic(@NonNull UUID characteristicUUID);
    public abstract void processPacket(@NonNull UUID characteristicUUID, @NonNull byte[] packet);
    protected abstract void cleanUp();

    //endregion


    //region Propagating Data

    public @Nullable Action1<Throwable> onError;
    public @Nullable Action1<T> onResponse;

    protected void onError(@NonNull Throwable e) {
        Logger.error(LOG_TAG, "Data error " + e);

        if (this.onError != null) {
            this.onError.call(e);

            clearListeners();
        }
    }

    protected void onResponse(@Nullable final T data) {
        Logger.info(LOG_TAG, "Finished decoding packets into " + data);

        if (this.onResponse != null) {
            this.onResponse.call(data);
        }
    }

    protected void onTransportDisconnected() {
        cleanUp();
        onError(new BluetoothConnectionLostError());
    }

    public void clearListeners() {
        this.onError = null;
        this.onResponse = null;
    }

    public boolean hasListeners() {
        return (this.onError != null && this.onResponse != null);
    }

    //endregion
}
