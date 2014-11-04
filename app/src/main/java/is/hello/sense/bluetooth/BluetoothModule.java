package is.hello.sense.bluetooth;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.bluetooth.stacks.DeviceCenter;
import is.hello.sense.bluetooth.stacks.android.NativeDeviceCenter;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

@Module(library = true, complete = false)
@SuppressWarnings("UnusedDeclaration")
public class BluetoothModule {
    @Provides @Singleton Scheduler provideScheduler() {
        return AndroidSchedulers.mainThread();
    }

    @Provides @Singleton
    DeviceCenter provideDeviceCenter(@NonNull Context applicationContext,
                                                          @NonNull Scheduler scheduler) {
        return new NativeDeviceCenter(applicationContext, scheduler);
    }
}
