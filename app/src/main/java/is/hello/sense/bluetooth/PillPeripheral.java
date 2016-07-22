package is.hello.sense.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.GattService;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.Bytes;
import is.hello.sense.bluetooth.exceptions.BleCacheException;
import is.hello.sense.bluetooth.exceptions.PillNotFoundException;
import rx.Observable;
import rx.Subscriber;


//todo move to commonsense or commonpill??? after this is working
public final class PillPeripheral implements Serializable {

    //region Identifiers
    public static final byte[] NORMAL_ADVERTISEMENT_SERVICE_128_BIT = Bytes.fromString("23D1BCEA5F782315DEEF121210E10000");
    public static final byte[] DFU_ADVERTISEMENT_SERVICE_128_BIT = Bytes.fromString("23D1BCEA5F782315DEEF121230150000");
    private static final UUID SERVICE = UUID.fromString("0000e110-1212-efde-1523-785feabcd123");
    private static final UUID CHARACTERISTIC_COMMAND_UUID = UUID.fromString("0000DEED-0000-1000-8000-00805F9B34FB");
    private static final byte COMMAND_WIPE_FIRMWARE = 8;
    private static final int TIME_OUT_SECONDS = 10;
    private static final int minRSSI = -70;

    //endregion


    //region Fields

    private final GattPeripheral gattPeripheral;
    private final boolean inDfuMode;
    private GattService service;
    private boolean isUpdating;

    //endregion


    public static boolean isPillDfu(@Nullable final AdvertisingData advertisingData) {
        return advertisingData != null && advertisingData.anyRecordMatches(AdvertisingData.TYPE_INCOMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, b -> Arrays.equals(DFU_ADVERTISEMENT_SERVICE_128_BIT, b));
    }

    public static boolean isPillNormal(@Nullable final AdvertisingData advertisingData) {
        return advertisingData != null && advertisingData.anyRecordMatches(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, b -> Arrays.equals(PillPeripheral.NORMAL_ADVERTISEMENT_SERVICE_128_BIT, b));
    }

    //region Creation
    PillPeripheral(@NonNull final GattPeripheral gattPeripheral) {
        this.gattPeripheral = gattPeripheral;
        this.inDfuMode = isPillDfu(gattPeripheral.getAdvertisingData());
        this.isUpdating = false;
    }
    //endregion

    // Could probably move this function to something like PillDfuPresenter, but it made the flow of
    // ConnectPillFragment easy to manage by returning the PillPeripheral. If we ever need to clear
    // the cache from another part of the app we should consider moving this code to be a better
    // utility function.
    public Observable<PillPeripheral> clearCache(@NonNull final Context context) {
        return Observable.create(subscriber -> {
            try {
                final boolean[] cacheCleared = {false};
                final Field field = gattPeripheral.getClass().getDeclaredField("bluetoothDevice");
                field.setAccessible(true);
                final BluetoothDevice bluetoothDevice = (BluetoothDevice) field.get(gattPeripheral);
                if (bluetoothDevice != null) {
                    bluetoothDevice.connectGatt(context, true, new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                try {
                                    final Method localMethod = gatt.getClass().getMethod("refresh");
                                    if (localMethod != null) {
                                        cacheCleared[0] = (boolean) (Boolean) localMethod.invoke(gatt);
                                    }
                                } catch (final Exception localException) {
                                    Log.d(getClass().getSimpleName(), "Failed to get refresh method: " + localException);
                                    // Don't exit from here. We need to make sure we disconnect so
                                    // the user can try again.
                                }
                                gatt.disconnect();
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                if (cacheCleared[0]) {
                                    subscriber.onNext(PillPeripheral.this);
                                    subscriber.onCompleted();
                                } else {
                                    subscriber.onError(new BleCacheException());
                                }
                            }
                        }
                    });
                }

            } catch (NoSuchFieldException e) {
                //todo I read that the above should trigger this error but I haven't seen it on my 5x.
                // if we confirm that this error is not commonly triggered for our supported devices
                // we can have it throw an error.
                Log.d(getClass().getSimpleName(), "NoSuchFieldException: " + e);
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                Log.d(getClass().getSimpleName(), "IllegalAccessException: " + e);
                e.printStackTrace();
            }
        });
    }

    //region Attributes

    public boolean isInDfuMode() {
        return inDfuMode;
    }

    /**
     * @return if the {@link DfuService} has started to update this pill peripheral
     */
    public boolean isUpdating() {
        return isUpdating;
    }

    public void setIsUpdating(final boolean isUpdating){
        this.isUpdating = isUpdating;
    }

    public int getScanTimeRssi() {
        return gattPeripheral.getScanTimeRssi();
    }

    public String getAddress() {
        return gattPeripheral.getAddress();
    }

    public String getName() {
        return gattPeripheral.getName();
    }

    public boolean isTooFar() {
        return getScanTimeRssi() < minRSSI;
    }
    //endregion


    //region Timeouts

    @NonNull
    private OperationTimeout createOperationTimeout(@NonNull final String name) {
        return gattPeripheral.createOperationTimeout(name, 30, TimeUnit.SECONDS);
    }

    //endregion


    //region Connecting
    @NonNull
    public Observable<PillPeripheral> enterDfuMode(@NonNull final Context context) {
        Log.d(getClass().getSimpleName(), "enterDfuMode()");
        if (inDfuMode) {
            return Observable.create(new Observable.OnSubscribe<PillPeripheral>() {
                @Override
                public void call(final Subscriber<? super PillPeripheral> subscriber) {
                    subscriber.onNext(PillPeripheral.this);
                    subscriber.onCompleted();
                }
            });
        }
        return removeBond()
                .flatMap(PillPeripheral::connect)
                .flatMap(PillPeripheral::wipeFirmware)
                .flatMap(pillPeripheral3 -> pillPeripheral3.clearCache(context))
                .timeout(20, TimeUnit.SECONDS)
                .delay(5, TimeUnit.SECONDS); // avoid any potential race conditions
    }

    @NonNull
    public Observable<PillPeripheral> connect() {
        Log.d(getClass().getSimpleName(), "connect()");
        if (inDfuMode) {
            return Observable.error(new IllegalStateException("Cannot connect to sleep pill in dfu mode."));
        }

        final OperationTimeout operationTimeout = createOperationTimeout("Connect");
        return gattPeripheral.connect(GattPeripheral.CONNECT_FLAG_DEFAULTS, operationTimeout)
                             .flatMap(connectedPeripheral -> {
                                 Log.d(getClass().getSimpleName(), "discoverService(" + SERVICE + ")");
                                 return connectedPeripheral.discoverService(SERVICE, operationTimeout);
                             })
                             .map(gattService -> {
                                 Log.d(getClass().getSimpleName(), "connected");
                                 this.service = gattService;
                                 return this;
                             });
    }

    @NonNull
    public Observable<PillPeripheral> removeBond() {
        Log.d(getClass().getSimpleName(), "removeBond()");
        return gattPeripheral.removeBond(createOperationTimeout("Remove Bond TimeOut")).map(ignored -> {
            Log.d(getClass().getSimpleName(), "bond removed");
            return this;
        });
    }

    public boolean isConnected() {
        return (gattPeripheral.getConnectionStatus() == GattPeripheral.STATUS_CONNECTED &&
                service != null);
    }

    //endregion


    //region Commands

    @NonNull
    private Observable<Void> writeCommand(@NonNull final UUID identifier,
                                          @NonNull final GattPeripheral.WriteType writeType,
                                          @NonNull final byte[] payload) {
        Log.d(getClass().getSimpleName(), "writeCommand(" + identifier + ", " + writeType + ", " + Arrays.toString(payload) + ")");

        if (!isConnected()) {
            return Observable.error(new PillNotFoundException("writeCommand(...) requires a connection"));
        }

        return service.getCharacteristic(identifier)
                      .write(writeType,
                             payload,
                             gattPeripheral.createOperationTimeout("Animation",
                                                                   TIME_OUT_SECONDS,
                                                                   TimeUnit.SECONDS));
    }

    public Observable<PillPeripheral> wipeFirmware() {
        Log.d(getClass().getSimpleName(), "wipeFirmware()");

        final byte[] payload = {COMMAND_WIPE_FIRMWARE};
        return writeCommand(CHARACTERISTIC_COMMAND_UUID, GattPeripheral.WriteType.NO_RESPONSE, payload)
                /**
                 * We don't wait for a response from the write command. In order to avoid a race
                 * condition we delay.
                 */
                .delay(5, TimeUnit.SECONDS) //this is important
                .map(aVoid -> this);
    }

    //endregion


}
