package com.hello.ble.stack;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.BleOperationCallback.OperationFailReason;
import com.hello.ble.HelloBle;
import com.hello.ble.devices.HelloBleDevice;
import com.hello.ble.devices.Pill;
import com.hello.ble.stack.transmission.BlePacketHandler;
import com.hello.ble.util.BleUUID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by pangwu on 7/14/14.
 */
public class HelloGattLayer extends BluetoothGattCallback {
    public static final int GATT_OPERATION_TIMEOUT_MS = 15000;

    private HelloBleDevice sender;

    private BluetoothDevice bluetoothDevice;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService bluetoothGattService;

    private UUID targetServiceUUID;
    private int connectionStatus = BluetoothProfile.STATE_DISCONNECTED;

    private final BleOperationCallback<Void> connectedCallback;
    private GattOperationTimeoutRunnable connectTimeoutRunnable;

    private final BleOperationCallback<Integer> disconnectedCallback;
    private GattOperationTimeoutRunnable disconnectTimeoutRunnable;

    private BleOperationCallback<UUID> commandWriteCallback;

    private Map<UUID, BleOperationCallback<UUID>> subscribeFinishedCallbacks = new HashMap<>();
    private Map<UUID, BleOperationCallback<UUID>> unsubscribeFinishedCallbacks = new HashMap<>();

    private Map<UUID, GattOperationTimeoutRunnable> subscribeTimeoutRunnables = new HashMap<>();
    private Map<UUID, GattOperationTimeoutRunnable> unsubscribeTimeoutRunnables = new HashMap<>();

    private final BlePacketHandler transmissionLayer;

    private Handler messageHandler;
    private HandlerThread lopperThread = new HandlerThread("HelloGattHandler");


    private synchronized void waitUntilReady() {
        messageHandler = new Handler(Looper.getMainLooper());
    }

    public int getConnectionStatus() {
        return this.connectionStatus;
    }


    public void setCommandWriteCallback(final BleOperationCallback<UUID> bleOperationCallback) {
        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                HelloGattLayer.this.commandWriteCallback = bleOperationCallback;
            }
        });
    }

    public BleOperationCallback<UUID> getCommandWriteCallback() {
        return this.commandWriteCallback;
    }


    public HelloGattLayer(final HelloBleDevice helloDevice,
                          final BluetoothDevice device,
                          final UUID targetDeviceService,
                          final BlePacketHandler transmissionLayer,
                          final BleOperationCallback<Void> userProvidedconnectedCallback,
                          final BleOperationCallback<Integer> disconnectedCallback) {
        this.sender = helloDevice;
        this.bluetoothDevice = device;
        this.targetServiceUUID = targetDeviceService;

        this.transmissionLayer = transmissionLayer;

        this.disconnectedCallback = new BleOperationCallback<Integer>() {

            private void releaseResources() {
                if (HelloGattLayer.this.bluetoothGatt != null) {
                    HelloGattLayer.this.bluetoothGatt.close();  // Fuck this line cost me two days!!
                }


                HelloGattLayer.this.subscribeFinishedCallbacks.clear();
                HelloGattLayer.this.unsubscribeFinishedCallbacks.clear();

                HelloGattLayer.this.subscribeTimeoutRunnables.clear();
                HelloGattLayer.this.unsubscribeTimeoutRunnables.clear();
            }

            @Override
            public void onCompleted(final HelloBleDevice sender, final Integer reason) {
                HelloGattLayer.this.connectionStatus = BluetoothProfile.STATE_DISCONNECTED;
                releaseResources();

                if (disconnectedCallback != null) {
                    disconnectedCallback.onCompleted(sender, reason);
                }
            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                HelloGattLayer.this.connectionStatus = BluetoothProfile.STATE_DISCONNECTED;

                if (reason == OperationFailReason.TIME_OUT) {
                    releaseResources();
                }

                if (disconnectedCallback != null) {
                    disconnectedCallback.onFailed(sender, reason, errorCode);
                }

            }
        };

        this.connectedCallback = new BleOperationCallback<Void>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final Void data) {
                HelloGattLayer.this.connectionStatus = BluetoothProfile.STATE_CONNECTED;

                if (userProvidedconnectedCallback != null) {
                    userProvidedconnectedCallback.onCompleted(HelloGattLayer.this.sender, null);
                }
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if (userProvidedconnectedCallback != null) {
                    userProvidedconnectedCallback.onFailed(HelloGattLayer.this.sender, reason, errorCode);
                }

                // Any exception in connection should result in a hard reset.
                disconnect();
            }
        };

        //this.lopperThread.start();
        this.waitUntilReady();
    }

    public boolean readCharacteristic(final UUID targetServiceUUID, final UUID charUUID) {
        if (this.bluetoothGattService == null) {
            return false;
        }

        final BluetoothGattService service = this.bluetoothGatt.getService(targetServiceUUID);
        if (service == null) {
            return false;
        }
        final BluetoothGattCharacteristic characteristic = service.getCharacteristic(charUUID);
        if (characteristic == null) {
            return false;
        }

        return this.bluetoothGatt.readCharacteristic(characteristic);

    }

    public void connect() {
        this.connectionStatus = BluetoothProfile.STATE_CONNECTING;
        this.connectTimeoutRunnable = new GattOperationTimeoutRunnable(this.sender, this.bluetoothGatt, this.connectedCallback);

        HelloGattLayer.this.bluetoothGatt = HelloGattLayer.this.bluetoothDevice.connectGatt(HelloBle.getApplicationContext(), false, HelloGattLayer.this);
        if (!HelloGattLayer.this.messageHandler.postDelayed(this.connectTimeoutRunnable, HelloGattLayer.GATT_OPERATION_TIMEOUT_MS)) {
            // Hard reset everything.
            this.connectedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.MESSAGE_QUEUE_ERROR, 0);

        }

    }

    public void writeCommand(final byte[] commandData) {
        writeCommand(BleUUID.CHAR_COMMAND_UUID, commandData);
    }

    public void writeCommand(final UUID commandInterfaceUUID, final byte[] commandData) {
        if (commandData.length > 20) {
            throw new IllegalArgumentException("data should not exceed 20 bytes.");
        }

        final byte[] commandCopy = Arrays.copyOf(commandData, commandData.length);
        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                final BluetoothGattCharacteristic commandCharacteristic = HelloGattLayer.this.bluetoothGattService.getCharacteristic(commandInterfaceUUID);
                commandCharacteristic.setValue(commandCopy);
                HelloGattLayer.this.bluetoothGatt.writeCharacteristic(commandCharacteristic);
            }
        });

    }

    public void writeLargeCommand(final UUID commandInterfaceUUID, final byte[] commandData, final BleOperationCallback operationCallback) {
        final byte[] commandCopy = Arrays.copyOf(commandData, commandData.length);
        final List<byte[]> blePackets = this.transmissionLayer.prepareBlePacket(commandCopy);
        final LinkedList<byte[]> remainingPackets = new LinkedList<byte[]>(blePackets);

        final BleOperationCallback<UUID> loopCommandWriteCallback = new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final UUID data) {
                remainingPackets.removeFirst();

                if (remainingPackets.size() > 0) {
                    HelloGattLayer.this.messageHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            final BluetoothGattCharacteristic commandCharacteristic = HelloGattLayer.this.bluetoothGattService.getCharacteristic(commandInterfaceUUID);
                            commandCharacteristic.setValue(remainingPackets.getFirst());
                            HelloGattLayer.this.bluetoothGatt.writeCharacteristic(commandCharacteristic);
                        }
                    });
                } else {

                }
            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                HelloGattLayer.this.setCommandWriteCallback(null);
                if (operationCallback != null) {
                    operationCallback.onFailed(sender, reason, errorCode);
                }
            }
        };

        this.setCommandWriteCallback(loopCommandWriteCallback);

        // TODO: write next packet after the previous one succeed.

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                final BluetoothGattCharacteristic commandCharacteristic = HelloGattLayer.this.bluetoothGattService.getCharacteristic(commandInterfaceUUID);
                commandCharacteristic.setValue(remainingPackets.getFirst());
                HelloGattLayer.this.bluetoothGatt.writeCharacteristic(commandCharacteristic);
            }
        });


    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                if (newState == BluetoothProfile.STATE_CONNECTED) {

                    switch (HelloGattLayer.this.connectionStatus) {
                        case BluetoothProfile.STATE_CONNECTING:
                            // Connecting -> Connected
                            HelloGattLayer.this.messageHandler.removeCallbacks(HelloGattLayer.this.connectTimeoutRunnable);

                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                // GATT Connected
                                // Reset the timeout counter, give service discovery another GATT_OPERATION_TIMEOUT_MS to finish.
                                HelloGattLayer.this.connectTimeoutRunnable = new GattOperationTimeoutRunnable(HelloGattLayer.this.sender,
                                        HelloGattLayer.this.bluetoothGatt,
                                        HelloGattLayer.this.connectedCallback
                                );
                                if (!HelloGattLayer.this.messageHandler.postDelayed(HelloGattLayer.this.connectTimeoutRunnable, GATT_OPERATION_TIMEOUT_MS)) {
                                    // We cannot re-register the timeout counter, fail quick!
                                    HelloGattLayer.this.connectedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.MESSAGE_QUEUE_ERROR, 0);
                                } else {
                                    // everything looks ok so far, start service discovery.
                                    boolean discoverServices = gatt.discoverServices();

                                    // crap, service discovery initialization failure.
                                    if (!discoverServices) {
                                        Log.w(HelloGattLayer.class.getName(), "Unable to discover service for " + HelloGattLayer.this.sender.toString());
                                        HelloGattLayer.this.connectedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.SERVICE_DISCOVERY_FAILED, -1);
                                    }
                                }
                            } else {
                                // GATT connection failed.
                                HelloGattLayer.this.connectedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.GATT_ERROR, status);
                            }

                            break;
                        default:
                            //disconnect();

                            //Other state -> connected....?
                            break;


                    }
                }


                if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                    switch (HelloGattLayer.this.connectionStatus) {
                        case BluetoothProfile.STATE_CONNECTED:
                            // Connected -> Disconnected: Connection lost.
                            HelloGattLayer.this.disconnectedCallback.onCompleted(HelloGattLayer.this.sender, status);

                            break;
                        case BluetoothProfile.STATE_DISCONNECTING:
                            // Disconnecting -> Disconnected: User initial disconnection.
                            // Unregister disconnection timeout event
                            HelloGattLayer.this.messageHandler.removeCallbacks(HelloGattLayer.this.disconnectTimeoutRunnable);

                            HelloGattLayer.this.disconnectedCallback.onCompleted(HelloGattLayer.this.sender, status);

                            break;

                        case BluetoothProfile.STATE_CONNECTING:
                            // Connecting -> Disconnected: We got kicked out
                            // Unregister connection timeout event
                            HelloGattLayer.this.messageHandler.removeCallbacks(HelloGattLayer.this.connectTimeoutRunnable);

                            // Fire disconnected succeed event
                            HelloGattLayer.this.disconnectedCallback.onCompleted(HelloGattLayer.this.sender, status);
                            break;
                    }

                }
            }
        });

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.i(HelloGattLayer.class.getName(), "status = " + status + ", new state = " + newState);
        }

        super.onConnectionStateChange(gatt, status, newState);

    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                // unregister the timeout event.
                HelloGattLayer.this.messageHandler.removeCallbacks(HelloGattLayer.this.connectTimeoutRunnable);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    HelloGattLayer.this.bluetoothGattService = gatt.getService(HelloGattLayer.this.targetServiceUUID);
                    if (HelloGattLayer.this.bluetoothGattService != null) {
                        HelloGattLayer.this.connectedCallback.onCompleted(HelloGattLayer.this.sender, null);
                    } else {
                        HelloGattLayer.this.connectedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.SERVICE_DISCOVERY_FAILED, status);
                    }

                } else {
                    // service discovery failed.
                    HelloGattLayer.this.connectedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.SERVICE_DISCOVERY_FAILED, status);
                }
                //IO.log("Pill connected: " + PillGattLayer.this.sender.getAddress());
            }
        });
    }

    @Override
    public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        final byte[] values = characteristic.getValue();
        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                HelloGattLayer.this.transmissionLayer.dispatch(characteristic.getUuid(), values);
            }
        });
    }

    @Override
    public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                if ((BleUUID.CHAR_COMMAND_UUID.equals(characteristic.getUuid()) || BleUUID.CHAR_PROTOBUF_COMMAND_UUID.equals(characteristic.getUuid()))
                        && HelloGattLayer.this.commandWriteCallback != null) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        HelloGattLayer.this.commandWriteCallback.onCompleted(HelloGattLayer.this.sender, characteristic.getUuid());
                    } else {
                        HelloGattLayer.this.commandWriteCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.GATT_ERROR, status);
                    }
                }
            }
        });
    }

    @Override
    public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        final byte[] values = characteristic.getValue();
        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                HelloGattLayer.this.transmissionLayer.dispatch(characteristic.getUuid(), values);
            }
        });


    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(HelloGattLayer.class.getName(), "onDescriptorWrite, status: " + status);
                BleOperationCallback<UUID> callback = null;
                GattOperationTimeoutRunnable timeoutRunnable = null;
                final UUID charUUID = descriptor.getCharacteristic().getUuid();

                if (Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    callback = HelloGattLayer.this.subscribeFinishedCallbacks.get(charUUID);
                    timeoutRunnable = HelloGattLayer.this.subscribeTimeoutRunnables.get(charUUID);
                    HelloGattLayer.this.subscribeTimeoutRunnables.remove(charUUID);
                } else if (Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    callback = HelloGattLayer.this.unsubscribeFinishedCallbacks.get(charUUID);
                    timeoutRunnable = HelloGattLayer.this.unsubscribeTimeoutRunnables.get(charUUID);
                    HelloGattLayer.this.unsubscribeTimeoutRunnables.remove(charUUID);
                }

                if (timeoutRunnable != null) {
                    HelloGattLayer.this.messageHandler.removeCallbacks(timeoutRunnable);
                }

                if (callback == null) {
                    return;
                }

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    callback.onCompleted(HelloGattLayer.this.sender, descriptor.getCharacteristic().getUuid());
                } else {
                    callback.onFailed(HelloGattLayer.this.sender, OperationFailReason.GATT_ERROR, status);
                }
            }
        });
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
    }

    public void subscribeNotification(final UUID charUUID,
                                      final BleOperationCallback<UUID> subscribeFinishedCallback) {

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                // check connection, fail early
                if (HelloGattLayer.this.sender.isConnected() == false && subscribeFinishedCallback != null) {
                    subscribeFinishedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.CONNECTION_LOST, 0);
                    return;
                }

                final BluetoothGattCharacteristic characteristic = HelloGattLayer.this.bluetoothGattService.getCharacteristic(charUUID);
                if (!HelloGattLayer.this.bluetoothGatt.setCharacteristicNotification(characteristic, true)) {
                    Log.w(Pill.class.getName(), "Set notification for Characteristic: " + characteristic.getUuid() + " failed.");
                    if (subscribeFinishedCallback != null) {
                        subscribeFinishedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.SET_CHAR_NOTIFICATION_FAILED, 0);
                    }

                    return;
                } else {
                    final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleUUID.DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);  // This is the {0x01, 0x00} that shows up in the firmware
                    if (!HelloGattLayer.this.bluetoothGatt.writeDescriptor(descriptor)) {
                        Log.w(Pill.class.getName(), "Set notification for descriptor: " + descriptor.getUuid() + " failed.");

                        if (subscribeFinishedCallback != null) {
                            subscribeFinishedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.WRITE_CCCD_FAILED, 0);
                        }

                        return;
                    }
                }

                // OK everything seems good here.
                // Generate timeout runnable for unexpected connection lost.
                final GattOperationTimeoutRunnable timeoutRunnable =
                        new GattOperationTimeoutRunnable(HelloGattLayer.this.sender,
                                HelloGattLayer.this.bluetoothGatt,
                                subscribeFinishedCallback);
                // mark down the runnable so we can remove it later after operation succeed.
                HelloGattLayer.this.subscribeTimeoutRunnables.put(charUUID, timeoutRunnable);
                HelloGattLayer.this.messageHandler.postDelayed(timeoutRunnable, GATT_OPERATION_TIMEOUT_MS);

                if (subscribeFinishedCallback != null) {
                    HelloGattLayer.this.subscribeFinishedCallbacks.put(charUUID, subscribeFinishedCallback);
                }
            }
        });


    }

    public void unsubscribeNotification(final UUID charUUID,
                                        final BleOperationCallback<UUID> unsubscribeFinishedCallback) {


        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {

                // check connection
                if (HelloGattLayer.this.sender.isConnected() == false && unsubscribeFinishedCallback != null) {
                    unsubscribeFinishedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.CONNECTION_LOST, 0);
                    return;
                }

                // Remove the subscription callback and timeout runnable since we no long need them.
                HelloGattLayer.this.subscribeFinishedCallbacks.remove(charUUID);
                HelloGattLayer.this.subscribeTimeoutRunnables.remove(charUUID);

                final BluetoothGattCharacteristic characteristic = HelloGattLayer.this.bluetoothGattService.getCharacteristic(charUUID);
                final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleUUID.DESCRIPTOR_CHAR_COMMAND_RESPONSE_CONFIG);
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);  // This is the {0x01, 0x00} that shows up in the firmware
                if (!HelloGattLayer.this.bluetoothGatt.writeDescriptor(descriptor)) {
                    Log.w(Pill.class.getName(), "Set notification for descriptor: " + descriptor.getUuid() + " failed.");

                    // Notify the upper layer we cannot write to gatt.
                    if (unsubscribeFinishedCallback != null) {
                        unsubscribeFinishedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.SET_CHAR_NOTIFICATION_FAILED, 0);
                    }
                    return;
                } else {
                    if (!HelloGattLayer.this.bluetoothGatt.setCharacteristicNotification(characteristic, false)) {
                        Log.w(Pill.class.getName(), "Reset notification for Characteristic: " + characteristic.getUuid() + " failed.");
                        if (unsubscribeFinishedCallback != null) {
                            unsubscribeFinishedCallback.onFailed(HelloGattLayer.this.sender, OperationFailReason.WRITE_CCCD_FAILED, 0);
                        }
                        return;
                    }

                }

                // OK everything seems good here.
                // Generate timeout runnable for unexpected connection lost.
                final GattOperationTimeoutRunnable timeoutRunnable =
                        new GattOperationTimeoutRunnable(HelloGattLayer.this.sender,
                                HelloGattLayer.this.bluetoothGatt,
                                unsubscribeFinishedCallback);

                // mark down the runnable so we can remove it later after operation succeed.
                HelloGattLayer.this.unsubscribeTimeoutRunnables.put(charUUID, timeoutRunnable);
                HelloGattLayer.this.messageHandler.postDelayed(timeoutRunnable, GATT_OPERATION_TIMEOUT_MS);


                if (unsubscribeFinishedCallback != null) {
                    HelloGattLayer.this.unsubscribeFinishedCallbacks.put(charUUID, unsubscribeFinishedCallback);
                }
            }
        });

    }

    public void disconnect() {
        if (this.connectionStatus == BluetoothProfile.STATE_DISCONNECTED) {
            // Do not trigger the disconnect event
            Log.i(HelloGattLayer.class.getName(), "disconnect() called without connect.");
            return;
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) HelloBle.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (this.bluetoothGatt != null && bluetoothManager.getConnectionState(this.bluetoothDevice, BluetoothProfile.GATT) == BluetoothProfile.STATE_DISCONNECTED) {
            Log.i(HelloGattLayer.class.getName(), "device is actually disconnected.");
            this.messageHandler.post(new Runnable() {
                public void run() {
                    HelloGattLayer.this.connectionStatus = BluetoothProfile.STATE_DISCONNECTED;
                    HelloGattLayer.this.disconnectedCallback.onCompleted(HelloGattLayer.this.sender, 0);
                }
            });

            return;
        }

        this.connectionStatus = BluetoothProfile.STATE_DISCONNECTING;

        this.messageHandler.post(new Runnable() {
            @Override
            public void run() {
                if (HelloGattLayer.this.bluetoothGatt != null) {

                    // Create timeout runnable, prepare for unexpected.
                    HelloGattLayer.this.disconnectTimeoutRunnable =
                            new GattOperationTimeoutRunnable(
                                    HelloGattLayer.this.sender,
                                    HelloGattLayer.this.bluetoothGatt,
                                    HelloGattLayer.this.disconnectedCallback);

                    // Register the timeout.
                    boolean postResult = HelloGattLayer.this.messageHandler.postDelayed(HelloGattLayer.this.disconnectTimeoutRunnable, GATT_OPERATION_TIMEOUT_MS);
                    if (!postResult) {
                        Log.w(HelloGattLayer.class.getName(), "Post delay failed. Force disconnected.");
                        disconnectedCallback.onCompleted(HelloGattLayer.this.sender, -1);
                        return;
                    }


                    // Trigger the normal disconnect.
                    HelloGattLayer.this.bluetoothGatt.disconnect();

                } else {
                    // For some reason the device is not initialized, still return succeed.
                    Log.w(HelloGattLayer.class.getName(), "Disconnect with null gatt layer. Force disconnected.");
                    if (disconnectedCallback != null) {
                        disconnectedCallback.onCompleted(HelloGattLayer.this.sender, -1);
                    }
                }
            }
        });

    }
}
