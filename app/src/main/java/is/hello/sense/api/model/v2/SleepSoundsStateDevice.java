package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Devices;

public class SleepSoundsStateDevice extends ApiResponse {
    private SleepSoundsState sleepSoundsState;
    private Devices devices;

    public SleepSoundsStateDevice(@NonNull SleepSoundsState sleepSoundsState, @NonNull Devices devices) {
        this.sleepSoundsState = sleepSoundsState;
        this.devices = devices;
    }

    public SleepSoundsState getSleepSoundsState() {
        return sleepSoundsState;
    }

    public Devices getDevices() {
        return devices;
    }
}
