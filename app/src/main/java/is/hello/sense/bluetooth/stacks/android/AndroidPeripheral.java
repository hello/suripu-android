package is.hello.sense.bluetooth.stacks.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import is.hello.sense.bluetooth.errors.BluetoothGattError;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.sense.bluetooth.errors.PeripheralConnectionError;
import is.hello.sense.bluetooth.errors.PeripheralServiceDiscoveryFailedError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.OperationTimeout;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.PeripheralService;
import is.hello.sense.bluetooth.stacks.SchedulerOperationTimeout;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.bluetooth.stacks.util.AdvertisingData;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;

import static rx.android.content.ContentObservable.fromBroadcast;

public class AndroidPeripheral implements Peripheral {
    /**
     * How long to delay response after a successful service discovery.
     * <p/>
     * Settled on 5 seconds after experimenting with Jackson. Idea for delay from
     * <a href="https://code.google.com/p/android/issues/detail?id=58381">here</a>.
     */
    private static final int SERVICES_DELAY_S = 5;

    private final @NonNull AndroidBluetoothStack stack;
    private final @NonNull BluetoothDevice bluetoothDevice;
    private final int scannedRssi;
    private final @NonNull AdvertisingData advertisingData;
    private final GattDispatcher gattDispatcher = new GattDispatcher();

    private boolean suspendDisconnectBroadcasts = false;
    private @Nullable BluetoothGatt gatt;
    private @Nullable Subscription bluetoothStateSubscription;

    AndroidPeripheral(@NonNull AndroidBluetoothStack stack,
                      @NonNull BluetoothDevice bluetoothDevice,
                      int scannedRssi,
                      @NonNull AdvertisingData advertisingData) {
        this.stack = stack;
        this.bluetoothDevice = bluetoothDevice;
        this.scannedRssi = scannedRssi;
        this.advertisingData = advertisingData;

        gattDispatcher.addConnectionStateListener((gatt, gattStatus, newState, removeThisListener) -> {
            if (newState == STATUS_DISCONNECTED) {
                closeGatt(gatt);

                if (!suspendDisconnectBroadcasts) {
                    Intent disconnect = new Intent(ACTION_DISCONNECTED);
                    disconnect.putExtra(EXTRA_NAME, getName());
                    disconnect.putExtra(EXTRA_ADDRESS, getAddress());
                    LocalBroadcastManager.getInstance(stack.applicationContext)
                                         .sendBroadcast(disconnect);
                }
            }
        });
    }


    //region Attributes

    @NonNull
    @Override
    public OperationTimeout createOperationTimeout(@NonNull String name, long duration, @NonNull TimeUnit timeUnit) {
        return new SchedulerOperationTimeout(name, duration, timeUnit);
    }

    @Override
    public int getScanTimeRssi() {
        return scannedRssi;
    }

    @Override
    public String getAddress() {
        return bluetoothDevice.getAddress();
    }

    @Override
    public String getName() {
        return bluetoothDevice.getName();
    }

    @NonNull
    @Override
    public AdvertisingData getAdvertisingData() {
        return advertisingData;
    }

    @Override
    @NonNull
    public BluetoothStack getStack() {
        return stack;
    }

    //endregion


    //region Connectivity

    void closeGatt(@Nullable BluetoothGatt gatt) {
        if (gatt != null) {
            Log.i(LOG_TAG, "Closing gatt layer");

            gatt.close();
            if (gatt == this.gatt) {
                gattDispatcher.dispatchDisconnect();
                this.gatt = null;

                if (bluetoothStateSubscription != null) {
                    bluetoothStateSubscription.unsubscribe();
                    this.bluetoothStateSubscription = null;
                }
            }
        }
    }

    @NonNull
    @Override
    public Observable<Peripheral> connect(@NonNull OperationTimeout timeout) {
        return stack.newConfiguredObservable(s -> {
            if (getConnectionStatus() == STATUS_CONNECTED) {
                Logger.warn(LOG_TAG, "Redundant call to connect(), ignoring.");

                s.onNext(this);
                s.onCompleted();
                return;
            } else if (getConnectionStatus() == STATUS_CONNECTING || getConnectionStatus() == STATUS_DISCONNECTING) {
                s.onError(new PeripheralConnectionError("Peripheral is changing connection status."));
                return;
            }

            AtomicBoolean hasRetried = new AtomicBoolean(false);
            GattDispatcher.ConnectionStateListener listener = (gatt, gattStatus, newState, removeThisListener) -> {
                // The first connection attempt made after a user has power cycled their radio,
                // or the connection to a device is unexpectedly lost, will seemingly fail 100%
                // of the time. The error code varies by manufacturer. Retrying silently resolves
                // the issue.
                if (BluetoothGattError.isRecoverableConnectError(gattStatus) && !hasRetried.get()) {
                    Logger.warn(LOG_TAG, "First connection attempt failed due to stack error, retrying.");

                    hasRetried.set(true);
                    gatt.close();
                    this.gatt = bluetoothDevice.connectGatt(stack.applicationContext, false, gattDispatcher);
                    //noinspection ConstantConditions
                    if (gatt != null) {
                        timeout.reschedule();
                    } else {
                        this.suspendDisconnectBroadcasts = false;
                        s.onError(new BluetoothGattError(BluetoothGattError.GATT_INTERNAL_ERROR, BluetoothGattError.Operation.CONNECT));
                    }
                } else if (gattStatus != BluetoothGatt.GATT_SUCCESS) {
                    timeout.unschedule();

                    Logger.error(LOG_TAG, "Could not connect. " + BluetoothGattError.statusToString(gattStatus));
                    this.suspendDisconnectBroadcasts = false;
                    s.onError(new BluetoothGattError(gattStatus, BluetoothGattError.Operation.CONNECT));

                    removeThisListener.call();
                } else if (newState == STATUS_CONNECTED) {
                    timeout.unschedule();

                    Logger.info(LOG_TAG, "Connected " + toString());

                    Observable<Intent> bluetoothStateObserver = fromBroadcast(stack.applicationContext, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
                    this.bluetoothStateSubscription = bluetoothStateObserver.subscribe(intent -> {
                        Logger.info(LOG_TAG, "User disabled bluetooth radio, abandoning connection");

                        if (!stack.getAdapter().isEnabled()) {
                            gatt.disconnect();
                        }
                    }, e -> Logger.error(LOG_TAG, "Bluetooth state observation error", e));

                    this.suspendDisconnectBroadcasts = false;
                    s.onNext(this);
                    s.onCompleted();

                    removeThisListener.call();
                }
            };
            gattDispatcher.addConnectionStateListener(listener);

            timeout.setTimeoutAction(() -> {
                timeout.unschedule();

                gattDispatcher.removeConnectionStateListener(listener);
                if (bluetoothStateSubscription != null) {
                    bluetoothStateSubscription.unsubscribe();
                    this.bluetoothStateSubscription = null;
                }

                this.suspendDisconnectBroadcasts = false;
                s.onError(new OperationTimeoutError(OperationTimeoutError.Operation.CONNECT));
            }, stack.scheduler);

            Logger.info(LOG_TAG, "Connecting " + toString());

            setPairingConfirmation(true);
            if (gatt != null) {
                if (gatt.connect()) {
                    this.suspendDisconnectBroadcasts = true;
                    timeout.schedule();
                } else {
                    s.onError(new BluetoothGattError(BluetoothGattError.GATT_INTERNAL_ERROR, BluetoothGattError.Operation.CONNECT));
                }
            } else {
                this.gatt = bluetoothDevice.connectGatt(stack.applicationContext, false, gattDispatcher);
                if (gatt != null) {
                    this.suspendDisconnectBroadcasts = true;
                    timeout.schedule();
                } else {
                    s.onError(new BluetoothGattError(BluetoothGattError.GATT_INTERNAL_ERROR, BluetoothGattError.Operation.CONNECT));
                }
            }
        });
    }

    @NonNull
    @Override
    public Observable<Peripheral> disconnect() {
        return stack.newConfiguredObservable(s -> {
            int connectionStatus = getConnectionStatus();
            if (connectionStatus == STATUS_DISCONNECTED || connectionStatus == STATUS_DISCONNECTING || gatt == null) {
                s.onNext(this);
                s.onCompleted();
                return;
            } else if (connectionStatus == STATUS_CONNECTING) {
                s.onError(new PeripheralConnectionError("Peripheral is connecting"));
                return;
            }

            gattDispatcher.addConnectionStateListener((gatt, gattStatus, newState, removeThisListener) -> {
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (gattStatus != BluetoothGatt.GATT_SUCCESS) {
                        Logger.info(LOG_TAG, "Could not disconnect " + toString() + "; " + BluetoothGattError.statusToString(gattStatus));

                        s.onError(new BluetoothGattError(gattStatus, BluetoothGattError.Operation.DISCONNECT));
                    } else {
                        Logger.info(LOG_TAG, "Disconnected " + toString());

                        s.onNext(this);
                        s.onCompleted();
                    }

                    removeThisListener.call();
                }
            });

            Logger.info(LOG_TAG, "Disconnecting " + toString());
            gatt.disconnect();
        });
    }

    @Override
    public @ConnectivityStatus int getConnectionStatus() {
        if (gatt != null) {
            @ConnectivityStatus int status = stack.bluetoothManager.getConnectionState(bluetoothDevice, BluetoothProfile.GATT);
            return status;
        } else {
            return STATUS_DISCONNECTED;
        }
    }

    //endregion


    //region Internal

    private <T> void setupTimeout(@NonNull OperationTimeoutError.Operation operation,
                                  @NonNull OperationTimeout timeout,
                                  @NonNull Subscriber<T> subscriber,
                                  @Nullable Action0 disconnectListener) {
        timeout.setTimeoutAction(() -> {
            switch (operation) {
                case DISCOVER_SERVICES:
                    gattDispatcher.onServicesDiscovered = null;
                    break;

                case SUBSCRIBE_NOTIFICATION:
                    gattDispatcher.onDescriptorWrite = null;
                    break;

                case UNSUBSCRIBE_NOTIFICATION:
                    gattDispatcher.onDescriptorWrite = null;
                    break;

                case WRITE_COMMAND:
                    gattDispatcher.onCharacteristicWrite = null;
                    break;
            }
            if (disconnectListener != null) {
                gattDispatcher.removeDisconnectListener(disconnectListener);
            }

            disconnect().subscribe(ignored -> subscriber.onError(new OperationTimeoutError(operation)),
                                   e -> subscriber.onError(new OperationTimeoutError(operation, e)));

            timeout.unschedule();
        }, stack.scheduler);
    }

    //endregion


    //region Bonding

    private void setPairingConfirmation(boolean confirm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            bluetoothDevice.setPairingConfirmation(confirm);
        } else {
            try {
                Method m = bluetoothDevice.getClass().getMethod("setPairingConfirmation", boolean.class);
                m.setAccessible(true);
                m.invoke(bluetoothDevice, confirm);
            } catch (NoSuchMethodException e) {
                Logger.error(LOG_TAG, "Could not find setPairingConfirmation", e);
            } catch (InvocationTargetException | IllegalAccessException e) {
                Logger.error(LOG_TAG, "Could not call setPairingConfirmation", e);
            }
        }
    }

    private Observable<Intent> createBondChangeReceiver() {
        return fromBroadcast(stack.applicationContext, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                .subscribeOn(stack.scheduler)
                .filter(intent -> {
                    BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    return (bondedDevice != null && bondedDevice.getAddress().equals(bluetoothDevice.getAddress()));
                });
    }

    private Observable<Intent> listenForBonding() {
        return stack.newConfiguredObservable(s -> {
            createBondChangeReceiver().subscribe(new Subscriber<Intent>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    s.onError(e);
                }

                @Override
                public void onNext(Intent intent) {
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                    Logger.debug(Peripheral.LOG_TAG, "Bond status changed from " + PeripheralBondAlterationError.getBondStateString(previousState) + " to " + PeripheralBondAlterationError.getBondStateString(state));

                    if (state == BluetoothDevice.BOND_BONDED) {
                        Logger.debug(LOG_TAG, "Bonding succeeded.");

                        s.onNext(intent);
                        s.onCompleted();

                        unsubscribe();
                    } else if (state == BluetoothDevice.ERROR || state == BOND_NONE && previousState == BOND_BONDING) {
                        int reason = intent.getIntExtra(PeripheralBondAlterationError.EXTRA_REASON, PeripheralBondAlterationError.REASON_UNKNOWN_FAILURE);
                        Logger.error(LOG_TAG, "Bonding failed for reason " + PeripheralBondAlterationError.getReasonString(reason), null);
                        s.onError(new PeripheralBondAlterationError(reason));

                        unsubscribe();
                    }
                }
            });
        });
    }

    @Override
    public @BondStatus int getBondStatus() {
        @BondStatus int bondStatus = bluetoothDevice.getBondState();
        return bondStatus;
    }

    //endregion


    //region Discovering Services

    @NonNull
    @Override
    public Observable<Map<UUID, PeripheralService>> discoverServices(@NonNull OperationTimeout timeout) {
        Observable<Map<UUID, PeripheralService>> discoverServices = stack.newConfiguredObservable(s -> {
            if (getConnectionStatus() != STATUS_CONNECTED || gatt == null) {
                s.onError(new PeripheralConnectionError());
                return;
            }

            Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(s, timeout);
            setupTimeout(OperationTimeoutError.Operation.DISCOVER_SERVICES, timeout, s, onDisconnect);

            gattDispatcher.onServicesDiscovered = (gatt, status) -> {
                timeout.unschedule();

                gattDispatcher.removeDisconnectListener(onDisconnect);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Map<UUID, PeripheralService> services = AndroidPeripheralService.wrapGattServices(gatt.getServices());
                    s.onNext(services);
                    s.onCompleted();

                    gattDispatcher.onServicesDiscovered = null;
                } else {
                    Logger.error(LOG_TAG, "Could not discover services. " + BluetoothGattError.statusToString(status));

                    s.onError(new BluetoothGattError(status, BluetoothGattError.Operation.DISCOVER_SERVICES));

                    gattDispatcher.onServicesDiscovered = null;
                }
            };

            if (gatt.discoverServices()) {
                timeout.schedule();
            } else {
                gattDispatcher.onServicesDiscovered = null;

                s.onError(new PeripheralServiceDiscoveryFailedError());
            }
        });

        // See <https://code.google.com/p/android/issues/detail?id=58381>
        return discoverServices.delay(SERVICES_DELAY_S, TimeUnit.SECONDS);
    }

    @NonNull
    @Override
    public Observable<PeripheralService> discoverService(@NonNull UUID serviceIdentifier, @NonNull OperationTimeout timeout) {
        return discoverServices(timeout).flatMap(services -> {
            PeripheralService service = services.get(serviceIdentifier);
            if (service != null) {
                return Observable.just(service);
            } else {
                return Observable.error(new PeripheralServiceDiscoveryFailedError());
            }
        });
    }


    //endregion


    //region Characteristics

    private @NonNull BluetoothGattService getGattService(@NonNull PeripheralService onPeripheralService) {
        return ((AndroidPeripheralService) onPeripheralService).service;
    }

    @NonNull
    @Override
    public Observable<UUID> subscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                  @NonNull UUID characteristicIdentifier,
                                                  @NonNull UUID descriptorIdentifier,
                                                  @NonNull OperationTimeout timeout) {
        return stack.newConfiguredObservable(s -> {
            if (getConnectionStatus() != STATUS_CONNECTED || gatt == null) {
                s.onError(new PeripheralConnectionError());
                return;
            }

            BluetoothGattService service = getGattService(onPeripheralService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicIdentifier);
            if (gatt.setCharacteristicNotification(characteristic, true)) {
                Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(s, timeout);
                setupTimeout(OperationTimeoutError.Operation.SUBSCRIBE_NOTIFICATION, timeout, s, onDisconnect);

                BluetoothGattDescriptor descriptorToWrite = characteristic.getDescriptor(descriptorIdentifier);
                descriptorToWrite.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                gattDispatcher.onDescriptorWrite = (gatt, descriptor, status) -> {
                    if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                        return;
                    }

                    timeout.unschedule();

                    if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                        Logger.info(LOG_TAG, "Waiting for implicit bond");
                        listenForBonding().subscribe(intent -> {
                            Logger.debug(LOG_TAG, "Retrying descriptor write.");
                            if (gatt.writeDescriptor(descriptorToWrite)) {
                                timeout.schedule();
                            } else {
                                s.onError(new BluetoothGattError(status, BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION));
                                gattDispatcher.onDescriptorWrite = null;
                                gattDispatcher.removeDisconnectListener(onDisconnect);
                            }
                        }, s::onError);

                        return;
                    }

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        s.onNext(characteristicIdentifier);
                        s.onCompleted();
                    } else {
                        Logger.error(LOG_TAG, "Could not subscribe to characteristic. " + BluetoothGattError.statusToString(status), null);
                        s.onError(new BluetoothGattError(status, BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION));
                    }

                    gattDispatcher.onDescriptorWrite = null;
                    gattDispatcher.removeDisconnectListener(onDisconnect);
                };

                if (gatt.writeDescriptor(descriptorToWrite)) {
                    timeout.schedule();
                } else {
                    gattDispatcher.onDescriptorWrite = null;
                    gattDispatcher.removeDisconnectListener(onDisconnect);

                    s.onError(new BluetoothGattError(BluetoothGatt.GATT_FAILURE, BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION));
                }
            } else {
                s.onError(new BluetoothGattError(BluetoothGatt.GATT_WRITE_NOT_PERMITTED, BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION));
            }
        });
    }

    @NonNull
    @Override
    public Observable<UUID> unsubscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                    @NonNull UUID characteristicIdentifier,
                                                    @NonNull UUID descriptorIdentifier,
                                                    @NonNull OperationTimeout timeout) {
        return stack.newConfiguredObservable(s -> {
            if (getConnectionStatus() != STATUS_CONNECTED || gatt == null) {
                s.onError(new PeripheralConnectionError());
                return;
            }

            BluetoothGattService service = getGattService(onPeripheralService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicIdentifier);

            Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(s, timeout);
            setupTimeout(OperationTimeoutError.Operation.SUBSCRIBE_NOTIFICATION, timeout, s, onDisconnect);

            gattDispatcher.onDescriptorWrite = (gatt, descriptor, status) -> {
                if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    return;
                }

                timeout.unschedule();

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (gatt.setCharacteristicNotification(characteristic, false)) {
                        s.onNext(characteristicIdentifier);
                        s.onCompleted();
                    } else {
                        Logger.error(LOG_TAG, "Could not unsubscribe from characteristic. " + BluetoothGattError.statusToString(status));
                        s.onError(new BluetoothGattError(BluetoothGatt.GATT_FAILURE, BluetoothGattError.Operation.UNSUBSCRIBE_NOTIFICATION));
                    }
                } else {
                    s.onError(new BluetoothGattError(status, BluetoothGattError.Operation.UNSUBSCRIBE_NOTIFICATION));
                }

                gattDispatcher.removeDisconnectListener(onDisconnect);
                gattDispatcher.onDescriptorWrite = null;
            };

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorIdentifier);
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            if (gatt.writeDescriptor(descriptor)) {
                timeout.schedule();
            } else {
                gattDispatcher.removeDisconnectListener(onDisconnect);
                gattDispatcher.onDescriptorWrite = null;

                s.onError(new BluetoothGattError(BluetoothGatt.GATT_WRITE_NOT_PERMITTED, BluetoothGattError.Operation.UNSUBSCRIBE_NOTIFICATION));
            }
        });
    }

    @NonNull
    @Override
    public Observable<Void> writeCommand(@NonNull PeripheralService onPeripheralService,
                                         @NonNull UUID identifier,
                                         @NonNull WriteType writeType,
                                         @NonNull byte[] payload,
                                         @NonNull OperationTimeout timeout) {
        return stack.newConfiguredObservable(s -> {
            if (getConnectionStatus() != STATUS_CONNECTED || gatt == null) {
                s.onError(new PeripheralConnectionError());
                return;
            }

            Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(s, timeout);
            setupTimeout(OperationTimeoutError.Operation.SUBSCRIBE_NOTIFICATION, timeout, s, onDisconnect);

            gattDispatcher.onCharacteristicWrite = (gatt, characteristic, status) -> {
                timeout.unschedule();

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Logger.error(LOG_TAG, "Could not write command " + identifier + ", " + BluetoothGattError.statusToString(status));
                    s.onError(new BluetoothGattError(status, BluetoothGattError.Operation.WRITE_COMMAND));
                } else {
                    s.onNext(null);
                    s.onCompleted();
                }

                gattDispatcher.removeDisconnectListener(onDisconnect);
                gattDispatcher.onCharacteristicWrite = null;
            };

            BluetoothGattService service = getGattService(onPeripheralService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(identifier);
            // Looks like write type might need to be specified for some phones. See
            // <http://stackoverflow.com/questions/25888817/android-bluetooth-status-133-in-oncharacteristicwrite>
            characteristic.setWriteType(writeType.value);
            characteristic.setValue(payload);
            if (gatt.writeCharacteristic(characteristic)) {
                timeout.schedule();
            } else {
                gattDispatcher.removeDisconnectListener(onDisconnect);
                gattDispatcher.onCharacteristicWrite = null;

                s.onError(new BluetoothGattError(BluetoothGatt.GATT_WRITE_NOT_PERMITTED, BluetoothGattError.Operation.WRITE_COMMAND));
            }
        });
    }

    @Override
    public void setPacketHandler(@Nullable PacketHandler dataHandler) {
        gattDispatcher.packetHandler = dataHandler;
    }

    //endregion


    @Override
    public String toString() {
        return "{AndroidPeripheral " +
                "name=" + getName() +
                ", address=" + getAddress() +
                ", connectionStatus=" + getConnectionStatus() +
                ", bondStatus=" + getBondStatus() +
                ", scannedRssi=" + getScanTimeRssi() +
                '}';
    }
}
