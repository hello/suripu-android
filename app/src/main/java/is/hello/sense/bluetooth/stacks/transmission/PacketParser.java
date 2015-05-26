package is.hello.sense.bluetooth.stacks.transmission;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.UUID;

import is.hello.sense.bluetooth.errors.BluetoothConnectionLostError;
import is.hello.sense.util.Logger;
import rx.functions.Action1;

/**
 * Responsible for decoding incoming packets. Separate from
 * {@link PacketHandler} to allow for state isolation.
 *
 * @param <T>   The type of value produced by the parser.
 */
public abstract class PacketParser<T> {
    public static final String LOG_TAG = "Bluetooth/" + PacketParser.class.getSimpleName();

    //region Primitive Methods

    /**
     * Returns whether or not the parser can process a packet
     * coming from a specified characteristic.
     */
    public abstract boolean canProcessPacket(@NonNull UUID characteristicUUID);

    /**
     * Process a single packet from a specified characteristic.
     * <p/>
     * The implementation of this method should inform the outside
     * world of state changes through the {@link #dispatchError(Throwable)}
     * and {@link #dispatchResponse(Object)} methods.
     */
    public abstract void processPacket(@NonNull UUID characteristicUUID, @NonNull byte[] packet);

    /**
     * Cleans up any state in the parser.
     * <p />
     * Implementations of {@link #processPacket(UUID, byte[])} should
     * call this method to do clean-up after they are finished.
     */
    protected abstract void cleanUp();

    //endregion


    //region Propagating Data

    /**
     * The registered error listener.
     */
    public @Nullable Action1<Throwable> errorListener;

    /**
     * The registered response listener.
     */
    public @Nullable Action1<T> responseListener;

    /**
     * Pass off the given Throwable to the registered error listener.
     */
    protected void dispatchError(@NonNull Throwable e) {
        Logger.error(LOG_TAG, "Data error " + e);

        if (this.errorListener != null) {
            this.errorListener.call(e);

            clearListeners();
        }
    }

    /**
     * Pass off the fully parsed response value to the registered response listener.
     */
    protected void dispatchResponse(@Nullable final T data) {
        Logger.info(LOG_TAG, "Finished decoding packets into " + data);

        if (this.responseListener != null) {
            this.responseListener.call(data);
        }
    }

    /**
     * Informs the parser that the Bluetooth transport disconnected.
     * <p />
     * Default implementation calls {@link #cleanUp()} and dispatches an error.
     */
    protected void onTransportDisconnected() {
        cleanUp();
        dispatchError(new BluetoothConnectionLostError());
    }

    /**
     * Clears the parser's listeners.
     */
    public void clearListeners() {
        this.errorListener = null;
        this.responseListener = null;
    }

    /**
     * Returns whether or not the parser has listeners.
     * <p/>
     * The parser will only have listeners when there is a command in-flight.
     */
    public boolean hasListeners() {
        return (this.errorListener != null && this.responseListener != null);
    }

    //endregion
}
