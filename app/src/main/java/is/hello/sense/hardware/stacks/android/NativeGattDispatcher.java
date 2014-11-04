package is.hello.sense.hardware.stacks.android;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.Nullable;

import com.hello.ble.stack.transmission.BlePacketHandler;

import is.hello.sense.hardware.Device;
import is.hello.sense.util.Logger;
import rx.functions.Action2;
import rx.functions.Action3;

public class NativeGattDispatcher extends BluetoothGattCallback {
    public @Nullable BlePacketHandler packetHandler;
    public @Nullable Action3<BluetoothGatt, Integer, Integer> onConnectionStateChanged;
    public @Nullable Action2<BluetoothGatt, Integer> onServicesDiscovered;
    public @Nullable Action3<BluetoothGatt, BluetoothGattCharacteristic, Integer> onCharacteristicWrite;
    public @Nullable Action3<BluetoothGatt, BluetoothGattDescriptor, Integer> onDescriptorWrite;

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        Logger.info(Device.LOG_TAG, "onConnectionStateChange('" + gatt + "', " + status + ", " + newState + ")");

        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
            Logger.info(Device.LOG_TAG, "Closing gatt layer");
            gatt.close();
        }

        if (onConnectionStateChanged != null)
            onConnectionStateChanged.call(gatt, status, newState);
        else
            Logger.warn(Device.LOG_TAG, "unhandled call to onConnectionStateChange");
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Logger.info(Device.LOG_TAG, "onServicesDiscovered('" + gatt + "', " + status + ")");

        if (onServicesDiscovered != null)
            onServicesDiscovered.call(gatt, status);
        else
            Logger.warn(Device.LOG_TAG, "unhandled call to onServicesDiscovered");
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        Logger.info(Device.LOG_TAG, "onCharacteristicRead('" + gatt + "', " + characteristic + ", " + status + ")");

        if (packetHandler != null) {
            packetHandler.dispatch(characteristic.getUuid(), characteristic.getValue());
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        Logger.info(Device.LOG_TAG, "onCharacteristicWrite('" + gatt + "', " + characteristic + ", " + status + ")");

        if (onCharacteristicWrite != null)
            onCharacteristicWrite.call(gatt, characteristic, status);
        else
            Logger.warn(Device.LOG_TAG, "unhandled call to onCharacteristicWrite");
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        Logger.info(Device.LOG_TAG, "onCharacteristicChanged('" + gatt + "', " + characteristic + ", " + ")");

        if (packetHandler != null) {
            packetHandler.dispatch(characteristic.getUuid(), characteristic.getValue());
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        Logger.info(Device.LOG_TAG, "onDescriptorWrite('" + gatt + "', " + descriptor + ", " + ")");

        if (onDescriptorWrite != null)
            onDescriptorWrite.call(gatt, descriptor, status);
        else
            Logger.warn(Device.LOG_TAG, "unhandled call to onDescriptorWrite");
    }
}
