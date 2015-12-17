package is.hello.sense.bluetooth;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.buruberi.bluetooth.Buruberi;
import is.hello.buruberi.bluetooth.errors.BuruberiException;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.util.ErrorListener;
import is.hello.sense.util.Analytics;

/**
 * A partial object graph that vends a configured BluetoothStack object.
 * <p/>
 * Requires a containing module to provide an unqualified
 * application Context in order to compile.
 */
@Module(library = true, complete = false)
@SuppressWarnings("UnusedDeclaration")
public class BluetoothModule {
    @Provides ErrorListener provideErrorListener() {
        return new ErrorListener() {
            @Override
            public void call(Throwable e) {
                if (e != null && !(e instanceof BuruberiException)) {
                    Analytics.trackUnexpectedError(e);
                }
            }
        };
    }

    @Provides @Singleton BluetoothStack provideDeviceCenter(@NonNull Context applicationContext,
                                                            @NonNull ErrorListener errorListener) {
        return new Buruberi()
                .setApplicationContext(applicationContext)
                .setErrorListener(errorListener)
                .build();
    }
}
