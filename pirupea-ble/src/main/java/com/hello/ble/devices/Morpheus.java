package com.hello.ble.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.hello.ble.BleOperationCallback;
import com.hello.ble.HelloBle;
import com.hello.ble.MorpheusCommandType;
import com.hello.ble.protobuf.MorpheusBle.MorpheusCommand;
import com.hello.ble.protobuf.MorpheusBle.MorpheusCommand.CommandType;
import com.hello.ble.stack.HelloGattLayer;
import com.hello.ble.stack.application.MorpheusProtobufResponseDataHandler;
import com.hello.ble.stack.application.MorpheusResponseDataHandler;
import com.hello.ble.stack.transmission.MorpheusBlePacketHandler;
import com.hello.ble.util.BleUUID;
import com.hello.ble.util.HelloBleDeviceScanner;
import com.hello.ble.util.MorpheusScanner;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by pangwu on 8/6/14.
 */
public class Morpheus extends HelloBleDevice {
    private static final int DEFAULT_SCAN_INTERVAL_MS = 10000;
    private static int COMMAND_VERSION = 0;

    private MorpheusResponseDataHandler commandResponsePacketHandler;
    private MorpheusProtobufResponseDataHandler protobufCommandResponseHandler;

    private BleOperationCallback<MorpheusCommandType> commandResponseCallback;
    private BleOperationCallback<MorpheusCommand> protoBufResponseCallback;

    public void setCommandResponseCallback(final BleOperationCallback<MorpheusCommandType> commandResponseCallback){
        this.commandResponseCallback = commandResponseCallback;
    }

    public void setProtoBufResponseCallback(final BleOperationCallback<MorpheusCommand> protoBufResponseCallback){
        this.protoBufResponseCallback = protoBufResponseCallback;
    }


    public Morpheus(final Context context, final BluetoothDevice bluetoothDevice){
        super(context, bluetoothDevice);
        checkNotNull(context);


        this.commandResponsePacketHandler = new MorpheusResponseDataHandler(this);
        this.protobufCommandResponseHandler = new MorpheusProtobufResponseDataHandler(this);

        final BleOperationCallback<Void> connectedCallback = new BleOperationCallback<Void>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final Void data) {

                Morpheus.this.gattLayer.subscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID data) {
                        Morpheus.this.gattLayer.unsubscribeNotification(data, new BleOperationCallback<UUID>() {
                            @Override
                            public void onCompleted(final HelloBleDevice sender, final UUID data) {
                                if(Morpheus.this.connectedCallback != null){
                                    Morpheus.this.connectedCallback.onCompleted(sender, null);
                                }
                            }

                            @Override
                            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                                if(Morpheus.this.connectedCallback != null){
                                    Morpheus.this.connectedCallback.onFailed(sender, reason, errorCode);
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if(OperationFailReason.GATT_ERROR == reason && errorCode == 5){
                            // Authentication required.
                            pair(new BleOperationCallback<Void>() {
                                @Override
                                public void onCompleted(final HelloBleDevice sender, final Void data) {
                                    if(Morpheus.this.connectedCallback != null){
                                        Morpheus.this.connectedCallback.onCompleted(sender, data);
                                    }
                                }

                                @Override
                                public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                                    if(Morpheus.this.connectedCallback != null){
                                        Morpheus.this.connectedCallback.onFailed(sender, reason, errorCode);
                                    }
                                }
                            });
                        }else{
                            if(Morpheus.this.connectedCallback != null){
                                Morpheus.this.connectedCallback.onFailed(sender, reason, errorCode);
                            }
                        }
                    }
                });


            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                if(Morpheus.this.connectedCallback != null){
                    Morpheus.this.connectedCallback.onFailed(sender, reason, errorCode);
                }
            }
        };

        final BleOperationCallback<Integer> disconnectCallback = new BleOperationCallback<Integer>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final Integer reason) {
                if(Morpheus.this.disconnectedCallback != null){
                    Morpheus.this.disconnectedCallback.onCompleted(sender, reason);
                }
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if(Morpheus.this.disconnectedCallback != null){
                    Morpheus.this.disconnectedCallback.onFailed(sender, reason, errorCode);
                }
            }
        };

        // this is the transmission layer
        final MorpheusBlePacketHandler transmissionLayer = new MorpheusBlePacketHandler();

        // attach application layer on top of transmission layer
        //transmissionLayer.registerDataHandler(this.commandResponsePacketHandler);
        transmissionLayer.registerDataHandler(this.protobufCommandResponseHandler);

        // attach the link layer to transmission layer
        this.gattLayer = new HelloGattLayer(this, this.bluetoothDevice,
                BleUUID.MORPHEUS_SERVICE_UUID,
                transmissionLayer,
                connectedCallback,
                disconnectCallback);

    }


    public static boolean discover(final String address, final BleOperationCallback<Morpheus> onDiscoverCompleted, final int maxScanTime){
        //checkNotNull(context);
        checkNotNull(HelloBle.getApplicationContext());
        checkNotNull(onDiscoverCompleted);

        final Context context = HelloBle.getApplicationContext();
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            return false;
        }


        final Set<Morpheus> gods = new HashSet<Morpheus>();

        final BleOperationCallback<Set<HelloBleDevice>> scanDiscoveryCallback = new BleOperationCallback<Set<HelloBleDevice>>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final Set<HelloBleDevice> advertisingMorpheus) {
                for(final HelloBleDevice device:advertisingMorpheus){
                    gods.add((Morpheus) device);
                }

                Morpheus targetMorpheus = null;

                for(final Morpheus morpheus:gods){
                    if(morpheus.getAddress().equals(address)){
                        targetMorpheus = morpheus;
                    }
                }

                onDiscoverCompleted.onCompleted(null, targetMorpheus);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                // This will never be called.
            }
        };


        final HelloBleDeviceScanner scanner = new MorpheusScanner(new String[]{ address },
                maxScanTime <= 0 ? DEFAULT_SCAN_INTERVAL_MS : maxScanTime,
                scanDiscoveryCallback);

        scanner.beginDiscovery();

        return true;

    }

    public static boolean discover(final BleOperationCallback<Set<Morpheus>> onDiscoverCompleted, final int maxScanTime){
        //checkNotNull(context);
        checkNotNull(HelloBle.getApplicationContext());
        checkNotNull(onDiscoverCompleted);

        final Context context = HelloBle.getApplicationContext();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            return false;
        }

        final Set<Morpheus> finalDevices = new HashSet<Morpheus>();

        final BleOperationCallback<Set<HelloBleDevice>> scanDiscoveryCallback = new BleOperationCallback<Set<HelloBleDevice>>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedPill, final Set<HelloBleDevice> advertisingDevices) {

                for(final HelloBleDevice device:advertisingDevices){
                    finalDevices.add((Morpheus)device);
                }
                onDiscoverCompleted.onCompleted(null, finalDevices);
                //bondScanner.beginDiscovery();
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                // This will never be called.
            }
        };

        final HelloBleDeviceScanner scanner = new MorpheusScanner(null,
                maxScanTime <= 0 ? DEFAULT_SCAN_INTERVAL_MS : maxScanTime,
                scanDiscoveryCallback);

        scanner.beginDiscovery();

        return true;

    }

    public void setConnectedCallback(final BleOperationCallback<Void> connectedCallback){
        this.connectedCallback = connectedCallback;
    }

    public void setDisconnectedCallback(final BleOperationCallback<Integer> disconnectCallback){
        this.disconnectedCallback = disconnectCallback;
    }


    public void switchToPairingMode(final BleOperationCallback<Void> pairingFinishedCallback){
        pairingModeSwitch(true, pairingFinishedCallback);
    }

    public void switchToNormalMode(final BleOperationCallback<Void> pairingFinishedCallback){
        pairingModeSwitch(false, pairingFinishedCallback);
    }

    private void pairingModeSwitch(final boolean toPairingMode, final BleOperationCallback<Void> modeChangedFinishedCallback){
        this.protobufCommandResponseHandler.setDataCallback(new BleOperationCallback<MorpheusCommand>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final MorpheusCommand response) {
                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(response.getType() == CommandType.MORPHEUS_COMMAND_ERROR){
                            if(modeChangedFinishedCallback != null){
                                modeChangedFinishedCallback.onFailed(sender, OperationFailReason.INTERNAL_ERROR, response.getError().getNumber());
                            }
                        }else {
                            if ((toPairingMode == false && response.getType() != CommandType.MORPHEUS_COMMAND_SWITCH_TO_NORMAL_MODE) ||
                                    (toPairingMode && response.getType() != CommandType.MORPHEUS_COMMAND_SWITCH_TO_PAIRING_MODE)) {
                                // something is wrong here
                                if (modeChangedFinishedCallback != null) {
                                    modeChangedFinishedCallback.onFailed(sender, OperationFailReason.WRONG_ACK_TYPE, 0);
                                }
                            } else {
                                if (modeChangedFinishedCallback != null) {
                                    modeChangedFinishedCallback.onCompleted(sender, null);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason internalReason, final int internalCode) {
                        if(modeChangedFinishedCallback != null){
                            modeChangedFinishedCallback.onFailed(sender, internalReason, internalCode);
                        }
                    }
                });


            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(modeChangedFinishedCallback != null){
                            modeChangedFinishedCallback.onFailed(sender, reason, errorCode);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason internalReason, final int internalCode) {
                        if(modeChangedFinishedCallback != null){
                            modeChangedFinishedCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });

            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedDevice, final UUID charUUID) {
                final MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder().setType(toPairingMode ?
                            CommandType.MORPHEUS_COMMAND_SWITCH_TO_PAIRING_MODE : CommandType.MORPHEUS_COMMAND_SWITCH_TO_NORMAL_MODE)
                        .setVersion(COMMAND_VERSION)
                        .build();
                final byte[] commandData = morpheusCommand.toByteArray();
                Morpheus.this.gattLayer.writeLargeCommand(BleUUID.CHAR_PROTOBUF_COMMAND_UUID, commandData, modeChangedFinishedCallback);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if(modeChangedFinishedCallback != null){
                    modeChangedFinishedCallback.onFailed(sender, reason, errorCode);
                }
            }
        });
    }


    public void clearPairedUser(final BleOperationCallback<Void> clearFinishedCallback){
        this.protobufCommandResponseHandler.setDataCallback(new BleOperationCallback<MorpheusCommand>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final MorpheusCommand response) {
                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(response.getType() != CommandType.MORPHEUS_COMMAND_EREASE_PAIRED_PHONE){
                            // something is wrong here
                            if(response.getType() == CommandType.MORPHEUS_COMMAND_ERROR){
                                if(clearFinishedCallback != null){
                                    clearFinishedCallback.onFailed(sender, OperationFailReason.INTERNAL_ERROR, response.getError().getNumber());
                                }
                            }else {
                                if (clearFinishedCallback != null) {
                                    // Wrong command received, which means data out of order for some reason.
                                    clearFinishedCallback.onFailed(sender, OperationFailReason.WRONG_ACK_TYPE, 0);
                                }
                            }
                        }else {
                            if (clearFinishedCallback != null) {
                                clearFinishedCallback.onCompleted(sender, null);
                            }
                        }
                    }

                    @Override
                    public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                        if(clearFinishedCallback != null){
                            clearFinishedCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });


            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(HelloBleDevice sender, final UUID charUUID) {
                        if(clearFinishedCallback != null){
                            clearFinishedCallback.onFailed(sender, reason, errorCode);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason internalReason, int internalCode) {
                        if(clearFinishedCallback != null){
                            clearFinishedCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });

            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedDevice, final UUID charUUID) {
                final MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                        .setType(CommandType.MORPHEUS_COMMAND_EREASE_PAIRED_PHONE)
                        .setVersion(COMMAND_VERSION)
                        .build();
                final byte[] commandData = morpheusCommand.toByteArray();
                Morpheus.this.gattLayer.writeLargeCommand(BleUUID.CHAR_PROTOBUF_COMMAND_UUID, commandData, clearFinishedCallback);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if(clearFinishedCallback != null){
                    clearFinishedCallback.onFailed(sender, reason, errorCode);
                }
            }
        });
    }



    public void getDeviceId(final BleOperationCallback<String> getDeviceIdCallback){
        this.protobufCommandResponseHandler.setDataCallback(new BleOperationCallback<MorpheusCommand>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final MorpheusCommand response) {
                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(response.getType() != CommandType.MORPHEUS_COMMAND_GET_DEVICE_ID){

                            if(response.getType() == CommandType.MORPHEUS_COMMAND_ERROR){
                                if(getDeviceIdCallback != null){
                                    getDeviceIdCallback.onFailed(sender, OperationFailReason.INTERNAL_ERROR, response.getError().getNumber());
                                }
                            }else {
                                if (getDeviceIdCallback != null) {
                                    // Wrong command received, which means data out of order for some reason.
                                    getDeviceIdCallback.onFailed(sender, OperationFailReason.WRONG_ACK_TYPE, 0);
                                }
                            }

                        }else{
                            if(getDeviceIdCallback != null){
                                getDeviceIdCallback.onCompleted(sender, response.getDeviceId());
                            }
                        }
                    }

                    @Override
                    public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                        if(getDeviceIdCallback != null){
                            getDeviceIdCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });

            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(HelloBleDevice sender, final UUID charUUID) {
                        if(getDeviceIdCallback != null){
                            getDeviceIdCallback.onFailed(sender, reason, errorCode);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason internalReason, final int internalCode) {
                        if(getDeviceIdCallback != null){
                            getDeviceIdCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });

            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedDevice, final UUID charUUID) {
                final MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                        .setType(CommandType.MORPHEUS_COMMAND_GET_DEVICE_ID)
                        .setVersion(COMMAND_VERSION)
                        .build();
                final byte[] commandData = morpheusCommand.toByteArray();
                Morpheus.this.gattLayer.writeLargeCommand(BleUUID.CHAR_PROTOBUF_COMMAND_UUID, commandData, getDeviceIdCallback);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if(getDeviceIdCallback != null){
                    getDeviceIdCallback.onFailed(sender, reason, errorCode);
                }
            }
        });
    }


    public void wipeFirmware(final BleOperationCallback<Void> operationCallback){
        final BleOperationCallback<Void> writeCallback = new BleOperationCallback<Void>() {
            @Override
            public void onCompleted(HelloBleDevice sender, Void data) {
                operationCallback.onCompleted(sender, null);

            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                operationCallback.onFailed(sender, reason, errorCode);
            }
        };

        this.gattLayer.subscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice connectedDevice, final UUID charUUID) {
                final MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                        .setType(CommandType.MORPHEUS_COMMAND_MORPHEUS_DFU_BEGIN)
                        .setVersion(COMMAND_VERSION)
                        .build();
                final byte[] commandData = morpheusCommand.toByteArray();
                Morpheus.this.gattLayer.writeLargeCommand(BleUUID.CHAR_PROTOBUF_COMMAND_UUID, commandData, writeCallback);
            }

            @Override
            public void onFailed(HelloBleDevice sender, OperationFailReason reason, int errorCode) {
                if(operationCallback != null){
                    operationCallback.onFailed(sender, reason, errorCode);
                }
            }
        });
    }


    public void setWIFIConnection(final String BSSID, final String SSID, final String password, final BleOperationCallback<Void> operationCallback){
        this.protobufCommandResponseHandler.setDataCallback(new BleOperationCallback<MorpheusCommand>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final MorpheusCommand replyCommand) {

                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(replyCommand.getType() == CommandType.MORPHEUS_COMMAND_SET_WIFI_ENDPOINT){

                            if(operationCallback != null){
                                operationCallback.onCompleted(sender, null);
                            }
                        }else{
                            if(replyCommand.getType() == CommandType.MORPHEUS_COMMAND_ERROR){
                                if(operationCallback != null){
                                    operationCallback.onFailed(sender, OperationFailReason.INTERNAL_ERROR, replyCommand.getError().getNumber());
                                }
                            }else {
                                if (operationCallback != null) {
                                    // Wrong command received, which means data out of order for some reason.
                                    operationCallback.onFailed(sender, OperationFailReason.WRONG_ACK_TYPE, replyCommand.getType().getNumber());
                                }
                            }

                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });


            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, OperationFailReason.DATA_LOST_OR_OUT_OF_ORDER, 0);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, OperationFailReason.DATA_LOST_OR_OUT_OF_ORDER, 0);
                        }
                    }
                });
            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                // Write command

                final MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                        .setType(CommandType.MORPHEUS_COMMAND_SET_WIFI_ENDPOINT)
                        .setVersion(COMMAND_VERSION)
                        .setWifiName(BSSID)
                        .setWifiSSID(SSID)
                        .setWifiPassword(password)
                        .build();
                Morpheus.this.gattLayer.writeLargeCommand(BleUUID.CHAR_PROTOBUF_COMMAND_UUID, morpheusCommand.toByteArray(), operationCallback);

            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                if(operationCallback != null){
                    operationCallback.onFailed(sender, reason, errorCode);
                }
            }
        });
    }


    public void pairPill(final String accountToken, final BleOperationCallback<String> operationCallback){
        this.protobufCommandResponseHandler.setDataCallback(new BleOperationCallback<MorpheusCommand>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final MorpheusCommand replyCommand) {

                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(replyCommand.getType() == CommandType.MORPHEUS_COMMAND_PAIR_PILL){
                            final String pillId = replyCommand.getDeviceId();
                            final String accountId = replyCommand.getAccountId();

                            if(operationCallback != null){
                                operationCallback.onCompleted(sender, pillId);
                            }
                        }else{
                            if(replyCommand.getType() == CommandType.MORPHEUS_COMMAND_ERROR){
                                if(operationCallback != null){
                                    operationCallback.onFailed(sender, OperationFailReason.INTERNAL_ERROR, replyCommand.getError().getNumber());
                                }
                            }else {
                                if (operationCallback != null) {
                                    // Wrong command received, which means data out of order for some reason.
                                    operationCallback.onFailed(sender, OperationFailReason.WRONG_ACK_TYPE, 0);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });


            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, OperationFailReason.DATA_LOST_OR_OUT_OF_ORDER, 0);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, OperationFailReason.DATA_LOST_OR_OUT_OF_ORDER, 0);
                        }
                    }
                });
            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                // Write command
                final MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                        .setType(CommandType.MORPHEUS_COMMAND_PAIR_PILL)
                        .setVersion(COMMAND_VERSION)
                        .setAccountId(accountToken)
                        .build();
                Morpheus.this.gattLayer.writeLargeCommand(BleUUID.CHAR_PROTOBUF_COMMAND_UUID, morpheusCommand.toByteArray(), operationCallback);

            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                if(operationCallback != null){
                    operationCallback.onFailed(sender, reason, errorCode);
                }
            }
        });
    }


    public void unpairPill(final String pillId, final BleOperationCallback<String> operationCallback){
        this.protobufCommandResponseHandler.setDataCallback(new BleOperationCallback<MorpheusCommand>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final MorpheusCommand replyCommand) {

                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(replyCommand.getType() == CommandType.MORPHEUS_COMMAND_UNPAIR_PILL){

                            if(operationCallback != null){
                                operationCallback.onCompleted(sender, pillId);
                            }
                        }else{
                            if(replyCommand.getType() == CommandType.MORPHEUS_COMMAND_ERROR){
                                if(operationCallback != null){
                                    operationCallback.onFailed(sender, OperationFailReason.INTERNAL_ERROR, replyCommand.getError().getNumber());
                                }
                            }else {
                                if (operationCallback != null) {
                                    // Wrong command received, which means data out of order for some reason.
                                    operationCallback.onFailed(sender, OperationFailReason.WRONG_ACK_TYPE, 0);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });


            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, OperationFailReason.DATA_LOST_OR_OUT_OF_ORDER, 0);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, OperationFailReason.DATA_LOST_OR_OUT_OF_ORDER, 0);
                        }
                    }
                });
            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                // Write command
                final MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                        .setType(CommandType.MORPHEUS_COMMAND_UNPAIR_PILL)
                        .setVersion(COMMAND_VERSION)
                        .setDeviceId(pillId.toUpperCase())
                        .build();
                Morpheus.this.gattLayer.writeLargeCommand(BleUUID.CHAR_PROTOBUF_COMMAND_UUID, morpheusCommand.toByteArray(), operationCallback);

            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                if(operationCallback != null){
                    operationCallback.onFailed(sender, reason, errorCode);
                }
            }
        });
    }


    public void linkAccount(final String accountToken, final BleOperationCallback<Void> operationCallback){
        this.protobufCommandResponseHandler.setDataCallback(new BleOperationCallback<MorpheusCommand>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final MorpheusCommand replyCommand) {

                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(replyCommand.getType() == CommandType.MORPHEUS_COMMAND_PAIR_SENSE){
                            final String morpheusId = replyCommand.getDeviceId();
                            final String accountId = replyCommand.getAccountId();

                            if(operationCallback != null){
                                operationCallback.onCompleted(sender, null);
                            }
                        }else{
                            if(replyCommand.getType() == CommandType.MORPHEUS_COMMAND_ERROR){
                                if(operationCallback != null){
                                    operationCallback.onFailed(sender, OperationFailReason.INTERNAL_ERROR, replyCommand.getError().getNumber());
                                }
                            }else {
                                if (operationCallback != null) {
                                    // Wrong command received, which means data out of order for some reason.
                                    operationCallback.onFailed(sender, OperationFailReason.WRONG_ACK_TYPE, replyCommand.getType().getNumber());
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, reason, errorCode);
                        }
                    }
                });


            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                Morpheus.this.gattLayer.unsubscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
                    @Override
                    public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, OperationFailReason.DATA_LOST_OR_OUT_OF_ORDER, 0);
                        }
                    }

                    @Override
                    public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                        if(operationCallback != null){
                            operationCallback.onFailed(sender, OperationFailReason.DATA_LOST_OR_OUT_OF_ORDER, 0);
                        }
                    }
                });
            }
        });


        this.gattLayer.subscribeNotification(BleUUID.CHAR_PROTOBUF_COMMAND_RESPONSE_UUID, new BleOperationCallback<UUID>() {
            @Override
            public void onCompleted(final HelloBleDevice sender, final UUID charUUID) {
                // Write command
                final MorpheusCommand morpheusCommand = MorpheusCommand.newBuilder()
                        .setType(CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                        .setVersion(COMMAND_VERSION)
                        .setAccountId(accountToken)
                        .build();
                Morpheus.this.gattLayer.writeLargeCommand(BleUUID.CHAR_PROTOBUF_COMMAND_UUID, morpheusCommand.toByteArray(), operationCallback);

            }

            @Override
            public void onFailed(final HelloBleDevice sender, final OperationFailReason reason, final int errorCode) {
                if(operationCallback != null){
                    operationCallback.onFailed(sender, reason, errorCode);
                }
            }
        });
    }


}
