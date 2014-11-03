package is.hello.sense.hardware;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.hardware.stacks.android.NativeDeviceCenter;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

@Module(library = true)
@SuppressWarnings("UnusedDeclaration")
public class BluetoothModule {
    @Provides @Singleton Scheduler provideScheduler() {
        return AndroidSchedulers.mainThread();
    }

    @Provides @Singleton DeviceCenter provideDeviceCenter(@NonNull Context applicationContext,
                                                          @NonNull Scheduler scheduler) {
        return new NativeDeviceCenter(applicationContext, scheduler);
    }
}
