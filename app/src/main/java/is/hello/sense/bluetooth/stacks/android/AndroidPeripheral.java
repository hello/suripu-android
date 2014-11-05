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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import is.hello.sense.bluetooth.errors.BluetoothError;
import is.hello.sense.bluetooth.errors.BondingError;
import is.hello.sense.bluetooth.errors.PeripheralConnectionError;
import is.hello.sense.bluetooth.errors.PeripheralServiceDiscoveryFailedError;
import is.hello.sense.bluetooth.errors.GattError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.PeripheralService;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscription;

import static rx.android.observables.AndroidObservable.fromBroadcast;

public class AndroidPeripheral implements Peripheral {
    private final @NonNull AndroidBluetoothStack deviceCenter;
    private final @NonNull BluetoothDevice bluetoothDevice;
    private final int scannedRssi;
    private final GattDispatcher gattDispatcher = new GattDispatcher();

    private BluetoothGatt gatt;
    private Map<UUID, PeripheralService> cachedPeripheralServices;

    AndroidPeripheral(@NonNull AndroidBluetoothStack deviceCenter,
                      @NonNull BluetoothDevice bluetoothDevice,
                      int scannedRssi) {
        this.deviceCenter = deviceCenter;
        this.bluetoothDevice = bluetoothDevice;
        this.scannedRssi = scannedRssi;
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
        return deviceCenter;
    }

    //endregion


    //region Connectivity

    @NonNull
    @Override
    public Observable<Peripheral> connect() {
        if (this.gatt != null) {
            Logger.warn(LOG_TAG, "Redundant call to connect(), ignoring.");

            if (getConnectionStatus() == STATUS_CONNECTED) {
                return Observable.just(this);
            } else if (getConnectionStatus() == STATUS_CONNECTING || getConnectionStatus() == STATUS_DISCONNECTING) {
                return Observable.error(new PeripheralConnectionError("Peripheral is changing conection status."));
            }
        }

        return deviceCenter.newConfiguredObservable(s -> {
            gattDispatcher.onConnectionStateChanged = (gatt, connectStatus, newState) -> {
                if (connectStatus != BluetoothGatt.GATT_SUCCESS) {
                    Logger.error(LOG_TAG, "Could not connect. " + GattError.statusToString(connectStatus));
                    s.onError(new GattError(connectStatus));

                    if (newState == BluetoothAdapter.STATE_DISCONNECTED) {
                        gatt.disconnect();
                        this.gatt = null;
                    }

                    gattDispatcher.onConnectionStateChanged = null;
                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Logger.info(LOG_TAG, "Connected " + toString());

                    s.onNext(this);
                    s.onCompleted();

                    gattDispatcher.onConnectionStateChanged = null;
                }
            };
            this.gatt = bluetoothDevice.connectGatt(deviceCenter.applicationContext, false, gattDispatcher);
        });
    }

    @NonNull
    @Override
    public Observable<Peripheral> disconnect() {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            return Observable.error(new PeripheralConnectionError());
        }

        return deviceCenter.newConfiguredObservable(s -> {
            gattDispatcher.onConnectionStateChanged = (gatt, connectStatus, newState) -> {
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (connectStatus != BluetoothGatt.GATT_SUCCESS) {
                        Logger.info(LOG_TAG, "Could not disconnect " + toString() + "; " + GattError.statusToString(connectStatus));

                        s.onError(new GattError(connectStatus));
                    } else {
                        Logger.info(LOG_TAG, "Disconnected " + toString());

                        gatt.close(); // This call is not safe unless the device is disconnected.
                        this.gatt = null;

                        s.onNext(this);
                        s.onCompleted();
                    }

                    this.cachedPeripheralServices = null;
                }
            };
            gatt.disconnect();
        });
    }

    @Override
    public int getConnectionStatus() {
        return deviceCenter.bluetoothManager.getConnectionState(bluetoothDevice, BluetoothProfile.GATT);
    }

    //endregion


    //region Bonding

    private Observable<Intent> createBondReceiver() {
        return fromBroadcast(deviceCenter.applicationContext, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                .subscribeOn(deviceCenter.scheduler)
                .filter(intent -> {
                    BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    return (bondedDevice != null && bondedDevice.getAddress().equals(bluetoothDevice.getAddress()));
                })
                .take(2);
    }

    private boolean tryCreateBond() {
        try {
            Method method = bluetoothDevice.getClass().getMethod("createBond", (Class[]) null);
            return (Boolean) method.invoke(bluetoothDevice, (Object[]) null);
        } catch (Exception e) {
            Logger.error(LOG_TAG, "Could not invoke `createBond` on native BluetoothDevice.", e);
            return false;
        }
    }

    private boolean tryRemoveBond() {
        try {
            Method method = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
            return (Boolean) method.invoke(bluetoothDevice, (Object[]) null);
        } catch (Exception e) {
            Logger.error(LOG_TAG, "Could not invoke `removeBond` on native BluetoothDevice.", e);
            return false;
        }
    }

    @NonNull
    @Override
    public Observable<Peripheral> createBond() {
        if (getConnectionStatus() != STATUS_CONNECTED) {
            return Observable.error(new PeripheralConnectionError());
        }

        if (getBondStatus() == BOND_BONDED) {
            return Observable.just(this);
        }

        return deviceCenter.newConfiguredObservable(s -> {
            Subscription subscription = createBondReceiver().subscribe(intent -> {
                Logger.info(Peripheral.LOG_TAG, "Bond status change " + intent);

                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDED) {
                    s.onNext(this);
                    s.onCompleted();
                } else if (state == BluetoothDevice.ERROR) {
                    s.onError(new BondingError());
                }
            }, s::onError);

            if (!tryCreateBond()) {
                subscription.unsubscribe();
                s.onError(new BondingError());
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

        return deviceCenter.newConfiguredObservable(s -> {
            if (tryRemoveBond()) {
                createBondReceiver().subscribe(intent -> {
                    Logger.info(Peripheral.LOG_TAG, "Bond status change " + intent);
                    BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (!bondedDevice.getAddress().equals(bluetoothDevice.getAddress()))
                        return;

                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    if (state == BluetoothDevice.BOND_NONE) {
                        s.onNext(this);
                        s.onCompleted();
                    } else if (state == BluetoothDevice.ERROR) {
                        s.onError(new BondingError());
                    }
                }, s::onError);
            } else {
                s.onError(new BondingError());
            }
        });
    }

    public Observable<Peripheral> recreateBond() {
        return deviceCenter.newConfiguredObservable(s -> removeBond().subscribe(ignored -> createBond().subscribe(s), s::onError));
    }

    @Override
    public int getBondStatus() {
        return bluetoothDevice.getBondState();
    }

    //endregion


    //region Discovering Services

    @NonNull
    @Override
    public Observable<Collection<PeripheralService>> discoverServices() {
        if (getConnectionStatus() != STATUS_CONNECTED)
            return Observable.error(new PeripheralConnectionError());

        return deviceCenter.newConfiguredObservable(s -> {
            gattDispatcher.onServicesDiscovered = (gatt, status) -> {
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                    Logger.info(LOG_TAG, "Insufficient authorization returned, waiting for implicit re-createBond.");
                    createBondReceiver().subscribe(intent -> {
                        Logger.info(LOG_TAG, "Implicit re-createBond completed, retrying services discovery.");
                        if (!gatt.discoverServices()) {
                            s.onError(new PeripheralServiceDiscoveryFailedError());
                        }
                    }, e -> {
                        this.cachedPeripheralServices = null;
                        s.onError(new GattError(status));
                        gattDispatcher.onServicesDiscovered = null;
                    });
                } else if (status == BluetoothGatt.GATT_SUCCESS) {
                    this.cachedPeripheralServices = AndroidPeripheralService.wrapNativeServices(gatt.getServices());

                    s.onNext(cachedPeripheralServices.values());
                    s.onCompleted();

                    gattDispatcher.onServicesDiscovered = null;
                } else {
                    Logger.error(LOG_TAG, "Could not discover services. " + GattError.statusToString(status));

                    this.cachedPeripheralServices = null;
                    s.onError(new GattError(status));

                    gattDispatcher.onServicesDiscovered = null;
                }
            };

            if (!gatt.discoverServices()) {
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
                                                  @NonNull UUID descriptorIdentifier) {
        if (getConnectionStatus() != STATUS_CONNECTED)
            return Observable.error(new PeripheralConnectionError());

        return deviceCenter.newConfiguredObservable(s -> {
            BluetoothGattService service = getGattService(onPeripheralService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicIdentifier);
            if (gatt.setCharacteristicNotification(characteristic, true)) {
                gattDispatcher.onDescriptorWrite = (gatt, descriptor, status) -> {
                    if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE))
                        return;

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        s.onNext(characteristicIdentifier);
                        s.onCompleted();
                    } else {
                        Logger.error(LOG_TAG, "Could not subscribe to characteristic. " + GattError.statusToString(status));
                        s.onError(new GattError(status));
                    }

                    gattDispatcher.onDescriptorWrite = null;
                };
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorIdentifier);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(descriptor)) {
                    s.onError(new GattError(BluetoothGatt.GATT_FAILURE));
                }
            } else {
                s.onError(new GattError(BluetoothGatt.GATT_FAILURE));
            }
        });
    }

    @NonNull
    @Override
    public Observable<UUID> unsubscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                    @NonNull UUID characteristicIdentifier,
                                                    @NonNull UUID descriptorIdentifier) {
        if (getConnectionStatus() != STATUS_CONNECTED)
            return Observable.error(new PeripheralConnectionError());

        return deviceCenter.newConfiguredObservable(s -> {
            BluetoothGattService service = getGattService(onPeripheralService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicIdentifier);
            gattDispatcher.onDescriptorWrite = (gatt, descriptor, status) -> {
                if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE))
                    return;

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (gatt.setCharacteristicNotification(characteristic, false)) {
                        s.onNext(characteristicIdentifier);
                        s.onCompleted();
                    } else {
                        Logger.error(LOG_TAG, "Could not unsubscribe from characteristic. " + GattError.statusToString(status));
                        s.onError(new GattError(BluetoothGatt.GATT_FAILURE));
                    }
                } else {
                    s.onError(new GattError(status));
                }

                gattDispatcher.onDescriptorWrite = null;
            };
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorIdentifier);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (!gatt.writeDescriptor(descriptor)) {
                s.onError(new GattError(BluetoothGatt.GATT_FAILURE));
            }
        });
    }

    @NonNull
    @Override
    public Observable<Void> writeCommand(@NonNull PeripheralService onPeripheralService, @NonNull UUID identifier, @NonNull byte[] payload) {
        if (getConnectionStatus() != STATUS_CONNECTED)
            return Observable.error(new PeripheralConnectionError());

        return deviceCenter.newConfiguredObservable(s -> {
            gattDispatcher.onCharacteristicWrite = (gatt, characteristic, status) -> {
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                    if (getBondStatus() == BOND_NONE) {
                        Logger.info(LOG_TAG, "Command write failed, trying implicit re-createBond.");
                        createBondReceiver().subscribe(intent -> {
                            Logger.info(LOG_TAG, "Implicit re-createBond completed, retrying command write.");
                            writeCommand(onPeripheralService, identifier, payload).subscribe(s);
                        }, e -> {
                            Logger.error(LOG_TAG, "Could not perform implicit re-createBond. Propagating original error " + GattError.statusToString(status));
                            s.onError(new GattError(status));
                        });
                    } else {
                        Logger.info(LOG_TAG, "Command write failed, trying explicit re-createBond.");
                        recreateBond().subscribe(ignored -> {
                            Logger.info(LOG_TAG, "Explicit re-createBond completed, retrying command write.");
                            writeCommand(onPeripheralService, identifier, payload).subscribe(s);
                        }, e -> {
                            Logger.error(LOG_TAG, "Could not perform explicit re-createBond. Propagating original error " + GattError.statusToString(status));
                            s.onError(new GattError(status));
                        });
                    }
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    Logger.error(LOG_TAG, "Could not write command " + identifier + ", " + GattError.statusToString(status));
                    s.onError(new GattError(status));
                } else {
                    s.onNext(null);
                    s.onCompleted();
                }
            };

            BluetoothGattService service = getGattService(onPeripheralService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(identifier);
            characteristic.setValue(payload);
            if (!gatt.writeCharacteristic(characteristic)) {
                s.onError(new BluetoothError());
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
        return "{NativeDevice " +
                "name=" + getName() +
                ", address=" + getAddress() +
                ", connectionStatus=" + getConnectionStatus() +
                ", bondStatus=" + getBondStatus() +
                ", scannedRssi=" + getScanTimeRssi() +
                '}';
    }
}
