package com.hello.ble.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.BleOperationCallback.OperationFailReason;
import com.hello.ble.HelloBle;
import com.hello.ble.PillCommand;
import com.hello.ble.PillMotionData;
import com.hello.ble.stack.HelloGattLayer;
import com.hello.ble.stack.application.DeviceIdDataHandler;
import com.hello.ble.stack.application.MotionDataHandler;
import com.hello.ble.stack.application.MotionStreamDataHandler;
import com.hello.ble.stack.application.PillBatteryVoltageDataHandler;
import com.hello.ble.stack.application.PillResponseDataHandler;
import com.hello.ble.stack.application.TimeDataHandler;
import com.hello.ble.stack.transmission.PillBlePacketHandler;
import com.hello.ble.util.BleDateTimeConverter;
import com.hello.ble.util.BleUUID;
import com.hello.ble.util.HelloBleDeviceScanner;
import com.hello.ble.util.PillScanner;

import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by pangwu on 7/1/14.
 */
public class Pill extends HelloBleDevice {
    private TimeDataHandler bleTimeDataHandler;
    private MotionDataHandler motionPacketHandler;
    private MotionStreamDataHandler motionStreamDataHandler;
    private PillResponseDataHandler commandResponsePacketHandler;
    private DeviceIdDataHandler deviceIdDataHandler;

    private PillBatteryVoltageDataHandler pillBatteryVoltageDataHandler;

    private PillBlePacketHandler transmissionLayer;


    public Pill(final Context context, final BluetoothDevice pillDevice) {
        super(context, pillDevice);

        if (context == null)
            throw new IllegalArgumentException();

        this.bleTimeDataHandler = new TimeDataHandler(this);
        this.motionPacketHandler = new MotionDataHandler(this);
        this.commandResponsePacketHandler = new PillResponseDataHandler(this);
        this.motionStreamDataHandler = new MotionStreamDataHandler(this);
        this.pillBatteryVoltageDataHandler = new PillBatteryVoltageDataHandler(this);
        this.deviceIdDataHandler = new DeviceIdDataHandler(this);


        final BleOperationCallback<Void> connectedCallback = new BleOperationCallback<Void>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final Void data) {
                // We are not connected yet. get device id then we are connected.
                Pill.this.updateDeviceId(Pill.this.connectedCallback);
            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                if (Pill.this.connectedCallback != null) {
                    Pill.this.connectedCallback.onFailed(sender, reason, errorCode);
                }
            }
        };

        final BleOperationCallback<Integer> disconnectCallback = new BleOperationCallback<Integer>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final Integer reason) {
                if (Pill.this.disconnectedCallback != null) {
                    Pill.this.disconnectedCallback.onCompleted(sender, reason);
                }
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if (Pill.this.disconnectedCallback != null) {
                    Pill.this.disconnectedCallback.onFailed(sender, reason, errorCode);
                }
            }
        };

        // this is the transmission layer
        this.transmissionLayer = new PillBlePacketHandler();

        // attach application layer on top of transmission layer
        transmissionLayer.registerDataHandler(this.bleTimeDataHandler);
        transmissionLayer.registerDataHandler(this.bleTimeDataHandler);
        transmissionLayer.registerDataHandler(this.motionPacketHandler);
        transmissionLayer.registerDataHandler(this.commandResponsePacketHandler);
        transmissionLayer.registerDataHandler(this.deviceIdDataHandler);

        // attach the link layer to transmission layer
        this.gattLayer = new HelloGattLayer(this, this.bluetoothDevice,
                BleUUID.PILL_SERVICE_UUID,
                transmissionLayer,
                connectedCallback,
                disconnectCallback);

    }


    public void setTime(final DateTime target) {
        setTime(target, null);
    }


    public void setTime(final DateTime target, final BleOperationCallback<UUID> setTimeFinishedCallback) {

        this.gattLayer.setCommandWriteCallback(setTimeFinishedCallback);

        final byte[] optionalBLETime = BleDateTimeConverter.dateTimeToBLETime(target);
        if (optionalBLETime == null) {
            return;
        }

        final byte[] commandData = new byte[1 + optionalBLETime.length];
        commandData[0] = PillCommand.SET_TIME.getValue();
        for (int i = 0; i < optionalBLETime.length; i++) {
            commandData[i + 1] = optionalBLETime[i];
        }

        this.gattLayer.writeCommand(commandData);
    }

    private void saveAndResetPreviousCommandWriteCallback() {
        final BleOperationCallback<UUID> previousCommandWriteCallback = this.gattLayer.getCommandWriteCallback();

        // When the current callback has been executed, set the previous callback to place.
        this.gattLayer.setCommandWriteCallback(new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final UUID charUUID) {
                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
            }
        });
    }


    public void getTime(final BleOperationCallback<DateTime> getTimeCallback) {

        saveAndResetPreviousCommandWriteCallback();

        this.bleTimeDataHandler.setDataCallback(new BleOperationCallback<DateTime>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final DateTime dateTime) {
                // We don't care whether the unsubscription is successful or not
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DAY_DATETIME_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if (getTimeCallback != null) {
                            getTimeCallback.onCompleted(Pill.this, dateTime);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if (getTimeCallback != null) {
                            getTimeCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });


            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DAY_DATETIME_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if (getTimeCallback != null) {
                            getTimeCallback.onFailed(sender, reason, errorCode);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason internalReason, final int internalCode) {
                        if (getTimeCallback != null) {
                            getTimeCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });
            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_DAY_DATETIME_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final UUID charUUID) {
                final byte[] pillCommandData = new byte[]{PillCommand.GET_TIME.getValue()};
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if (getTimeCallback != null) {
                    getTimeCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

    }

    public void calibrate(final BleOperationCallback<Void> calibrateCallback) {
        saveAndResetPreviousCommandWriteCallback();

        this.commandResponsePacketHandler.setDataCallback(new BleOperationCallback<PillCommand>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final PillCommand data) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(HelloBleDevice sender, final UUID charUUID) {
                        if (calibrateCallback != null) {
                            calibrateCallback.onCompleted(Pill.this, null);
                        }
                    }

                    @Override
                    public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                        if (calibrateCallback != null) {
                            calibrateCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });

            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(HelloBleDevice sender, final UUID charUUID) {
                        if (calibrateCallback != null) {
                            calibrateCallback.onFailed(sender, reason, errorCode);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason internalReason, final int internalCode) {
                        if (calibrateCallback != null) {
                            calibrateCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });

            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final UUID charUUID) {
                final byte[] pillCommandData = new byte[]{PillCommand.CALIBRATE.getValue()};
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if (calibrateCallback != null) {
                    calibrateCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

    }


    @Deprecated
    public void startStream(final BleOperationCallback<Void> operationCallback, final BleOperationCallback<Long[]> dataCallback) {
        this.gattLayer.setCommandWriteCallback(null);

        this.gattLayer.subscribeNotification(BleUUID.CHAR_DATA_UUID, new BleOperationCallback<UUID>() {

            @Override
            public void onCompleted(final HelloBleDevice sender, final UUID dataCharUUID) {
                Pill.this.gattLayer.setCommandWriteCallback(new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID commandCharUUID) {
                        Pill.this.transmissionLayer.unregisterDataHandler(Pill.this.motionPacketHandler);
                        Pill.this.transmissionLayer.registerDataHandler(Pill.this.motionStreamDataHandler);
                        Pill.this.motionStreamDataHandler.setDataCallback(dataCallback);

                        if (operationCallback != null) {
                            operationCallback.onCompleted(sender, null);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if (operationCallback != null) {
                            operationCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });

                final byte[] pillCommandData = new byte[]{PillCommand.START_STREAM.getValue()};
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                if (operationCallback != null) {
                    operationCallback.onFailed(sender, reason, errorCode);
                }
            }
        });
    }

    @Deprecated
    public void stopStream(final BleOperationCallback<Void> operationCallback) {

        this.transmissionLayer.unregisterDataHandler(this.motionStreamDataHandler);
        this.transmissionLayer.registerDataHandler(this.motionPacketHandler);

        this.gattLayer.setCommandWriteCallback(new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DATA_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if (operationCallback != null) {
                            operationCallback.onCompleted(sender, null);
                        }
                    }

                    @Override
                    public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                        if (operationCallback != null) {
                            operationCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if (operationCallback != null) {
                    operationCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

        final byte[] pillCommandData = new byte[]{PillCommand.STOP_STREAM.getValue()};
        Pill.this.gattLayer.writeCommand(pillCommandData);

    }


    public void getData(final int unitLength, final BleOperationCallback<List<PillMotionData>> getDataCallback) {

        saveAndResetPreviousCommandWriteCallback();
        final MotionDataHandler motionDataHandler32Bit = new MotionDataHandler(unitLength, this);

        this.transmissionLayer.unregisterDataHandler(this.motionPacketHandler);  // avoid conflict
        this.transmissionLayer.registerDataHandler(motionDataHandler32Bit);

        motionDataHandler32Bit.setDataCallback(new BleOperationCallback<List<PillMotionData>>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final List<PillMotionData> data) {
                Pill.this.transmissionLayer.unregisterDataHandler(motionDataHandler32Bit);
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DATA_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if (getDataCallback != null) {
                            getDataCallback.onCompleted(Pill.this, data);
                        }
                    }

                    @Override
                    public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                        if (getDataCallback != null) {
                            getDataCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });
            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Pill.this.transmissionLayer.unregisterDataHandler(motionDataHandler32Bit);
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_DATA_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if (getDataCallback != null) {
                            getDataCallback.onFailed(sender, reason, errorCode);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason internalReason, final int internalCode) {
                        if (getDataCallback != null) {
                            getDataCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });


            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_DATA_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final UUID charUUID) {
                final byte[] pillCommandData = new byte[]{PillCommand.GET_DATA.getValue()};
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.transmissionLayer.unregisterDataHandler(motionDataHandler32Bit);
                if (getDataCallback != null) {
                    getDataCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

    }

    private void updateDeviceId(final BleOperationCallback<Void> followupOperationCallback) {
        this.transmissionLayer.unregisterDataHandler(this.motionPacketHandler);
        this.deviceIdDataHandler.setDataCallback(new BleOperationCallback<String>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final String data) {
                Pill.this.deviceIdDataHandler.setDataCallback(null);
                Pill.this.setId(data);

                Pill.this.transmissionLayer.registerDataHandler(Pill.this.motionPacketHandler);

                if (followupOperationCallback != null) {
                    followupOperationCallback.onCompleted(sender, null);
                }
            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Pill.this.deviceIdDataHandler.setDataCallback(null);
                Pill.this.transmissionLayer.registerDataHandler(Pill.this.motionPacketHandler);

                if (followupOperationCallback != null) {
                    followupOperationCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

        if (!this.gattLayer.readCharacteristic(BleUUID.DEVICE_INFO_SERVICE_UUID, BleUUID.CHAR_DEVICEID_UUID)) {
            this.deviceIdDataHandler.setDataCallback(null);
            if (followupOperationCallback != null) {
                followupOperationCallback.onFailed(this, OperationFailReason.GET_ID_FAILED, -1);
            }
        }
    }

    public void getBatteryLevel(final BleOperationCallback<Integer> getBatteryLevelCallback) {
        this.transmissionLayer.unregisterDataHandler(this.commandResponsePacketHandler);
        this.transmissionLayer.registerDataHandler(this.pillBatteryVoltageDataHandler);
        final BleOperationCallback<UUID> previousCommandWriteCallback = this.gattLayer.getCommandWriteCallback();

        this.pillBatteryVoltageDataHandler.setDataCallback(new BleOperationCallback<Integer>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final Integer data) {

                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
                Pill.this.transmissionLayer.registerDataHandler(Pill.this.commandResponsePacketHandler);
                Pill.this.transmissionLayer.unregisterDataHandler(Pill.this.pillBatteryVoltageDataHandler);

                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if (getBatteryLevelCallback != null) {
                            getBatteryLevelCallback.onCompleted(Pill.this, data);
                        }
                    }

                    @Override
                    public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                        if (getBatteryLevelCallback != null) {
                            getBatteryLevelCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });

            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {


                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
                Pill.this.transmissionLayer.registerDataHandler(Pill.this.commandResponsePacketHandler);
                Pill.this.transmissionLayer.unregisterDataHandler(Pill.this.pillBatteryVoltageDataHandler);

                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if (getBatteryLevelCallback != null) {
                            getBatteryLevelCallback.onFailed(sender, reason, errorCode);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason internalReason, final int internalCode) {
                        if (getBatteryLevelCallback != null) {
                            getBatteryLevelCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });

            }
        });

        final BleOperationCallback<UUID> commandWriteCallback = new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                // Do nothing, write to command interface succeed.
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, null);

                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
                Pill.this.transmissionLayer.registerDataHandler(Pill.this.commandResponsePacketHandler);
                Pill.this.transmissionLayer.unregisterDataHandler(Pill.this.pillBatteryVoltageDataHandler);

                if (getBatteryLevelCallback != null) {
                    getBatteryLevelCallback.onFailed(sender, reason, errorCode);
                }
            }
        };


        this.gattLayer.subscribeNotification(BleUUID.CHAR_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final UUID charUUID) {
                final byte[] pillCommandData = new byte[]{PillCommand.GET_BATTERY_VOLT.getValue()};
                Pill.this.gattLayer.setCommandWriteCallback(commandWriteCallback);
                Pill.this.gattLayer.writeCommand(pillCommandData);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                Pill.this.gattLayer.setCommandWriteCallback(previousCommandWriteCallback);
                Pill.this.transmissionLayer.registerDataHandler(Pill.this.commandResponsePacketHandler);
                Pill.this.transmissionLayer.unregisterDataHandler(Pill.this.pillBatteryVoltageDataHandler);

                if (getBatteryLevelCallback != null) {
                    getBatteryLevelCallback.onFailed(sender, reason, errorCode);
                }
            }
        });

    }

    public void setConnectedCallback(final BleOperationCallback<Void> connectedCallback) {
        this.connectedCallback = connectedCallback;
    }

    public void setDisconnectedCallback(final BleOperationCallback<Integer> disconnectCallback) {
        this.disconnectedCallback = disconnectCallback;
    }


    public boolean isPaired() {
        if (this.bluetoothDevice == null) {
            return false;
        }

        return this.bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED;
    }


    public static boolean discover(final String address, final BleOperationCallback<Pill> onDiscoverCompleted, final int maxScanTime) {
        if (onDiscoverCompleted == null)
            throw new IllegalArgumentException();

        final Context context = HelloBle.getApplicationContext();
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return false;
        }


        final Set<Pill> pills = new HashSet<Pill>();

        final BleOperationCallback<Set<HelloBleDevice>> scanDiscoveryCallback = new BleOperationCallback<Set<HelloBleDevice>>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final Set<HelloBleDevice> advertisingPills) {
                for (final HelloBleDevice device : advertisingPills) {
                    pills.add((Pill) device);
                }

                Pill targetPill = null;

                for (final Pill pill : pills) {
                    if (pill.getAddress().equals(address)) {
                        targetPill = pill;
                    }
                }

                onDiscoverCompleted.onCompleted(null, targetPill);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                // This will never be called.
            }
        };


        final HelloBleDeviceScanner scanner = new PillScanner(new String[]{address},
                maxScanTime <= 0 ? DEFAULT_SCAN_INTERVAL_MS : maxScanTime,
                scanDiscoveryCallback);

        scanner.beginDiscovery();

        return true;

    }

    public static boolean discover(final BleOperationCallback<Set<Pill>> onDiscoverCompleted, final int maxScanTime) {
        if (onDiscoverCompleted == null)
            throw new IllegalArgumentException();

        final Context context = HelloBle.getApplicationContext();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return false;
        }

        final Set<Pill> finalPills = new HashSet<Pill>();

        final BleOperationCallback<Set<HelloBleDevice>> scanDiscoveryCallback = new BleOperationCallback<Set<HelloBleDevice>>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final Set<HelloBleDevice> advertisingDevices) {

                for (final HelloBleDevice device : advertisingDevices) {
                    finalPills.add((Pill) device);
                }
                onDiscoverCompleted.onCompleted(null, finalPills);
                //bondScanner.beginDiscovery();
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                // This will never be called.
            }
        };

        final HelloBleDeviceScanner scanner = new PillScanner(null,
                maxScanTime <= 0 ? DEFAULT_SCAN_INTERVAL_MS : maxScanTime,
                scanDiscoveryCallback);

        scanner.beginDiscovery();

        return true;

    }

}
