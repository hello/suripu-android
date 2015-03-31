package is.hello.sense.bluetooth;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.android.AndroidBluetoothStack;
import is.hello.sense.bluetooth.stacks.util.ErrorListener;
import is.hello.sense.util.Errors;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * A partial object graph that vends a configured BluetoothStack object.
 * <p/>
 * Requires a containing module to provide an unqualified
 * application Context in order to compile.
 */
@Module(library = true, complete = false)
@SuppressWarnings("UnusedDeclaration")
public class BluetoothModule {
    @Provides Scheduler provideScheduler() {
        return AndroidSchedulers.mainThread();
    }

    @Provides ErrorListener provideErrorListener() {
        return Errors.REPORT_UNEXPECTED;
    }

    @Provides @Singleton BluetoothStack provideDeviceCenter(@NonNull Context applicationContext,
                                                            @NonNull Scheduler scheduler,
                                                            @NonNull ErrorListener errorListener) {
        return new AndroidBluetoothStack(applicationContext, scheduler, errorListener);
    }
}
