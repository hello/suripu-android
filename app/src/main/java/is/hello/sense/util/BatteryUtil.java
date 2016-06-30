package is.hello.sense.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.annotation.NonNull;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

/**
 * Only fetches current battery levels on creation. To refresh values use {@link this#refresh(Context)}
 */
public class BatteryUtil {

    private Intent batteryStatus;

    public static ErrorDialogFragment getErrorDialog(){
        return new ErrorDialogFragment.Builder()
                .withTitle(R.string.error_phone_battery_low_title)
                .withMessage(StringRef.from(R.string.error_phone_battery_low_message))
                .build();
    }

    public BatteryUtil(@NonNull final Context context){
       refresh(context);
    }

    public boolean canPerformOperation(@NonNull final Operation operation){
        return operation.minBatteryPercentage <= getBatteryPercentage();
    }

    public double getBatteryPercentage(){
        final int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        Logger.debug(BatteryUtil.class.getSimpleName(), "battery level " + level);
        return level / (double) scale;
    }

    public void refresh(@NonNull final Context context){
        final IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.batteryStatus = context.registerReceiver(null, filter);
    }

    public static class Operation {
        final double minBatteryPercentage;

        public Operation(){
            this(0.20);
        }

        public Operation(final double minimumBatteryPercentage){
            this.minBatteryPercentage = minimumBatteryPercentage;
            runAssertions();
        }

        private void runAssertions(){
            if(BuildConfig.DEBUG) {
                if(minBatteryPercentage > 1 || minBatteryPercentage < 0){
                    throw new NumberFormatException("minBatteryPercentage must be between 0 - 1");
                }
            }
        }
    }
}
