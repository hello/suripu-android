package is.hello.sense.bluetooth.stacks.android;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.util.Logger;
import rx.functions.Action0;
import rx.functions.Action2;
import rx.functions.Action3;

class GattDispatcher extends BluetoothGattCallback {
    private final List<ConnectionStateListener> connectionStateListeners = new ArrayList<>();
    private final AndroidPeripheral peripheral;

    public @Nullable PacketHandler packetHandler;
    public @Nullable Action2<BluetoothGatt, Integer> onServicesDiscovered;
    public @Nullable Action3<BluetoothGatt, BluetoothGattCharacteristic, Integer> onCharacteristicWrite;
    public @Nullable Action3<BluetoothGatt, BluetoothGattDescriptor, Integer> onDescriptorWrite;

    GattDispatcher(@NonNull AndroidPeripheral peripheral) {
        this.peripheral = peripheral;
    }

    void addConnectionStateListener(@NonNull ConnectionStateListener changeHandler) {
        connectionStateListeners.add(changeHandler);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        Logger.info(Peripheral.LOG_TAG, "onConnectionStateChange('" + gatt + "', " + status + ", " + newState + ")");

        if (connectionStateListeners.isEmpty()) {
            Logger.warn(Peripheral.LOG_TAG, "unhandled call to onConnectionStateChange");

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                peripheral.closeGatt();
            }
        } else {
            Iterator<ConnectionStateListener> iterator = connectionStateListeners.iterator();
            Action0 removeListener = iterator::remove;
            while (iterator.hasNext()) {
                ConnectionStateListener listener = iterator.next();
                listener.onConnectionStateChanged(gatt, status, newState, removeListener);
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Logger.info(Peripheral.LOG_TAG, "onServicesDiscovered('" + gatt + "', " + status + ")");

        if (onServicesDiscovered != null)
            onServicesDiscovered.call(gatt, status);
        else
            Logger.warn(Peripheral.LOG_TAG, "unhandled call to onServicesDiscovered");
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        Logger.info(Peripheral.LOG_TAG, "onCharacteristicRead('" + gatt + "', " + characteristic + ", " + status + ")");

        if (packetHandler != null) {
            packetHandler.process(characteristic.getUuid(), characteristic.getValue());
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        Logger.info(Peripheral.LOG_TAG, "onCharacteristicWrite('" + gatt + "', " + characteristic + ", " + status + ")");

        if (onCharacteristicWrite != null)
            onCharacteristicWrite.call(gatt, characteristic, status);
        else
            Logger.warn(Peripheral.LOG_TAG, "unhandled call to onCharacteristicWrite");
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        Logger.info(Peripheral.LOG_TAG, "onCharacteristicChanged('" + gatt + "', " + characteristic + ", " + ")");

        if (packetHandler != null) {
            packetHandler.process(characteristic.getUuid(), characteristic.getValue());
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        Logger.info(Peripheral.LOG_TAG, "onDescriptorWrite('" + gatt + "', " + descriptor + ", " + ")");

        if (onDescriptorWrite != null)
            onDescriptorWrite.call(gatt, descriptor, status);
        else
            Logger.warn(Peripheral.LOG_TAG, "unhandled call to onDescriptorWrite");
    }


    interface ConnectionStateListener {
        void onConnectionStateChanged(@NonNull BluetoothGatt gatt,
                                      int status,
                                      int newState,
                                      @NonNull Action0 removeListener);
    }
}
