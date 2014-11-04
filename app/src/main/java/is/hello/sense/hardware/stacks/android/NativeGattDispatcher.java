package is.hello.sense.hardware.stacks.android;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.Nullable;

import is.hello.sense.hardware.Command;
import is.hello.sense.hardware.Device;
import is.hello.sense.util.Logger;
import rx.functions.Action2;
import rx.functions.Action3;
import rx.subjects.PublishSubject;

public class NativeGattDispatcher extends BluetoothGattCallback {
    public final PublishSubject<Command> incomingSubject = PublishSubject.create();
    public @Nullable Action3<BluetoothGatt, Integer, Integer> onConnectionStateChanged;
    public @Nullable Action2<BluetoothGatt, Integer> onServicesDiscovered;
    public @Nullable Action3<BluetoothGatt, BluetoothGattCharacteristic, Integer> onCharacteristicWrite;
    public @Nullable Action3<BluetoothGatt, BluetoothGattDescriptor, Integer> onDescriptorWrite;

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        Logger.error(Device.LOG_TAG, "onConnectionStateChange('" + gatt + "', " + status + ", " + newState + ")");

        if (onConnectionStateChanged != null)
            onConnectionStateChanged.call(gatt, status, newState);
        else
            Logger.warn(Device.LOG_TAG, "unhandled call to onConnectionStateChange");
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Logger.error(Device.LOG_TAG, "onServicesDiscovered('" + gatt + "', " + status + ")");

        if (onServicesDiscovered != null)
            onServicesDiscovered.call(gatt, status);
        else
            Logger.warn(Device.LOG_TAG, "unhandled call to onServicesDiscovered");
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        Logger.error(Device.LOG_TAG, "onCharacteristicRead('" + gatt + "', " + characteristic + ", " + status + ")");

        incomingSubject.onNext(Command.with(characteristic.getUuid(), characteristic.getValue()));
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        Logger.error(Device.LOG_TAG, "onCharacteristicWrite('" + gatt + "', " + characteristic + ", " + status + ")");

        if (onCharacteristicWrite != null)
            onCharacteristicWrite.call(gatt, characteristic, status);
        else
            Logger.warn(Device.LOG_TAG, "unhandled call to onCharacteristicWrite");
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        Logger.error(Device.LOG_TAG, "onCharacteristicChanged('" + gatt + "', " + characteristic + ", " + ")");

        incomingSubject.onNext(Command.with(characteristic.getUuid(), characteristic.getValue()));
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        Logger.error(Device.LOG_TAG, "onDescriptorWrite('" + gatt + "', " + descriptor + ", " + ")");

        if (onDescriptorWrite != null)
            onDescriptorWrite.call(gatt, descriptor, status);
        else
            Logger.warn(Device.LOG_TAG, "unhandled call to onDescriptorWrite");
    }
}
