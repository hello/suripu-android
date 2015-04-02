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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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
import is.hello.sense.bluetooth.stacks.util.Util;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;

import static rx.android.observables.AndroidObservable.fromBroadcast;

public class AndroidPeripheral implements Peripheral {
    /**
     * How long to delay response after a successful service discovery
     * if {@link #CONFIG_WAIT_AFTER_SERVICE_DISCOVERY} is specified.
     * <p/>
     * Settled on 3 seconds after experimenting with Jackson. Idea for delay from
     * <a href="https://code.google.com/p/android/issues/detail?id=58381">here</a>.
     */
    private static final int SERVICES_DELAY_S = 3;

    private final @NonNull AndroidBluetoothStack stack;
    private final @NonNull BluetoothDevice bluetoothDevice;
    private final int scannedRssi;
    private final GattDispatcher gattDispatcher = new GattDispatcher();

    private BluetoothGatt gatt;
    private @Nullable Map<UUID, PeripheralService> cachedPeripheralServices;
    private @Nullable Subscription bluetoothStateSubscription;
    private @Config int config;

    AndroidPeripheral(@NonNull AndroidBluetoothStack stack,
                      @NonNull BluetoothDevice bluetoothDevice,
                      int scannedRssi) {
        this.stack = stack;
        this.bluetoothDevice = bluetoothDevice;
        this.scannedRssi = scannedRssi;
        this.config = stack.getDefaultConfig();

        gattDispatcher.addConnectionStateListener((gatt, gattStatus, newState, removeThisListener) -> {
            if (newState == STATUS_DISCONNECTED) {
                closeGatt(gatt);

                Intent disconnect = new Intent(ACTION_DISCONNECTED);
                disconnect.putExtra(EXTRA_NAME, getName());
                disconnect.putExtra(EXTRA_ADDRESS, getAddress());
                LocalBroadcastManager.getInstance(stack.applicationContext)
                                     .sendBroadcast(disconnect);
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

    @Override
    @NonNull
    public BluetoothStack getStack() {
        return stack;
    }

    @Override
    public void setConfig(@Config int newConfig) {
        this.config = newConfig;
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
                        s.onError(new BluetoothGattError(BluetoothGattError.GATT_INTERNAL_ERROR, BluetoothGattError.Operation.CONNECT));
                    }
                } else if (gattStatus != BluetoothGatt.GATT_SUCCESS) {
                    timeout.unschedule();

                    Logger.error(LOG_TAG, "Could not connect. " + BluetoothGattError.statusToString(gattStatus));
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

                    boolean clearBondOnConnect = ((config & CONFIG_FRAGILE_BONDS) == CONFIG_FRAGILE_BONDS);
                    if (getBondStatus() == BOND_NONE || !clearBondOnConnect) {
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
                            //noinspection ConstantConditions
                            if (gatt != null) {
                                timeout.schedule();
                            } else {
                                s.onError(new BluetoothGattError(BluetoothGattError.GATT_INTERNAL_ERROR, BluetoothGattError.Operation.CONNECT));
                            }
                        }, e -> {
                            Logger.warn(LOG_TAG, "Could not remove previously persisted bonding information, ignoring.", e);

                            s.onNext(this);
                            s.onCompleted();

                            removeThisListener.call();
                        });
                    }
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

                s.onError(new OperationTimeoutError(OperationTimeoutError.Operation.CONNECT));
            }, stack.scheduler);

            Logger.info(LOG_TAG, "Connecting " + toString());

            if (gatt != null) {
                if (gatt.connect()) {
                    timeout.schedule();
                } else {
                    s.onError(new BluetoothGattError(BluetoothGattError.GATT_INTERNAL_ERROR, BluetoothGattError.Operation.CONNECT));
                }
            } else {
                this.gatt = bluetoothDevice.connectGatt(stack.applicationContext, false, gattDispatcher);
                if (gatt != null) {
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
        int connectionStatus = getConnectionStatus();
        if (connectionStatus == STATUS_DISCONNECTED || connectionStatus == STATUS_DISCONNECTING) {
            return Observable.just(this);
        } else if (connectionStatus == STATUS_CONNECTING) {
            return Observable.error(new PeripheralConnectionError("Peripheral is connecting"));
        }

        Logger.info(LOG_TAG, "Disconnecting " + toString());

        return stack.newConfiguredObservable(s -> {
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

                    this.cachedPeripheralServices = null;

                    removeThisListener.call();
                }
            });

            boolean clearBondOnDisconnect = ((config & CONFIG_FRAGILE_BONDS) == CONFIG_FRAGILE_BONDS);
            if (clearBondOnDisconnect && getBondStatus() == BOND_BONDED) {
                removeBond().subscribe(ignored -> {}, s::onError);
            } else {
                gatt.disconnect();
            }
        });
    }

    @Override
    public @ConnectivityStatus int getConnectionStatus() {
        @ConnectivityStatus int status = stack.bluetoothManager.getConnectionState(bluetoothDevice, BluetoothProfile.GATT);
        return status;
    }

    //endregion


    //region Internal

    private boolean shouldAutoActivateCompatibilityShims() {
        return ((config & CONFIG_AUTO_ACTIVATE_COMPATIBILITY_SHIMS) == CONFIG_AUTO_ACTIVATE_COMPATIBILITY_SHIMS);
    }

    private void setWaitAfterServiceDiscovery() {
        boolean waitsAfterDiscovery = ((config & CONFIG_WAIT_AFTER_SERVICE_DISCOVERY) == CONFIG_WAIT_AFTER_SERVICE_DISCOVERY);
        if (!waitsAfterDiscovery) {
            Log.i(LOG_TAG, "Activating " + Util.peripheralConfigToString(CONFIG_WAIT_AFTER_SERVICE_DISCOVERY));
            config |= CONFIG_WAIT_AFTER_SERVICE_DISCOVERY;
        }
    }

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
                    if (shouldAutoActivateCompatibilityShims()) {
                        setWaitAfterServiceDiscovery();
                    }
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
    private Observable<Peripheral> removeBond() {
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
    public @BondStatus int getBondStatus() {
        @BondStatus int bondStatus = bluetoothDevice.getBondState();
        return bondStatus;
    }

    //endregion


    //region Discovering Services

    @NonNull
    @Override
    public Observable<Map<UUID, PeripheralService>> discoverServices(@NonNull OperationTimeout timeout) {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            return Observable.error(new PeripheralConnectionError());
        }

        Observable<Map<UUID, PeripheralService>> discoverServices = stack.newConfiguredObservable(s -> {
            Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(s, timeout);
            setupTimeout(OperationTimeoutError.Operation.DISCOVER_SERVICES, timeout, s, onDisconnect);

            gattDispatcher.onServicesDiscovered = (gatt, status) -> {
                timeout.unschedule();

                gattDispatcher.removeDisconnectListener(onDisconnect);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Map<UUID, PeripheralService> services = AndroidPeripheralService.wrapGattServices(gatt.getServices());
                    this.cachedPeripheralServices = services;

                    s.onNext(services);
                    s.onCompleted();

                    gattDispatcher.onServicesDiscovered = null;
                } else {
                    Logger.error(LOG_TAG, "Could not discover services. " + BluetoothGattError.statusToString(status));

                    this.cachedPeripheralServices = null;
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


        boolean waitAfterDiscovery = ((config & CONFIG_WAIT_AFTER_SERVICE_DISCOVERY) == CONFIG_WAIT_AFTER_SERVICE_DISCOVERY);
        if (waitAfterDiscovery) {
            // See <https://code.google.com/p/android/issues/detail?id=58381>
            return discoverServices.delay(SERVICES_DELAY_S, TimeUnit.SECONDS);
        } else {
            return discoverServices;
        }
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

    @Override
    public boolean hasDiscoveredServices() {
        return (gatt != null && cachedPeripheralServices != null);
    }

    @Nullable
    @Override
    public PeripheralService getService(@NonNull UUID serviceIdentifier) {
        if (gatt != null) {
            if (cachedPeripheralServices != null) {
                return cachedPeripheralServices.get(serviceIdentifier);
            } else {
                Logger.warn(LOG_TAG, "getService called before discoverServices.");
                return null;
            }
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
                                                  @NonNull OperationTimeout timeout) {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            return Observable.error(new PeripheralConnectionError());
        }

        return stack.newConfiguredObservable(s -> {
            BluetoothGattService service = getGattService(onPeripheralService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicIdentifier);
            if (gatt.setCharacteristicNotification(characteristic, true)) {
                Action0 onDisconnect = gattDispatcher.addTimeoutDisconnectListener(s, timeout);
                setupTimeout(OperationTimeoutError.Operation.SUBSCRIBE_NOTIFICATION, timeout, s, onDisconnect);

                gattDispatcher.onDescriptorWrite = (gatt, descriptor, status) -> {
                    if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                        return;
                    }

                    timeout.unschedule();

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        s.onNext(characteristicIdentifier);
                        s.onCompleted();
                    } else {
                        Logger.error(LOG_TAG, "Could not subscribe to characteristic. " + BluetoothGattError.statusToString(status));
                        if (shouldAutoActivateCompatibilityShims()) {
                            setWaitAfterServiceDiscovery();
                        }
                        s.onError(new BluetoothGattError(status, BluetoothGattError.Operation.SUBSCRIBE_NOTIFICATION));
                    }

                    gattDispatcher.onDescriptorWrite = null;
                    gattDispatcher.removeDisconnectListener(onDisconnect);
                };

                BluetoothGattDescriptor descriptorToWrite = characteristic.getDescriptor(descriptorIdentifier);
                descriptorToWrite.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (gatt.writeDescriptor(descriptorToWrite)) {
                    timeout.schedule();
                } else {
                    gattDispatcher.onDescriptorWrite = null;
                    gattDispatcher.removeDisconnectListener(onDisconnect);

                    if (shouldAutoActivateCompatibilityShims()) {
                        setWaitAfterServiceDiscovery();
                    }

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
        if (getConnectionStatus() != STATUS_CONNECTED) {
            return Observable.error(new PeripheralConnectionError());
        }

        return stack.newConfiguredObservable(s -> {
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
                                         @NonNull byte[] payload,
                                         @NonNull OperationTimeout timeout) {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            return Observable.error(new PeripheralConnectionError());
        }

        return stack.newConfiguredObservable(s -> {
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
