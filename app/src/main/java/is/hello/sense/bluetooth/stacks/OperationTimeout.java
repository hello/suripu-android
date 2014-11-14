package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;

import is.hello.sense.bluetooth.stacks.util.Recyclable;
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
     * resources.
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
}
