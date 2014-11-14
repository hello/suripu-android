package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import rx.subjects.ReplaySubject;

public class TestBluetoothStackConfig {
    final List<Peripheral> peripheralsInRange = new ArrayList<>();
    final ReplaySubject<Boolean> enabled = ReplaySubject.createWithSize(1);
    long latency;

    public TestBluetoothStackConfig() {
        this.enabled.onNext(true);
    }

    public TestBluetoothStackConfig addPeripheralInRange(@NonNull BluetoothStack stack, @NonNull TestPeripheralConfig config) {
        peripheralsInRange.add(new TestPeripheral(stack, config));
        return this;
    }

    public TestBluetoothStackConfig withEnabled(boolean enabled) {
        this.enabled.onNext(enabled);
        return this;
    }

    public TestBluetoothStackConfig withLatency(long latency) {
        this.latency = latency;
        return this;
    }

    public void reset() {
        this.peripheralsInRange.clear();
        this.enabled.onNext(true);
        this.latency = 0;
    }
}
