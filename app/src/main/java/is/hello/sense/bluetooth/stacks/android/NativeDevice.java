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
import java.util.List;
import java.util.UUID;

import is.hello.sense.bluetooth.errors.BluetoothException;
import is.hello.sense.bluetooth.errors.BondingException;
import is.hello.sense.bluetooth.errors.DiscoveryFailedException;
import is.hello.sense.bluetooth.errors.GattException;
import is.hello.sense.bluetooth.errors.NotConnectedException;
import is.hello.sense.bluetooth.errors.SubscriptionFailedException;
import is.hello.sense.bluetooth.stacks.Command;
import is.hello.sense.bluetooth.stacks.Device;
import is.hello.sense.bluetooth.stacks.DeviceCenter;
import is.hello.sense.bluetooth.stacks.Service;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Subscription;

import static rx.android.observables.AndroidObservable.fromBroadcast;

public class NativeDevice implements Device {
    private final @NonNull NativeDeviceCenter deviceCenter;
    private final @NonNull BluetoothDevice bluetoothDevice;
    private final int scannedRssi;
    private final NativeGattDispatcher gattDispatcher = new NativeGattDispatcher();

    private BluetoothGatt gatt;
    private List<Service> cachedServices;

    NativeDevice(@NonNull NativeDeviceCenter deviceCenter,
                 @NonNull BluetoothDevice bluetoothDevice,
                 int scannedRssi) {
        this.deviceCenter = deviceCenter;
        this.bluetoothDevice = bluetoothDevice;
        this.scannedRssi = scannedRssi;
    }


    //region Introspection

    @Override
    public int getScannedRssi() {
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
    public DeviceCenter getDeviceCenter() {
        return deviceCenter;
    }

    //endregion


    //region Connectivity

    @NonNull
    @Override
    public Observable<Device> connect() {
        if (this.gatt != null) {
            Logger.warn(LOG_TAG, "Redundant call to connect(), ignoring.");

            return Observable.just(this);
        }

        return deviceCenter.newConfiguredObservable(s -> {
            gattDispatcher.onConnectionStateChanged = (gatt, connectStatus, newState) -> {
                if (connectStatus != BluetoothGatt.GATT_SUCCESS) {
                    Logger.error(LOG_TAG, "Could not connect. " + GattException.getNameForStatus(connectStatus));
                    s.onError(new GattException(connectStatus));

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
    public Observable<Device> disconnect() {
        if (getConnectionStatus() != STATUS_CONNECTED)
            return Observable.error(new NotConnectedException());

        return deviceCenter.newConfiguredObservable(s -> {
            gattDispatcher.onConnectionStateChanged = (gatt, connectStatus, newState) -> {
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (connectStatus != BluetoothGatt.GATT_SUCCESS) {
                        Logger.info(LOG_TAG, "Could not disconnect " + toString() + "; " + GattException.getNameForStatus(connectStatus));

                        s.onError(new GattException(connectStatus));
                    } else {
                        Logger.info(LOG_TAG, "Disconnected " + toString());

                        gatt.close(); // This call is not safe unless the device is disconnected.
                        this.gatt = null;

                        s.onNext(this);
                        s.onCompleted();
                    }

                    this.cachedServices = null;
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

    private boolean createBond() {
        try {
            Method method = bluetoothDevice.getClass().getMethod("createBond", (Class[]) null);
            return (Boolean) method.invoke(bluetoothDevice, (Class[]) null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean removeBond() {
        try {
            Method method = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
            return (Boolean) method.invoke(bluetoothDevice, (Class[]) null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @NonNull
    @Override
    public Observable<Device> bond() {
        if (getBondStatus() == BOND_BONDED) {
            return Observable.just(this);
        }

        return deviceCenter.newConfiguredObservable(s -> {
            Subscription subscription = createBondReceiver().subscribe(intent -> {
                Logger.info(Device.LOG_TAG, "Bond status change " + intent);

                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDED) {
                    s.onNext(this);
                    s.onCompleted();
                } else if (state == BluetoothDevice.ERROR) {
                    s.onError(new BondingException());
                }
            }, s::onError);

            if (!createBond()) {
                subscription.unsubscribe();
                s.onError(new BondingException());
            }
        });
    }

    @NonNull
    @Override
    public Observable<Device> unbond() {
        if (getBondStatus() == BOND_NONE) {
            return Observable.just(this);
        }

        return deviceCenter.newConfiguredObservable(s -> {
            if (removeBond()) {
                createBondReceiver().subscribe(intent -> {
                    Logger.info(Device.LOG_TAG, "Bond status change " + intent);
                    BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (!bondedDevice.getAddress().equals(bluetoothDevice.getAddress()))
                        return;

                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    if (state == BluetoothDevice.BOND_NONE) {
                        s.onNext(this);
                        s.onCompleted();
                    } else if (state == BluetoothDevice.ERROR) {
                        s.onError(new BondingException());
                    }
                }, s::onError);
            } else {
                s.onError(new BondingException());
            }
        });
    }

    public Observable<Device> rebond() {
        return deviceCenter.newConfiguredObservable(s -> unbond().subscribe(ignored -> bond().subscribe(s), s::onError));
    }

    @Override
    public int getBondStatus() {
        return bluetoothDevice.getBondState();
    }

    //endregion


    //region Discovering Services

    @NonNull
    @Override
    public Observable<List<Service>> discoverServices() {
        if (getConnectionStatus() != STATUS_CONNECTED)
            return Observable.error(new NotConnectedException());

        return deviceCenter.newConfiguredObservable(s -> {
            gattDispatcher.onServicesDiscovered = (gatt, status) -> {
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                    Logger.info(LOG_TAG, "Insufficient authorization returned, waiting for implicit re-bond.");
                    createBondReceiver().subscribe(intent -> {
                        Logger.info(LOG_TAG, "Implicit re-bond completed, retrying services discovery.");
                        if (!gatt.discoverServices()) {
                            s.onError(new DiscoveryFailedException());
                        }
                    }, e -> {
                        this.cachedServices = null;
                        s.onError(new GattException(status));
                        gattDispatcher.onServicesDiscovered = null;
                    });
                } else if (status == BluetoothGatt.GATT_SUCCESS) {
                    this.cachedServices = NativeService.wrapNativeServices(gatt.getServices());

                    s.onNext(cachedServices);
                    s.onCompleted();

                    gattDispatcher.onServicesDiscovered = null;
                } else {
                    Logger.error(LOG_TAG, "Could not discover services. " + GattException.getNameForStatus(status));

                    this.cachedServices = null;
                    s.onError(new GattException(status));

                    gattDispatcher.onServicesDiscovered = null;
                }
            };

            if (!gatt.discoverServices()) {
                s.onError(new DiscoveryFailedException());
            }
        });
    }

    @Nullable
    @Override
    public Service getService(@NonNull UUID serviceIdentifier) {
        if (gatt != null) {
            BluetoothGattService nativeService = gatt.getService(serviceIdentifier);
            if (nativeService != null) {
                return new NativeService(nativeService);
            }
        }

        return null;
    }


    //endregion


    //region Characteristics

    private @NonNull BluetoothGattService getGattService(@NonNull Service onService) {
        return ((NativeService) onService).service;
    }

    @NonNull
    @Override
    public Observable<UUID> subscribeNotification(@NonNull Service onService,
                                                  @NonNull UUID characteristicIdentifier,
                                                  @NonNull UUID descriptorIdentifier) {
        if (getConnectionStatus() != STATUS_CONNECTED)
            return Observable.error(new NotConnectedException());

        return deviceCenter.newConfiguredObservable(s -> {
            BluetoothGattService service = getGattService(onService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicIdentifier);
            if (gatt.setCharacteristicNotification(characteristic, true)) {
                gattDispatcher.onDescriptorWrite = (gatt, descriptor, status) -> {
                    if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE))
                        return;

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        s.onNext(characteristicIdentifier);
                        s.onCompleted();
                    } else {
                        Logger.error(LOG_TAG, "Could not subscribe to characteristic. " + GattException.getNameForStatus(status));
                        s.onError(new GattException(status));
                    }

                    gattDispatcher.onDescriptorWrite = null;
                };
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorIdentifier);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(descriptor)) {
                    s.onError(new SubscriptionFailedException());
                }
            } else {
                s.onError(new SubscriptionFailedException());
            }
        });
    }

    @NonNull
    @Override
    public Observable<UUID> unsubscribeNotification(@NonNull Service onService,
                                                    @NonNull UUID characteristicIdentifier,
                                                    @NonNull UUID descriptorIdentifier) {
        if (getConnectionStatus() != STATUS_CONNECTED)
            return Observable.error(new NotConnectedException());

        return deviceCenter.newConfiguredObservable(s -> {
            BluetoothGattService service = getGattService(onService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicIdentifier);
            gattDispatcher.onDescriptorWrite = (gatt, descriptor, status) -> {
                if (!Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE))
                    return;

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (gatt.setCharacteristicNotification(characteristic, false)) {
                        s.onNext(characteristicIdentifier);
                        s.onCompleted();
                    } else {
                        Logger.error(LOG_TAG, "Could not unsubscribe from characteristic. " + GattException.getNameForStatus(status));
                        s.onError(new SubscriptionFailedException());
                    }
                } else {
                    s.onError(new GattException(status));
                }

                gattDispatcher.onDescriptorWrite = null;
            };
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorIdentifier);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (!gatt.writeDescriptor(descriptor)) {
                s.onError(new SubscriptionFailedException());
            }
        });
    }

    @NonNull
    @Override
    public Observable<Void> writeCommand(@NonNull Service onService, @NonNull Command command) {
        if (getConnectionStatus() != STATUS_CONNECTED)
            return Observable.error(new NotConnectedException());

        return deviceCenter.newConfiguredObservable(s -> {
            gattDispatcher.onCharacteristicWrite = (gatt, characteristic, status) -> {
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                    if (getBondStatus() == BOND_NONE) {
                        Logger.info(LOG_TAG, "Command write failed, trying implicit re-bond.");
                        createBondReceiver().subscribe(intent -> {
                            Logger.info(LOG_TAG, "Implicit re-bond completed, retrying command write.");
                            writeCommand(onService, command).subscribe(s);
                        }, e -> {
                            Logger.error(LOG_TAG, "Could not perform implicit re-bond. Propagating original error " + GattException.getNameForStatus(status));
                            s.onError(new GattException(status));
                        });
                    } else {
                        Logger.info(LOG_TAG, "Command write failed, trying explicit re-bond.");
                        rebond().subscribe(ignored -> {
                            Logger.info(LOG_TAG, "Explicit re-bond completed, retrying command write.");
                            writeCommand(onService, command).subscribe(s);
                        }, e -> {
                            Logger.error(LOG_TAG, "Could not perform explicit re-bond. Propagating original error " + GattException.getNameForStatus(status));
                            s.onError(new GattException(status));
                        });
                    }
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    Logger.error(LOG_TAG, "Could not write command " + command + ", " + GattException.getNameForStatus(status));
                    s.onError(new GattException(status));
                } else {
                    s.onNext(null);
                    s.onCompleted();
                }
            };

            BluetoothGattService service = getGattService(onService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(command.identifier);
            characteristic.setValue(command.payload);
            if (!gatt.writeCharacteristic(characteristic)) {
                s.onError(new BluetoothException());
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
                ", scannedRssi=" + getScannedRssi() +
                '}';
    }
}
