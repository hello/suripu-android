package com.hello.ble;

import com.hello.ble.devices.HelloBleDevice;

/**
 * Created by pangwu on 7/2/14.
 */
public interface BleOperationCallback<T> {

    public enum OperationFailReason {
        CONNECTION_LOST(0),
        GATT_ERROR(1),
        TIME_OUT(2),
        SET_CHAR_NOTIFICATION_FAILED(3),
        WRITE_CCCD_FAILED(4),
        MESSAGE_QUEUE_ERROR(5),
        SERVICE_DISCOVERY_FAILED(6),
        DATA_LOST_OR_OUT_OF_ORDER(7),
        INVALID_PROTOBUF(8),
        WRONG_ACK_TYPE(9),
        GET_ID_FAILED(10),
        GATT_NOT_INITIALIZED(11),
        INTERNAL_ERROR(12);

        private int value = 0;

        private OperationFailReason(int value) {
            this.value = value;
        }
    }

    public void onCompleted(final HelloBleDevice sender, final T data);

    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode);
}
