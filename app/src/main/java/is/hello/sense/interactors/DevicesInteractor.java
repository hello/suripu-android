package is.hello.sense.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class DevicesInteractor extends ValueInteractor<Devices> {
    private static final long RESUBSCRIBE_DELAY_SECONDS = 2;
    private static final int RETRY_TIMES = 10;
    private final ApiService apiService;

    public final InteractorSubject<Devices> devices = this.subject;

    public DevicesInteractor(@NonNull final ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Devices> provideUpdateObservable() {
        return apiService.registeredDevices();
    }

    public Observable<VoidResponse> unregisterDevice(@NonNull final BaseDevice device) {
        if (device instanceof SleepPillDevice) {
            return apiService.unregisterPill(device.deviceId);
        } else if (device instanceof SenseDevice) {
            return apiService.unregisterSense(device.deviceId);
        } else {
            return Observable.error(new Exception("Unknown device type '" + device.getClass() + "'"));
        }
    }

    public Observable<VoidResponse> removeSenseAssociations(@NonNull final SenseDevice senseDevice) {
        return apiService.removeSenseAssociations(senseDevice.deviceId);
    }

    /**
     * @return {@link Observable<Devices>} after retrying if initial returned devices were missing
     * due to bad sync with server. Update {@link DevicesInteractor#devices} if finished retrying and no other errors were thrown.
     */
    public Observable<Devices> getDevicesWithRetry() {
        final AtomicInteger retryRemaining = new AtomicInteger(RETRY_TIMES);
        return provideUpdateObservable().flatMap( deviceList -> {
            if(shouldResubscribe(deviceList, retryRemaining.getAndDecrement())){
                return Observable.error(new RetryRequiredException());
            } else {
                this.devices.onNext(deviceList);
                return Observable.just(deviceList);
            }
        }).retryWhen( errorObservable -> errorObservable.flatMap(this::provideErrorNotificationHandler));
    }

    private boolean shouldResubscribe(@Nullable final Devices devices,
                                      final int retryRemaining) {
        return retryRemaining > 0
                && (devices == null || devices.getSense() == null);
    }

    private Observable provideErrorNotificationHandler(final Throwable throwable) {
        if (throwable instanceof RetryRequiredException) {
            return Observable.just(null).delay(RESUBSCRIBE_DELAY_SECONDS, TimeUnit.SECONDS);
        } else {
            return Observable.error(throwable); //do not resubscribe
        }
    }

    private class RetryRequiredException extends Throwable {
        RetryRequiredException() {
            super("should only be visible in logs.");
        }
    }
}
