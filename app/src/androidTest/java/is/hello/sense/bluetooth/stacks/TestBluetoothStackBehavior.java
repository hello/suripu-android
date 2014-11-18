package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import rx.subjects.ReplaySubject;

public class TestBluetoothStackBehavior {
    final List<Peripheral> peripheralsInRange = new ArrayList<>();
    final ReplaySubject<Boolean> enabled = ReplaySubject.createWithSize(1);
    long latency;

    public TestBluetoothStackBehavior() {
        this.enabled.onNext(true);
    }

    public TestBluetoothStackBehavior addPeripheralInRange(@NonNull BluetoothStack stack, @NonNull TestPeripheralBehavior config) {
        peripheralsInRange.add(new TestPeripheral(stack, config));
        return this;
    }

    public TestBluetoothStackBehavior setEnabled(boolean enabled) {
        this.enabled.onNext(enabled);
        return this;
    }

    public TestBluetoothStackBehavior setLatency(long latency) {
        this.latency = latency;
        return this;
    }

    public void reset() {
        this.peripheralsInRange.clear();
        this.enabled.onNext(true);
        this.latency = 0;
    }
}
