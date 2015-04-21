package is.hello.sense.bluetooth.errors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.util.Errors;
import is.hello.sense.util.StringRef;

/**
 * Used to indicate that a bond could not be created or removed for a peripheral.
 * Generally indicates an unstable bluetooth service on the host device.
 */
public class PeripheralBondAlterationError extends BluetoothError implements Errors.Reporting {
    public final int reason;

    public PeripheralBondAlterationError(int reason) {
        super(getReasonString(reason));

        this.reason = reason;
    }

    //region Bonding Errors

    // This region is copied from the AOSP, and is under the Apache license.

    /**
     * The reason for a bond state change to have occurred.
     * <p/>
     * This extra is not publicly exposed before Android Lollipop / API Level 21,
     * and is only partially public in API Level 21. See the SDK Android Lollipop
     * source for the BluetoothDevice class for more info.
     * @see android.bluetooth.BluetoothDevice#ACTION_BOND_STATE_CHANGED
     */
    public static final String EXTRA_REASON = "android.bluetooth.device.extra.REASON";

    /**
     * Specific to the Hello BLE stack, indicates an unknown failure.
     */
    public static final int REASON_UNKNOWN_FAILURE = -1;

    /**
     * Specific to the Hello BLE stack, indicates that the host does
     * not have the private methods createBond or removeBond on BluetoothDevice.
     */
    public static final int REASON_ANDROID_API_CHANGED = -2;

    /**
     * A bond attempt succeeded
     */
    public static final int BOND_SUCCESS = 0;

    /**
     * A bond attempt failed because pins did not match, or remote device did
     * not respond to pin request in time
     */
    public static final int REASON_AUTH_FAILED = 1;

    /**
     * A bond attempt failed because the other side explicitly rejected
     * bonding
     */
    public static final int REASON_AUTH_REJECTED = 2;

    /**
     * A bond attempt failed because we canceled the bonding process
     */
    public static final int REASON_AUTH_CANCELED = 3;

    /**
     * A bond attempt failed because we could not contact the remote device
     */
    public static final int REASON_REMOTE_DEVICE_DOWN = 4;

    /**
     * A bond attempt failed because a discovery is in progress
     */
    public static final int REASON_DISCOVERY_IN_PROGRESS = 5;

    /**
     * A bond attempt failed because of authentication timeout
     */
    public static final int REASON_AUTH_TIMEOUT = 6;

    /**
     * A bond attempt failed because of repeated attempts
     */
    public static final int REASON_REPEATED_ATTEMPTS = 7;

    /**
     * A bond attempt failed because we received an Authentication Cancel
     * by remote end
     */
    public static final int REASON_REMOTE_AUTH_CANCELED = 8;

    /**
     * An existing bond was explicitly revoked
     */
    public static final int REASON_REMOVED = 9;

    /**
     * Returns the corresponding name for a given {@see #EXTRA_REASON}.
     */
    public static @NonNull String getReasonString(int reason) {
        switch (reason) {
            case BOND_SUCCESS:
                return "BOND_SUCCESS";

            case REASON_AUTH_FAILED:
                return "REASON_AUTH_FAILED";

            case REASON_AUTH_REJECTED:
                return "REASON_AUTH_REJECTED";

            case REASON_AUTH_CANCELED:
                return "REASON_AUTH_CANCELED";

            case REASON_REMOTE_DEVICE_DOWN:
                return "REASON_REMOTE_DEVICE_DOWN";

            case REASON_DISCOVERY_IN_PROGRESS:
                return "REASON_DISCOVERY_IN_PROGRESS";

            case REASON_AUTH_TIMEOUT:
                return "REASON_AUTH_TIMEOUT";

            case REASON_REPEATED_ATTEMPTS:
                return "REASON_REPEATED_ATTEMPTS";

            case REASON_REMOTE_AUTH_CANCELED:
                return "REASON_REMOTE_AUTH_CANCELED";

            case REASON_REMOVED:
                return "REASON_REMOVED";

            case REASON_ANDROID_API_CHANGED:
                return "REASON_ANDROID_API_CHANGED";

            case REASON_UNKNOWN_FAILURE:
            default:
                return "REASON_UNKNOWN_FAILURE (" + reason + ")";
        }
    }

    /**
     * Returns the corresponding name for a given bond state.
     */
    public static @NonNull String getBondStateString(int bondState) {
        switch (bondState) {
            case Peripheral.BOND_NONE:
                return "BOND_NONE";

            case Peripheral.BOND_BONDING:
                return "BOND_BONDING";

            case Peripheral.BOND_BONDED:
                return "BOND_BONDED";

            default:
                return "UNKNOWN (" + bondState + ")";
        }
    }

    //endregion


    @Override
    public boolean isFatal() {
        // If REASON_REMOVED/9 is reported, it indicates that the
        // bond state of the bluetooth device has gotten into a broken
        // state, and won't be fixed until the user restarts their phone.
        return (reason == PeripheralBondAlterationError.REASON_REMOVED);
    }

    @Nullable
    @Override
    public String getContextInfo() {
        return getReasonString(reason);
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        if (reason == PeripheralBondAlterationError.REASON_REMOTE_DEVICE_DOWN) {
            return StringRef.from(R.string.error_bluetooth_out_of_range);
        } else {
            return StringRef.from(R.string.error_bluetooth_bonding_change_fmt, getContextInfo());
        }
    }
}
