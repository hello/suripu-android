package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.stacks.util.Recyclable;
import is.hello.sense.bluetooth.stacks.util.TakesOwnership;
import rx.Scheduler;
import rx.functions.Action0;

/**
 * An opaque interface to be implemented by clients of the stack.
 * <p/>
 * The stack will {@see #schedule} a timeout when it begins an operation,
 * and {@see #unschedule} it either when the task completes, or fails.
 * <p/>
 * <em>Important:</em> OperationTimeout implementations are not guaranteed to be thread-safe.
 */
public interface OperationTimeout extends Recyclable {
    public static final String LOG_TAG = "Bluetooth." + OperationTimeout.class.getSimpleName();

    /**
     * Called by the bluetooth stack. Schedules a timeout timer.
     * <p/>
     * This method is not assumed to be safe to call until after {@see #setTimeoutAction} is called.
     */
    void schedule();

    /**
     * Called by the bluetooth stack. Unschedules the timeout timer.
     * <p/>
     * It is a valid condition for this method to be called multiple times,
     * it should become a no-op after the first call.
     * <p/>
     * This method is not assumed to be safe to call until after {@see #setTimeoutAction} is called.
     */
    void unschedule();

    /**
     * For use by clients. Unschedules and reschedules the timeout timer.
     * <p/>
     * This method is not assumed to be safe to call until after {@see #setTimeoutAction} is called.
     */
    void reschedule();

    /**
     * Called by the bluetooth stack. Specifies an action to run when
     * the timeout expires that will allow the stack to clean up any
     * resources. The client should unschedule and recycle the timeout
     * after it has finished running its clean up code.
     * <p/>
     * Client code should check the state of a peripheral after a timeout
     * has expired. It is implementation-specific what state the peripheral
     * will be in after a timeout.
     * @param action    Stack specific logic to handle the timeout.
     * @param scheduler The scheduler to run the handler on.
     */
    void setTimeoutAction(@NonNull Action0 action, @NonNull Scheduler scheduler);

    /**
     * Called by the bluetooth stack. Clears all state associated with the timeout operation.
     * <p/>
     * It is an error for this method to be called on a scheduled timeout.
     */
    void recycle();


    /**
     * Operation timeouts are intended to be kept in a finite pool associated with
     * an instance of the {@see is.hello.sense.bluetooth.stacks.Peripheral} interface.
     */
    public interface Pool {
        /**
         * The recommended capacity for implementations of pool.
         */
        public static final int RECOMMENDED_CAPACITY = 2;

        /**
         * Vends an operation timeout object for use with a Peripheral created by this stack.
         * <p/>
         * The callee becomes responsible for either calling {@see #recycle}, or for passing
         * the OperationTimeout onto a method/object that {@see TakesOwnership} of it.
         * @param name      The name of the timeout.
         * @param duration  The duration of the timeout.
         * @param timeUnit  The unit of the duration.
         * @return An object implementing OperationTimeout ready for use.
         */
        @TakesOwnership
        OperationTimeout acquire(@NonNull String name, long duration, TimeUnit timeUnit);
    }
}
