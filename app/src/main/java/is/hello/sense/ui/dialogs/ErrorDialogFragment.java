package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.ErrorResponse;
import is.hello.sense.api.model.RegistrationError;
import is.hello.sense.bluetooth.devices.SensePeripheralError;
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.errors.BluetoothConnectionLostError;
import is.hello.sense.bluetooth.errors.BluetoothDisabledError;
import is.hello.sense.bluetooth.errors.BluetoothError;
import is.hello.sense.bluetooth.errors.BluetoothGattError;
import is.hello.sense.bluetooth.errors.BluetoothPowerChangeError;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.sense.bluetooth.errors.PeripheralBusyError;
import is.hello.sense.bluetooth.errors.PeripheralConnectionError;
import is.hello.sense.bluetooth.errors.PeripheralNotFoundError;
import is.hello.sense.bluetooth.errors.PeripheralServiceDiscoveryFailedError;
import is.hello.sense.bluetooth.errors.PeripheralSetWifiError;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Errors;

public class ErrorDialogFragment extends DialogFragment {
    public static final String TAG = ErrorDialogFragment.class.getSimpleName();

    private static final String ARG_ERROR_TYPE = ErrorDialogFragment.class.getName() + ".ARG_ERROR_TYPE";
    private static final String ARG_ERROR_CONTEXT = ErrorDialogFragment.class.getName() + ".ARG_ERROR_CONTEXT";
    private static final String ARG_ERROR_OPERATION = ErrorDialogFragment.class.getName() + ".ARG_ERROR_OPERATION";
    private static final String ARG_MESSAGE = ErrorDialogFragment.class.getName() + ".ARG_MESSAGE";
    private static final String ARG_MESSAGE_RES = ErrorDialogFragment.class.getName() + ".ARG_MESSAGE_RES";
    private static final String ARG_SHOW_SUPPORT_LINK = ErrorDialogFragment.class.getName() + ".ARG_SHOW_SUPPORT_LINK";
    private static final String ARG_SHOW_BLE_SUPPORT = ErrorDialogFragment.class.getName() + ".ARG_SHOW_BLE_SUPPORT";
    private static final String ARG_FATAL_MESSAGE_RES = ErrorDialogFragment.class.getName() + ".ARG_FATAL_MESSAGE_RES";
    private static final String ARG_ACTION_INTENT = ErrorDialogFragment.class.getName() + ".ARG_ACTION_INTENT";
    private static final String ARG_ACTION_TITLE_RES = ErrorDialogFragment.class.getName() + ".ARG_ACTION_TITLE_RES";


    //region Creation

    public static ErrorDialogFragment presentError(@NonNull FragmentManager fm, @Nullable Throwable e) {
        ErrorDialogFragment fragment = ErrorDialogFragment.newInstance(e);
        fragment.show(fm, TAG);
        return fragment;
    }

    public static ErrorDialogFragment presentBluetoothError(@NonNull FragmentManager fm,
                                                            @NonNull Context context,
                                                            @NonNull Throwable e) {
        String message = null;
        if (e instanceof BluetoothDisabledError) {
            message = context.getString(R.string.error_bluetooth_disabled);
        } else if (e instanceof BluetoothGattError) {
            int statusCode = ((BluetoothGattError) e).statusCode;
            switch (statusCode) {
                case BluetoothGattError.GATT_STACK_ERROR: {
                    message = context.getString(R.string.error_bluetooth_gatt_stack);
                    break;
                }

                case BluetoothGattError.GATT_CONN_TERMINATE_LOCAL_HOST: {
                    message = context.getString(R.string.error_bluetooth_gatt_connection_lost);
                    break;
                }

                case BluetoothGattError.GATT_CONN_TIMEOUT: {
                    message = context.getString(R.string.error_bluetooth_gatt_connection_timeout);
                    break;
                }

                case BluetoothGattError.GATT_CONN_FAIL_ESTABLISH: {
                    message = context.getString(R.string.error_bluetooth_gatt_connection_failed);
                    break;
                }

                default: {
                    message = context.getString(R.string.error_bluetooth_gatt_failure_fmt, e.getMessage());
                    break;
                }
            }
        } else if (e instanceof OperationTimeoutError) {
            message = context.getString(R.string.error_generic_bluetooth_timeout);
        } else if (e instanceof PeripheralBondAlterationError) {
            int failureReason = ((PeripheralBondAlterationError) e).reason;
            if (failureReason == PeripheralBondAlterationError.REASON_REMOTE_DEVICE_DOWN) {
                message = context.getString(R.string.error_bluetooth_out_of_range);
            } else {
                message = context.getString(R.string.error_bluetooth_bonding_change_fmt, PeripheralBondAlterationError.getReasonString(failureReason));
            }
        } else if (e instanceof PeripheralConnectionError) {
            message = context.getString(R.string.error_bluetooth_no_connection);
        } else if (e instanceof PeripheralServiceDiscoveryFailedError) {
            message = context.getString(R.string.error_bluetooth_service_discovery_failed);
        } else if (e instanceof PeripheralNotFoundError) {
            message = context.getString(R.string.error_sense_not_found);
        } else if (e instanceof BluetoothPowerChangeError) {
            message = context.getString(R.string.error_bluetooth_power_change);
        } else if (e instanceof PeripheralBusyError) {
            message = context.getString(R.string.error_bluetooth_peripheral_busy);
        } else if (e instanceof BluetoothConnectionLostError) {
            message = context.getString(R.string.error_bluetooth_connection_lost);
        } else if (e instanceof PeripheralSetWifiError) {
            PeripheralSetWifiError.Reason reason = ((PeripheralSetWifiError) e).reason;
            switch (reason) {
                case MALFORMED_BYTES: {
                    message = context.getString(R.string.error_bluetooth_malformed_wifi_password);
                    break;
                }

                case CONTAINS_NUL_BYTE: {
                    message = context.getString(R.string.error_bluetooth_wep_password_nul);
                    break;
                }

                case EMPTY_PASSWORD: {
                    message = context.getString(R.string.error_bluetooth_empty_wifi_password);
                    break;
                }

                default: {
                    message = reason.toString();
                    break;
                }
            }
        } else if (e instanceof SensePeripheralError) {
            SenseCommandProtos.ErrorType errorType = ((SensePeripheralError) e).errorType;
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
                    message = context.getString(R.string.error_network_failure);
                    break;

                case WLAN_CONNECTION_ERROR:
                case NO_ENDPOINT_IN_RANGE:
                    message = context.getString(R.string.error_wifi_connection_failed);
                    break;

                case FAIL_TO_OBTAIN_IP:
                    message = context.getString(R.string.error_wifi_ip_failure);
                    break;

                case INTERNAL_DATA_ERROR:
                case DEVICE_NO_MEMORY:
                case INTERNAL_OPERATION_FAILED:
                    message = context.getString(R.string.error_generic_sense_failure);
                    break;

                default:
                    message = errorType.toString();
                    break;
            }
        }


        ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(message);
        if (BluetoothError.isFatal(e)) {
            dialogFragment.setShowBluetoothSupport(true);
            dialogFragment.setFatalMessage(R.string.error_addendum_unstable_stack);
        }
        dialogFragment.setErrorType(Errors.getType(e));
        dialogFragment.setErrorContext(Errors.getContext(e));
        dialogFragment.setShowSupportLink(true);
        dialogFragment.show(fm, TAG);

        return dialogFragment;
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
                if (error.isNetworkError()) {
                    arguments.putInt(ARG_MESSAGE_RES, R.string.error_network_unavailable);
                    arguments.remove(ARG_MESSAGE);
                } else {
                    ErrorResponse errorResponse = error.getErrorResponse();
                    if (errorResponse != null) {
                        RegistrationError registrationError = RegistrationError.fromString(errorResponse.getMessage());
                        if (registrationError != RegistrationError.UNKNOWN) {
                            arguments.putInt(ARG_MESSAGE_RES, registrationError.messageRes);
                            arguments.remove(ARG_MESSAGE);
                        }
                    }
                }
            }
        }
        fragment.setArguments(arguments);
        fragment.setErrorType(Errors.getType(e));
        fragment.setErrorContext(Errors.getContext(e));

        return fragment;
    }

    public static ErrorDialogFragment newInstance(@Nullable String message) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_MESSAGE, message);
        fragment.setArguments(arguments);

        return fragment;
    }

    public static ErrorDialogFragment newInstance(@StringRes int messageRes) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(ARG_MESSAGE_RES, messageRes);
        fragment.setArguments(arguments);

        return fragment;
    }

    //endregion


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(true);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

        boolean isFatal = getArguments().containsKey(ARG_FATAL_MESSAGE_RES);
        if (isFatal) {
            dialog.setTitle(R.string.dialog_error_title_fatal);
            dialog.setTitleColor(getResources().getColor(R.color.destructive_accent));
        } else {
            dialog.setTitle(R.string.dialog_error_title);
        }
        String message = getMessage();
        if (!TextUtils.isEmpty(message)) {
            if (isFatal) {
                SpannableStringBuilder messageBuilder = new SpannableStringBuilder(message);
                messageBuilder.append(getText(getFatalMessage()));
                dialog.setMessage(messageBuilder);
            } else {
                dialog.setMessage(message);
            }
        } else {
            dialog.setMessage(R.string.dialog_error_generic_message);
        }

        if (showSupportLink()) {
            SpannableStringBuilder footer = Styles.resolveSupportLinks(getActivity(), getText(R.string.error_addendum_support));
            footer.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, footer.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            footer.insert(0, dialog.getMessage());
            dialog.setMessage(footer);
        }

        if (getTargetFragment() != null) {
            dialog.setNegativeButton(R.string.action_retry, (button, which) -> getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null));
        }

        dialog.setPositiveButton(android.R.string.ok, null);

        if (getArguments().containsKey(ARG_ACTION_INTENT) && getArguments().containsKey(ARG_ACTION_TITLE_RES)) {
            int titleRes = getArguments().getInt(ARG_ACTION_TITLE_RES);
            dialog.setNegativeButton(titleRes, (button, which) -> {
                Intent intent = getArguments().getParcelable(ARG_ACTION_INTENT);
                startActivity(intent);
            });
        } else if (getShowBluetoothSupport()) {
            dialog.setNegativeButton(R.string.action_more_info, (button, which) -> {
                UserSupport.showForDeviceIssue(getActivity(), UserSupport.DeviceIssue.UNSTABLE_BLUETOOTH);
            });
        }

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        Analytics.trackError(getMessage(), getErrorType(), getErrorContext(), getErrorOperation());
    }

    private void setShowBluetoothSupport(boolean showBluetoothSupport) {
        getArguments().putBoolean(ARG_SHOW_BLE_SUPPORT, showBluetoothSupport);
    }


    private boolean getShowBluetoothSupport() {
        return getArguments().getBoolean(ARG_SHOW_BLE_SUPPORT, false);
    }

    private void setFatalMessage(@StringRes int messageRes) {
        getArguments().putInt(ARG_FATAL_MESSAGE_RES, messageRes);
    }

    private @StringRes int getFatalMessage() {
        return getArguments().getInt(ARG_FATAL_MESSAGE_RES);
    }

    private String getMessage() {
        if (getArguments().containsKey(ARG_MESSAGE_RES)) {
            return getString(getArguments().getInt(ARG_MESSAGE_RES));
        } else {
            return getArguments().getString(ARG_MESSAGE);
        }
    }

    //region Error Context

    private @Nullable String getErrorContext() {
        return getArguments().getString(ARG_ERROR_CONTEXT);
    }

    private void setErrorContext(@Nullable String errorContext) {
        getArguments().putString(ARG_ERROR_CONTEXT, errorContext);
    }

    private @Nullable String getErrorType() {
        return getArguments().getString(ARG_ERROR_TYPE);
    }

    private void setErrorType(@Nullable String errorType) {
        getArguments().putString(ARG_ERROR_TYPE, errorType);
    }

    public @Nullable String getErrorOperation() {
        return getArguments().getString(ARG_ERROR_OPERATION);
    }

    public void setErrorOperation(@Nullable String errorType) {
        getArguments().putString(ARG_ERROR_OPERATION, errorType);
    }

    //endregion


    //region Actions

    public boolean showSupportLink() {
        return getArguments().getBoolean(ARG_SHOW_SUPPORT_LINK, false);
    }

    public void setShowSupportLink(boolean show) {
        getArguments().putBoolean(ARG_SHOW_SUPPORT_LINK, show);
    }

    public void setAction(@NonNull Intent intent, @StringRes int titleRes) {
        getArguments().putParcelable(ARG_ACTION_INTENT, intent);
        getArguments().putInt(ARG_ACTION_TITLE_RES, titleRes);
    }

    //endregion
}
