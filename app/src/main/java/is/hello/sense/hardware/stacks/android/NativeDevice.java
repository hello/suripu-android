package is.hello.sense.hardware.stacks.android;

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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import is.hello.sense.hardware.Command;
import is.hello.sense.hardware.Device;
import is.hello.sense.hardware.Service;
import is.hello.sense.hardware.errors.BluetoothException;
import is.hello.sense.hardware.errors.BondingException;
import is.hello.sense.hardware.errors.DiscoveryFailedException;
import is.hello.sense.hardware.errors.NotConnectedException;
import is.hello.sense.hardware.errors.StatusException;
import is.hello.sense.hardware.errors.SubscriptionFailedException;
import rx.Observable;

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

    //endregion


    //region Connectivity

    @NonNull
    @Override
    public Observable<Device> connect(@NonNull UUID targetService) {
        return deviceCenter.newConfiguredObservable(s -> {
            gattDispatcher.onConnectionStateChanged = (gatt, connectStatus, newState) -> {
                if (connectStatus != BluetoothGatt.GATT_SUCCESS) {
                    s.onError(new StatusException(connectStatus));
                    gattDispatcher.onConnectionStateChanged = null;
                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
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
                    gatt.close();
                    this.gatt = null;
                    this.cachedServices = null;
                }
            };
            gatt.disconnect();
        });
    }

    @Override
    public int getConnectionStatus() {
        if (gatt != null)
            return gatt.getConnectionState(bluetoothDevice);
        else
            return STATUS_DISCONNECTED;
    }

    //endregion


    //region Bonding

    private Observable<Intent> createBondReceiver() {
        return fromBroadcast(deviceCenter.applicationContext, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)).subscribeOn(deviceCenter.scheduler).take(1);
    }

    private boolean createBond() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bluetoothDevice.createBond();
        } else {
            try {
                Method method = bluetoothDevice.getClass().getMethod("createBond", (Class[]) null);
                return (Boolean) method.invoke(bluetoothDevice, (Class[]) null);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
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
        return deviceCenter.newConfiguredObservable(s -> {
            if (createBond()) {
                createBondReceiver().subscribe(intent -> {
                    BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (!bondedDevice.getAddress().equals(bluetoothDevice.getAddress()))
                        return;

                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    if (state == BluetoothDevice.BOND_BONDED) {
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

    @NonNull
    @Override
    public Observable<Device> unbond() {
        return deviceCenter.newConfiguredObservable(s -> {
            if (removeBond()) {
                createBondReceiver().subscribe(intent -> {
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
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    this.cachedServices = NativeService.wrapNativeServices(gatt.getServices());
                    s.onNext(cachedServices);
                    s.onCompleted();
                } else {
                    this.cachedServices = null;
                    s.onError(new StatusException(status));
                }
                gattDispatcher.onServicesDiscovered = null;
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
                        s.onError(new StatusException(status));
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
                        s.onError(new SubscriptionFailedException());
                    }
                } else {
                    s.onError(new StatusException(status));
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
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    s.onError(new StatusException(status));
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

    @NonNull
    @Override
    public Observable<Command> incomingPackets() {
        if (getConnectionStatus() != STATUS_CONNECTED)
            return Observable.error(new NotConnectedException());

        return gattDispatcher.incomingSubject;
    }

    //endregion


    @Override
    public String toString() {
        return "{NativeDevice" +
                "name=" + getName() +
                "address=" + getAddress() +
                "connectionStatus" + getConnectionStatus() +
                "bondStatus" + getBondStatus() +
                "scannedRssi=" + getScannedRssi() +
                '}';
    }
}
