package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

import rx.Scheduler;
import rx.functions.Action0;

public final class TestOperationTimeout implements OperationTimeout {
    private Action0 action;
    private Scheduler scheduler;

    private boolean wasScheduled = false;
    private boolean wasUnscheduled = false;

    private String name;

    public static TestOperationTimeout acquire(@NonNull String name) {
        return new TestOperationTimeout(name);
    }

    private TestOperationTimeout(@NonNull String name) {
        this.name = name;
    }


    @Override
    public void schedule() {
        if (action == null || scheduler == null) {
            throw new IllegalStateException("Cannot schedule a time out that has no action");
        }

        this.wasScheduled = true;
    }

    @Override
    public void unschedule() {
        this.wasUnscheduled = true;
    }

    @Override
    public void reschedule() {
        unschedule();
        schedule();
    }

    @Override
    public void setTimeoutAction(@NonNull Action0 action, @NonNull Scheduler scheduler) {
        this.action = action;
        this.scheduler = scheduler;
    }


    public String getName() {
        return name;
    }

    public boolean wasScheduled() {
        return wasScheduled;
    }

    public boolean wasUnscheduled() {
        return wasUnscheduled;
    }

    public boolean isSetup() {
        return (action != null && scheduler != null);
    }

    public void fire() {
        if (action == null || scheduler == null) {
            throw new NullPointerException();
        }

        scheduler.createWorker().schedule(action);
    }

    @Override
    public String toString() {
        return "TestOperationTimeout{" +
                "action=" + action +
                ", scheduler=" + scheduler +
                ", name='" + name + '\'' +
                ", wasScheduled=" + wasScheduled +
                ", wasUnscheduled=" + wasUnscheduled +
                '}';
    }
}
