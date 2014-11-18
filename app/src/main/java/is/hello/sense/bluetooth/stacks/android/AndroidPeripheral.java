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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import is.hello.sense.bluetooth.errors.BluetoothGattError;
import is.hello.sense.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.errors.PeripheralConnectionError;
import is.hello.sense.bluetooth.errors.PeripheralServiceDiscoveryFailedError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.OperationTimeout;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.PeripheralService;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.bluetooth.stacks.util.TakesOwnership;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

import static rx.android.observables.AndroidObservable.fromBroadcast;

public class AndroidPeripheral implements Peripheral {
    private final @NonNull AndroidBluetoothStack stack;
    private final @NonNull BluetoothDevice bluetoothDevice;
    private final int scannedRssi;
    private final GattDispatcher gattDispatcher = new GattDispatcher();

    private BluetoothGatt gatt;
    private Map<UUID, PeripheralService> cachedPeripheralServices;
    private Subscription bluetoothStateSubscription;

    AndroidPeripheral(@NonNull AndroidBluetoothStack stack,
                      @NonNull BluetoothDevice bluetoothDevice,
                      int scannedRssi) {
        this.stack = stack;
        this.bluetoothDevice = bluetoothDevice;
        this.scannedRssi = scannedRssi;

        gattDispatcher.addConnectionStateListener((gatt, gattStatus, newState, removeThisListener) -> {
            if (newState == STATUS_DISCONNECTED) {
                closeGatt(gatt);
            }
        });
    }


    //region Introspection

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
    public Observable<Peripheral> connect() {
        if (this.gatt != null) {
            if (getConnectionStatus() == STATUS_CONNECTED) {
                Logger.warn(LOG_TAG, "Redundant call to connect(), ignoring.");

                return Observable.just(this);
            } else if (getConnectionStatus() == STATUS_CONNECTING || getConnectionStatus() == STATUS_DISCONNECTING) {
                return Observable.error(new PeripheralConnectionError("Peripheral is changing connection status."));
            }
        }

        return stack.newConfiguredObservable(s -> {
            AtomicBoolean hasRetried = new AtomicBoolean(false);
            gattDispatcher.addConnectionStateListener((gatt, gattStatus, newState, removeThisListener) -> {
                // The first connection attempt made after the user has power-cycled their
                // bluetooth radio will result in a 133/gatt stack error. Trying again
                // seems to work 100% of the time.
                if (gattStatus == BluetoothGattError.STACK_ERROR && !hasRetried.get()) {
                    Logger.warn(LOG_TAG, "First connection attempt failed due to stack error, retrying.");

                    hasRetried.set(true);
                    gatt.close();
                    this.gatt = bluetoothDevice.connectGatt(stack.applicationContext, false, gattDispatcher);
                } else if (gattStatus != BluetoothGatt.GATT_SUCCESS) {
                    Logger.error(LOG_TAG, "Could not connect. " + BluetoothGattError.statusToString(gattStatus));
                    s.onError(new BluetoothGattError(gattStatus));

                    removeThisListener.call();
                } else if (newState == STATUS_CONNECTED) {
                    Logger.info(LOG_TAG, "Connected " + toString());

                    Observable<Intent> bluetoothStateObserver = fromBroadcast(stack.applicationContext, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
                    this.bluetoothStateSubscription = bluetoothStateObserver.subscribe(intent -> {
                        Logger.info(LOG_TAG, "User disabled bluetooth radio, abandoning connection");

                        if (!stack.adapter.isEnabled()) {
                            gatt.disconnect();
                        }
                    }, e -> Logger.error(LOG_TAG, "Bluetooth state observation error", e));

                    if (getBondStatus() == BOND_NONE) {
                        s.onNext(this);
                        s.onCompleted();

                        removeThisListener.call();
                    } else {
                        // Pre-existing bonding information guarantees that every
                        // other connection will not function correctly, so we just
                        // get rid of it right off to save us the trouble later.

                        Logger.info(LOG_TAG, "Previously persisted bonding discovered, removing.");

                        removeBond().subscribe(ignored -> {
                            Logger.info(LOG_TAG, "Previously persisted bonding removed, reconnecting.");

                            // We can't reuse a BluetoothGatt object after removing a bond, because, whatever.
                            gatt.close();
                            this.gatt = bluetoothDevice.connectGatt(stack.applicationContext, false, gattDispatcher);
                        }, e -> {
                            Logger.warn(LOG_TAG, "Could not remove previously persisted bonding information, ignoring.", e);

                            s.onNext(this);
                            s.onCompleted();

                            removeThisListener.call();
                        });
                    }
                }
            });

            Logger.info(LOG_TAG, "Connecting " + toString());

            if (gatt != null) {
                gatt.connect();
            } else {
                this.gatt = bluetoothDevice.connectGatt(stack.applicationContext, false, gattDispatcher);
            }
        });
    }

    @NonNull
    @Override
    public Observable<Peripheral> disconnect() {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            return Observable.error(new PeripheralConnectionError());
        }

        Logger.info(LOG_TAG, "Disconnecting " + toString());

        return stack.newConfiguredObservable(s -> {
            gattDispatcher.addConnectionStateListener((gatt, gattStatus, newState, removeThisListener) -> {
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (gattStatus != BluetoothGatt.GATT_SUCCESS) {
                        Logger.info(LOG_TAG, "Could not disconnect " + toString() + "; " + BluetoothGattError.statusToString(gattStatus));

                        s.onError(new BluetoothGattError(gattStatus));
                    } else {
                        Logger.info(LOG_TAG, "Disconnected " + toString());

                        s.onNext(this);
                        s.onCompleted();
                    }

                    this.cachedPeripheralServices = null;

                    removeThisListener.call();
                }
            });

            if (getBondStatus() == BluetoothDevice.BOND_BONDED) {
                removeBond().subscribe(ignored -> {}, s::onError);
            } else {
                gatt.disconnect();
            }
        });
    }

    @Override
    public int getConnectionStatus() {
        return stack.bluetoothManager.getConnectionState(bluetoothDevice, BluetoothProfile.GATT);
    }

    //endregion


    //region Internal

    private <T> void setupTimeout(@NonNull OperationTimeoutError.Operation operation,
                                  @NonNull OperationTimeout timeout,
                                  @NonNull Subscriber<T> subscriber) {
        timeout.setTimeoutAction(() -> {
            switch (operation) {
                case DISCOVER_SERVICES:
                    gattDispatcher.onServicesDiscovered = null;
                    break;

                case SUBSCRIBE_NOTIFICATION:
                case UNSUBSCRIBE_NOTIFICATION:
                    gattDispatcher.onDescriptorWrite = null;
                    break;

                case WRITE_COMMAND:
                    gattDispatcher.onCharacteristicWrite = null;
                    break;
            }

            disconnect().subscribe(ignored -> subscriber.onError(new OperationTimeoutError(operation)),
                    e -> subscriber.onError(new OperationTimeoutError(operation, e)));
        }, stack.scheduler);
    }

    //endregion


    //region Bonding

    private Observable<Intent> createBondReceiver() {
        return fromBroadcast(stack.applicationContext, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                .subscribeOn(stack.scheduler)
                .filter(intent -> {
                    BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    return (bondedDevice != null && bondedDevice.getAddress().equals(bluetoothDevice.getAddress()));
                });
    }

    private boolean tryCreateBond() {
        try {
            Method method = bluetoothDevice.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(bluetoothDevice, (Object[]) null);
            return true;
        } catch (Exception e) {
            Logger.error(LOG_TAG, "Could not invoke `createBond` on native BluetoothDevice.", e);
            return false;
        }
    }

    private boolean tryRemoveBond() {
        try {
            Method method = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(bluetoothDevice, (Object[]) null);
            return true;
        } catch (Exception e) {
            Logger.error(LOG_TAG, "Could not invoke `removeBond` on native BluetoothDevice.", e);
            return false;
        }
    }

    @NonNull
    @Override
    public Observable<Peripheral> createBond() {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            Logger.error(LOG_TAG, "Cannot create bond without device being connected.");

            return Observable.error(new PeripheralConnectionError());
        }

        if (getBondStatus() == BOND_BONDED) {
            Logger.info(Peripheral.LOG_TAG, "Device already bonded, skipping.");

            return Observable.just(this);
        }

        return stack.newConfiguredObservable(s -> {
            Subscription subscription = createBondReceiver().subscribe(new Subscriber<Intent>() {
                @Override
                public void onCompleted() {}

                @Override
                public void onError(Throwable e) {
                    s.onError(e);
                }

                @Override
                public void onNext(Intent intent) {
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                    Logger.info(Peripheral.LOG_TAG, "Bond status changed from " + PeripheralBondAlterationError.getBondStateString(previousState) + " to " + PeripheralBondAlterationError.getBondStateString(state));

                    if (state == BluetoothDevice.BOND_BONDED) {
                        Logger.info(LOG_TAG, "Bonding succeeded.");

                        s.onNext(AndroidPeripheral.this);
                        s.onCompleted();

                        unsubscribe();
                    } else if (state == BluetoothDevice.ERROR || state == BOND_NONE && previousState == BOND_BONDING) {
                        int reason = intent.getIntExtra(PeripheralBondAlterationError.EXTRA_REASON, PeripheralBondAlterationError.REASON_UNKNOWN_FAILURE);
                        Logger.error(LOG_TAG, "Bonding failed for reason " + PeripheralBondAlterationError.getReasonString(reason));
                        s.onError(new PeripheralBondAlterationError(reason));

                        unsubscribe();
                    }
                }
            });

            if (!tryCreateBond()) {
                Logger.error(LOG_TAG, "createBond failed.");

                subscription.unsubscribe();
                s.onError(new PeripheralBondAlterationError(PeripheralBondAlterationError.REASON_ANDROID_API_CHANGED));
            }
        });
    }

    @NonNull
    @Override
    public Observable<Peripheral> removeBond() {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            return Observable.error(new PeripheralConnectionError());
        }

        if (getBondStatus() == BOND_NONE) {
            return Observable.just(this);
        }

        return stack.newConfiguredObservable(s -> {
            if (tryRemoveBond()) {
                createBondReceiver().subscribe(new Subscriber<Intent>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }

                    @Override
                    public void onNext(Intent intent) {
                        int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                        int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                        Logger.info(Peripheral.LOG_TAG, "Bond status changed from " + PeripheralBondAlterationError.getBondStateString(previousState) + " to " + PeripheralBondAlterationError.getBondStateString(state));

                        if (state == BOND_NONE) {
                            Logger.info(LOG_TAG, "Unbonding succeeded.");

                            s.onNext(AndroidPeripheral.this);
                            s.onCompleted();

                            unsubscribe();
                        } else if (state == BluetoothDevice.ERROR) {
                            int reason = intent.getIntExtra(PeripheralBondAlterationError.EXTRA_REASON, PeripheralBondAlterationError.REASON_UNKNOWN_FAILURE);
                            Logger.error(LOG_TAG, "Unbonding failed for reason " + PeripheralBondAlterationError.getReasonString(reason));
                            s.onError(new PeripheralBondAlterationError(reason));

                            unsubscribe();
                        }
                    }
                });
            } else {
                Logger.error(LOG_TAG, "removeBond failed.");

                s.onError(new PeripheralBondAlterationError(PeripheralBondAlterationError.REASON_ANDROID_API_CHANGED));
            }
        });
    }

    @Override
    public int getBondStatus() {
        return bluetoothDevice.getBondState();
    }

    //endregion


    //region Discovering Services

    @NonNull
    @Override
    public Observable<Collection<PeripheralService>> discoverServices(@NonNull @TakesOwnership OperationTimeout timeout) {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            timeout.recycle();
            return Observable.error(new PeripheralConnectionError());
        }

        return stack.newConfiguredObservable(s -> {
            setupTimeout(OperationTimeoutError.Operation.DISCOVER_SERVICES, timeout, s);

            gattDispatcher.onServicesDiscovered = (gatt, status) -> {
                timeout.unschedule();
                timeout.recycle();

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    this.cachedPeripheralServices = AndroidPeripheralService.wrapGattServices(gatt.getServices());

                    s.onNext(cachedPeripheralServices.values());
                    s.onCompleted();

                    gattDispatcher.onServicesDiscovered = null;
                } else {
                    Logger.error(LOG_TAG, "Could not discover services. " + BluetoothGattError.statusToString(status));

                    this.cachedPeripheralServices = null;
                    s.onError(new BluetoothGattError(status));

                    gattDispatcher.onServicesDiscovered = null;
                }
            };

            if (gatt.discoverServices()) {
                timeout.schedule();
            } else {
                timeout.recycle();
                s.onError(new PeripheralServiceDiscoveryFailedError());
            }
        });
    }

    @Nullable
    @Override
    public PeripheralService getService(@NonNull UUID serviceIdentifier) {
        if (gatt != null) {
            return cachedPeripheralServices.get(serviceIdentifier);
        } else {
            return null;
        }
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
                                                  @NonNull @TakesOwnership OperationTimeout timeout) {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            timeout.recycle();
            return Observable.error(new PeripheralConnectionError());
        }

        return stack.newConfiguredObservable(s -> {
            setupTimeout(OperationTimeoutError.Operation.SUBSCRIBE_NOTIFICATION, timeout, s);

            BluetoothGattService service = getGattService(onPeripheralService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicIdentifier);
            if (gatt.setCharacteristicNotification(characteristic, true)) {
                BluetoothGattDescriptor descriptorToWrite = characteristic.getDescriptor(descriptorIdentifier);
                descriptorToWrite.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gattDispatcher.onDescriptorWrite = (gatt, descriptor, status) -> {
                    if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE))
                        return;

                    timeout.unschedule();
                    timeout.recycle();

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        s.onNext(characteristicIdentifier);
                        s.onCompleted();
                    } else {
                        Logger.error(LOG_TAG, "Could not subscribe to characteristic. " + BluetoothGattError.statusToString(status));
                        s.onError(new BluetoothGattError(status));
                    }

                    gattDispatcher.onDescriptorWrite = null;
                };
                if (gatt.writeDescriptor(descriptorToWrite)) {
                    timeout.schedule();
                } else {
                    timeout.recycle();
                    s.onError(new BluetoothGattError(BluetoothGatt.GATT_FAILURE));
                }
            } else {
                timeout.recycle();
                s.onError(new BluetoothGattError(BluetoothGatt.GATT_WRITE_NOT_PERMITTED));
            }
        });
    }

    @NonNull
    @Override
    public Observable<UUID> unsubscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                    @NonNull UUID characteristicIdentifier,
                                                    @NonNull UUID descriptorIdentifier,
                                                    @NonNull @TakesOwnership OperationTimeout timeout) {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            timeout.recycle();
            return Observable.error(new PeripheralConnectionError());
        }

        return stack.newConfiguredObservable(s -> {
            setupTimeout(OperationTimeoutError.Operation.UNSUBSCRIBE_NOTIFICATION, timeout, s);

            BluetoothGattService service = getGattService(onPeripheralService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicIdentifier);
            gattDispatcher.onDescriptorWrite = (gatt, descriptor, status) -> {
                if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE))
                    return;

                timeout.unschedule();
                timeout.recycle();

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (gatt.setCharacteristicNotification(characteristic, false)) {
                        s.onNext(characteristicIdentifier);
                        s.onCompleted();
                    } else {
                        Logger.error(LOG_TAG, "Could not unsubscribe from characteristic. " + BluetoothGattError.statusToString(status));
                        s.onError(new BluetoothGattError(BluetoothGatt.GATT_FAILURE));
                    }
                } else {
                    s.onError(new BluetoothGattError(status));
                }

                gattDispatcher.onDescriptorWrite = null;
            };
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorIdentifier);
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            if (gatt.writeDescriptor(descriptor)) {
                timeout.schedule();
            } else {
                timeout.recycle();
                s.onError(new BluetoothGattError(BluetoothGatt.GATT_WRITE_NOT_PERMITTED));
            }
        });
    }

    @NonNull
    @Override
    public Observable<Void> writeCommand(@NonNull PeripheralService onPeripheralService,
                                         @NonNull UUID identifier,
                                         @NonNull byte[] payload,
                                         @NonNull @TakesOwnership OperationTimeout timeout) {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            timeout.recycle();
            return Observable.error(new PeripheralConnectionError());
        }

        return stack.newConfiguredObservable(s -> {
            setupTimeout(OperationTimeoutError.Operation.WRITE_COMMAND, timeout, s);

            gattDispatcher.onCharacteristicWrite = (gatt, characteristic, status) -> {
                timeout.unschedule();
                timeout.recycle();

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Logger.error(LOG_TAG, "Could not write command " + identifier + ", " + BluetoothGattError.statusToString(status));
                    s.onError(new BluetoothGattError(status));
                } else {
                    s.onNext(null);
                    s.onCompleted();
                }
            };

            BluetoothGattService service = getGattService(onPeripheralService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(identifier);
            characteristic.setValue(payload);
            if (gatt.writeCharacteristic(characteristic)) {
                timeout.schedule();
            } else {
                timeout.recycle();
                s.onError(new BluetoothGattError(BluetoothGatt.GATT_WRITE_NOT_PERMITTED));
            }
        });
    }

    @Override
    public void setPacketHandler(@Nullable PacketHandler dataHandler) {
        gattDispatcher.packetHandler = dataHandler;
    }

    @Nullable
    @Override
    public PacketHandler getPacketHandler() {
        return gattDispatcher.packetHandler;
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
