package is.hello.sense.interactors.hardware;


import android.content.Context;
import android.support.annotation.NonNull;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.util.Analytics;
import rx.Observable;

import static is.hello.sense.util.Analytics.Upgrade.EVENT_FACTORY_RESET;

public class SenseResetOriginalInteractor extends BaseHardwareInteractor {

    public SenseResetOriginalInteractor(@NonNull final Context context,
                                        @NonNull final BluetoothStack bluetoothStack) {

        super(context, bluetoothStack);
    }

    @Override
    public Observable<SensePeripheral> discoverPeripheralForDevice(@NonNull final SenseDevice device) {
        sendOnStartAnalyticsEvent();
        return super.discoverPeripheralForDevice(device);
    }

    protected void sendOnStartAnalyticsEvent(){
        Analytics.trackEvent(EVENT_FACTORY_RESET, null);
    }
}
