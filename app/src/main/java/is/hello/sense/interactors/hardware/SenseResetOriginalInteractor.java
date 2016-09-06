package is.hello.sense.interactors.hardware;


import android.content.Context;
import android.support.annotation.NonNull;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;

public class SenseResetOriginalInteractor extends BaseHardwareInteractor {

    public SenseResetOriginalInteractor(@NonNull final Context context,
                                        @NonNull final BluetoothStack bluetoothStack) {

        super(context, bluetoothStack);
    }
}
