package is.hello.sense.hardware.stacks.android;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.Nullable;

import is.hello.sense.util.Logger;
import rx.functions.Action2;
import rx.functions.Action3;

public class NativeGattDispatcher extends BluetoothGattCallback {
    private static final String TAG = NativeGattDispatcher.class.getSimpleName();

    public @Nullable Action3<BluetoothGatt, Integer, Integer> onConnectionStateChanged;
    public @Nullable Action2<BluetoothGatt, Integer> onServicesDiscovered;
    public @Nullable Action3<BluetoothGatt, BluetoothGattCharacteristic, Integer> onCharacteristicRead;
    public @Nullable Action3<BluetoothGatt, BluetoothGattCharacteristic, Integer> onCharacteristicWrite;
    public @Nullable Action3<BluetoothGatt, BluetoothGattDescriptor, Integer> onDescriptorWrite;
    public @Nullable Action2<BluetoothGatt, BluetoothGattCharacteristic> onCharacteristicChanged;

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        Logger.error(TAG, "onConnectionStateChange('" + gatt + "', " + status + ", " + newState + ")");

        if (onConnectionStateChanged != null)
            onConnectionStateChanged.call(gatt, status, newState);
        else
            Logger.warn(TAG, "unhandled call to onConnectionStateChange");
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Logger.error(TAG, "onServicesDiscovered('" + gatt + "', " + status + ")");

        if (onServicesDiscovered != null)
            onServicesDiscovered.call(gatt, status);
        else
            Logger.warn(TAG, "unhandled call to onServicesDiscovered");
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        Logger.error(TAG, "onCharacteristicRead('" + gatt + "', " + characteristic + ", " + status + ")");

        if (onCharacteristicRead != null)
            onCharacteristicRead.call(gatt, characteristic, status);
        else
            Logger.warn(TAG, "unhandled call to onCharacteristicRead");
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        Logger.error(TAG, "onCharacteristicWrite('" + gatt + "', " + characteristic + ", " + status + ")");

        if (onCharacteristicWrite != null)
            onCharacteristicWrite.call(gatt, characteristic, status);
        else
            Logger.warn(TAG, "unhandled call to onCharacteristicWrite");
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        Logger.error(TAG, "onCharacteristicChanged('" + gatt + "', " + characteristic + ", " + ")");

        if (onCharacteristicChanged != null)
            onCharacteristicChanged.call(gatt, characteristic);
        else
            Logger.warn(TAG, "unhandled call to onCharacteristicChanged");
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        Logger.error(TAG, "onDescriptorWrite('" + gatt + "', " + descriptor + ", " + ")");

        if (onDescriptorWrite != null)
            onDescriptorWrite.call(gatt, descriptor, status);
        else
            Logger.warn(TAG, "unhandled call to onDescriptorWrite");
    }
}
