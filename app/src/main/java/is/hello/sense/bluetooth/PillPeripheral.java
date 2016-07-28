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

import is.hello.buruberi.bluetooth.errors.ServiceDiscoveryException;
import is.hello.buruberi.bluetooth.stacks.GattCharacteristic;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.GattService;
import is.hello.buruberi.bluetooth.stacks.OperationTimeout;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.Bytes;
import is.hello.sense.bluetooth.exceptions.BleCacheException;
import is.hello.sense.bluetooth.exceptions.PillCharNotFoundException;
import is.hello.sense.bluetooth.exceptions.PillNotFoundException;
import is.hello.sense.functional.Functions;
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
    private static final int TIME_OUT_SECONDS = 30;
    private static final int DFU_TIME_OUT_SECONDS = 80;
    private static final int RACE_CONDITION_DELAY_SECONDS = 10;
    private static final int minRSSI = -70;
    private static final String TAG = PillPeripheral.class.getSimpleName();

    public enum PillState {
        BondRemoved,
        Disconnected,
        Connected,
        Wiped,
        DfuMode
    }
    //endregion


    //region Fields

    private final GattPeripheral gattPeripheral;
    private final boolean inDfuMode;
    private GattService service;
    private DfuCallback dfuCallback;

    //endregion


    public static boolean isPillDfu(@Nullable final AdvertisingData advertisingData) {
        return advertisingData != null
                && advertisingData.anyRecordMatches(
                AdvertisingData.TYPE_INCOMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS,
                b -> Arrays.equals(DFU_ADVERTISEMENT_SERVICE_128_BIT, b));
    }

    public static boolean isPillNormal(@Nullable final AdvertisingData advertisingData) {
        return advertisingData != null
                && advertisingData.anyRecordMatches(
                AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS,
                b -> Arrays.equals(PillPeripheral.NORMAL_ADVERTISEMENT_SERVICE_128_BIT, b));
    }

    //region Creation
    PillPeripheral(@NonNull final GattPeripheral gattPeripheral) {
        this.gattPeripheral = gattPeripheral;
        this.inDfuMode = isPillDfu(gattPeripheral.getAdvertisingData());
    }
    //endregion

    // Could probably move this function to something like PillDfuPresenter, but it made the flow of
    // ConnectPillFragment easy to manage by returning the PillPeripheral. If we ever need to clear
    // the cache from another part of the app we should consider moving this code to be a better
    // utility function.
    public Observable<PillPeripheral> clearCache(@NonNull final Context context) {
        stateChanged(PillState.DfuMode);
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
                                    Log.d(TAG, "Failed to get refresh method: " + localException);
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
                Log.d(TAG, "NoSuchFieldException: " + e);
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                Log.d(TAG, "IllegalAccessException: " + e);
                e.printStackTrace();
            }
        });
    }

    //region Attributes

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

    public boolean isConnected() {
        return (gattPeripheral.getConnectionStatus() == GattPeripheral.STATUS_CONNECTED &&
                service != null);
    }

    private void stateChanged(final PillState state) {
        if (this.dfuCallback != null) {
            dfuCallback.onStateChange(state);
            if (state == PillState.DfuMode){
                dfuCallback = null;
            }
        }
    }
    //endregion


    //region Timeouts

    @NonNull
    private OperationTimeout createOperationTimeout(@NonNull final String name) {
        return gattPeripheral.createOperationTimeout(name, TIME_OUT_SECONDS, TimeUnit.SECONDS);
    }

    //endregion


    //region Connecting
    @NonNull
    public Observable<PillPeripheral> enterDfuMode(@NonNull final Context context, @NonNull final DfuCallback callback) {
        this.dfuCallback = callback;
        Log.d(TAG, "enterDfuMode()");
        if (inDfuMode) {
            stateChanged(PillState.DfuMode);
            return Observable.just(this);
        }
        return removeBond()
                .flatMap(PillPeripheral::disconnect)
                .flatMap(PillPeripheral::connect)
                .flatMap(PillPeripheral::wipeFirmware)
                .flatMap(pillPeripheral3 -> pillPeripheral3.clearCache(context))
                .timeout(DFU_TIME_OUT_SECONDS, TimeUnit.SECONDS)
                .delay(RACE_CONDITION_DELAY_SECONDS, TimeUnit.SECONDS)
                .doOnError(throwable -> {
                    if (throwable instanceof ServiceDiscoveryException || throwable instanceof PillCharNotFoundException) {
                        this.clearCache(context).subscribe(Functions.NO_OP, Functions.LOG_ERROR);
                    }
                });
    }

    public Observable<PillPeripheral> disconnect() {
        stateChanged(PillState.Disconnected);
        Log.d(TAG, "disconnect()");
        if (inDfuMode) {
            return Observable.error(new IllegalStateException("Cannot disconnect to sleep pill in dfu mode."));
        }

        return gattPeripheral.disconnect()
                             .delay(RACE_CONDITION_DELAY_SECONDS, TimeUnit.SECONDS)
                             .map(disconnectedPeripheral -> {
                                 Log.d(TAG, "disconnected");
                                 return this;
                             });
    }

    @NonNull
    public Observable<PillPeripheral> connect() {
        stateChanged(PillState.Connected);
        Log.d(TAG, "connect()");
        if (inDfuMode) {
            return Observable.error(new IllegalStateException("Cannot connect to sleep pill in dfu mode."));
        }

        return gattPeripheral.connect(GattPeripheral.CONNECT_FLAG_TRANSPORT_LE, createOperationTimeout("Connect"))
                             .flatMap(connectedPeripheral -> {
                                 Log.d(TAG, "discoverService(" + SERVICE + ")");
                                 return connectedPeripheral.discoverService(SERVICE, createOperationTimeout("Discover Service"));
                             })
                             .map(gattService -> {
                                 Log.d(TAG, "connected");
                                 this.service = gattService;
                                 return this;
                             });
    }

    @NonNull
    public Observable<PillPeripheral> removeBond() {
        stateChanged(PillState.BondRemoved);
        Log.d(TAG, "removeBond()");
        return gattPeripheral.removeBond(createOperationTimeout("Remove Bond TimeOut")).map(ignored -> {
            Log.d(TAG, "bond removed");
            return this;
        });
    }

    //endregion


    //region Commands

    @NonNull
    private Observable<Void> writeCommand(@NonNull final UUID identifier,
                                          @NonNull final GattPeripheral.WriteType writeType,
                                          @NonNull final byte[] payload) {
        Log.d(TAG, "writeCommand(" + identifier + ", " + writeType + ", " + Arrays.toString(payload) + ")");

        if (!isConnected()) {
            return Observable.error(new PillNotFoundException("writeCommand(...) requires a connection"));
        }

        final GattCharacteristic commandCharacteristic = service.getCharacteristic(identifier);

        if (commandCharacteristic == null) {
            return Observable.error(new PillCharNotFoundException(identifier));
        }

        return commandCharacteristic.write(writeType,
                                           payload,
                                           createOperationTimeout("Write Command"));
    }

    public Observable<PillPeripheral> wipeFirmware() {
        stateChanged(PillState.Wiped);
        Log.d(TAG, "wipeFirmware()");

        final byte[] payload = {COMMAND_WIPE_FIRMWARE};
        return writeCommand(CHARACTERISTIC_COMMAND_UUID, GattPeripheral.WriteType.NO_RESPONSE, payload)
                /**
                 * We don't wait for a response from the write command. In order to avoid a race
                 * condition we delay.
                 */
                .delay(RACE_CONDITION_DELAY_SECONDS, TimeUnit.SECONDS) //this is important
                .map(aVoid -> this);
    }

    //endregion

    public interface DfuCallback {
        void onStateChange(PillState state);
    }

}
