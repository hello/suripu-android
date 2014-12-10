package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.bluetooth.devices.SensePeripheralError;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.bluetooth.errors.BluetoothDisabledError;
import is.hello.sense.bluetooth.errors.BluetoothGattError;
import is.hello.sense.bluetooth.errors.BluetoothPowerChangeError;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.sense.bluetooth.errors.PeripheralConnectionError;
import is.hello.sense.bluetooth.errors.PeripheralNotFoundError;
import is.hello.sense.bluetooth.errors.PeripheralServiceDiscoveryFailedError;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;

public class ErrorDialogFragment extends DialogFragment {
    public static final String TAG = ErrorDialogFragment.class.getSimpleName();

    private static final String ARG_MESSAGE = ErrorDialogFragment.class.getName() + ".ARG_MESSAGE";
    private static final String ARG_HAS_REQUEST_INFO = ErrorDialogFragment.class.getName() + ".ARG_HAS_REQUEST_INFO";
    private static final String ARG_URL = ErrorDialogFragment.class.getName() + ".ARG_URL";
    private static final String ARG_RESPONSE_STATUS = ErrorDialogFragment.class.getName() + ".ARG_RESPONSE_STATUS";
    private static final String ARG_RESPONSE_REASON = ErrorDialogFragment.class.getName() + ".ARG_RESPONSE_REASON";

    private static final int RESPONSE_STATUS_UNKNOWN = -1;

    public static void presentError(@NonNull FragmentManager fm, @Nullable Throwable e) {
        ErrorDialogFragment fragment = ErrorDialogFragment.newInstance(e);
        fragment.show(fm, TAG);
    }

    public static void presentBluetoothError(@NonNull FragmentManager fm,
                                             @NonNull Context context,
                                             @NonNull Throwable e) {
        String message = null;
        if (e instanceof BluetoothDisabledError) {
            message = context.getString(R.string.error_bluetooth_disabled);
        } else if (e instanceof BluetoothGattError) {
            message = context.getString(R.string.error_bluetooth_gatt_failure_fmt, e.getMessage());
        } else if (e instanceof OperationTimeoutError) {
            message = context.getString(R.string.error_generic_bluetooth_timeout);
        } else if (e instanceof PeripheralBondAlterationError) {
            message = context.getString(R.string.error_bluetooth_bonding_change_fmt, e.toString());
        } else if (e instanceof PeripheralConnectionError) {
            message = context.getString(R.string.error_bluetooth_no_connection);
        } else if (e instanceof PeripheralServiceDiscoveryFailedError) {
            message = context.getString(R.string.error_bluetooth_service_discovery_failed);
        } else if (e instanceof PeripheralNotFoundError) {
            message = context.getString(R.string.error_sense_not_found);
        } else if (e instanceof BluetoothPowerChangeError) {
            message = context.getString(R.string.error_bluetooth_power_change);
        } else if (e instanceof SensePeripheralError) {
            MorpheusBle.ErrorType errorType = ((SensePeripheralError) e).errorType;
            switch (errorType) {
                case DEVICE_ALREADY_PAIRED:
                    message = context.getString(R.string.error_sense_already_paired);
                    break;

                case DEVICE_DATABASE_FULL:
                    message = context.getString(R.string.error_sense_device_db_full);
                    break;

                case TIME_OUT:
                    message = context.getString(R.string.error_generic_bluetooth_timeout);
                    break;

                case NETWORK_ERROR:
                case WLAN_CONNECTION_ERROR:
                    message = context.getString(R.string.error_bad_wifi_credentials);
                    break;

                case NO_ENDPOINT_IN_RANGE:
                    message = context.getString(R.string.error_wifi_out_of_range);
                    break;

                case FAIL_TO_OBTAIN_IP:
                    message = context.getString(R.string.error_wifi_ip_failure);
                    break;

                default:
                    message = errorType.toString();
                    break;
            }
        }

        ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(message);
        dialogFragment.show(fm, TAG);
    }

    public static ErrorDialogFragment newInstance(@Nullable Throwable e) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();

        Bundle arguments = new Bundle();
        if (e != null) {
            // RetrofitError is not properly Serializable! So we'll grab the things we're interested
            // out of the Throwable object and pull them out of the arguments on the other side.
            arguments.putString(ARG_MESSAGE, e.getMessage());

            if (e instanceof ApiException) {
                ApiException error = (ApiException) e;
                arguments.putBoolean(ARG_HAS_REQUEST_INFO, true);
                arguments.putString(ARG_URL, error.getUrl());
                if (error.getStatus() != null)
                    arguments.putInt(ARG_RESPONSE_STATUS, error.getStatus());
                arguments.putString(ARG_RESPONSE_REASON, error.getReason());
            }
        }
        fragment.setArguments(arguments);

        return fragment;
    }

    public static ErrorDialogFragment newInstance(@Nullable String message) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_MESSAGE, message);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(true);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

        dialog.setTitle(R.string.dialog_error_title);
        String message = getMessage();
        if (!TextUtils.isEmpty(message)) {
            if (hasRequestInfo()) {
                dialog.setMessage(getString(R.string.dialog_error_extended_message_format, message, getResponseReason(), getResponseStatus()));
            } else {
                dialog.setMessage(message);
            }
        } else {
            dialog.setMessage(R.string.dialog_error_generic_message);
        }

        if (getTargetFragment() != null) {
            dialog.setNegativeButton(R.string.action_retry, (button, which) -> getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null));
        }

        dialog.setPositiveButton(android.R.string.ok, null);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        Analytics.error(getMessage(), getResponseStatus());
    }

    private String getMessage() {
        return getArguments().getString(ARG_MESSAGE);
    }

    private boolean hasRequestInfo() {
        return getArguments().getBoolean(ARG_HAS_REQUEST_INFO, false);
    }

    private String getUrl() {
        return getArguments().getString(ARG_URL);
    }

    private String getResponseReason() {
        return getArguments().getString(ARG_RESPONSE_REASON);
    }

    private int getResponseStatus() {
        return getArguments().getInt(ARG_RESPONSE_STATUS, RESPONSE_STATUS_UNKNOWN);
    }
}
