package is.hello.sense.util;

import android.support.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * An executor that is paired with an activity or fragment
 * that will defer work when its companion object is paused.
 */
public class StateSafeExecutor implements Executor {
    private final Resumes host;
    private final Queue<Runnable> pending = new ArrayDeque<>(8); //Synchronize all access to this!

    /**
     * Constructs a state-safe executor that will
     * execute when a given host is not paused.
     */
    public StateSafeExecutor(@NonNull Resumes host) {
        this.host = host;
    }

    /**
     * Executes any pending work in the executor in
     * response to its companion object resuming.
     */
    public void executePendingForResume() {
        synchronized(pending) {
            Runnable runnable;
            while (host.isResumed() && (runnable = pending.poll()) != null) {
                runnable.run();
            }
        }
    }

    /**
     * Runs a given unit of work immediately if the executor's
     * companion object is not currently paused; enqueues the
     * work to be run on resume otherwise.
     */
    @Override
    public void execute(@NonNull Runnable command) {
        if (host.isResumed()) {
            command.run();
        } else {
            synchronized(pending) {
                pending.offer(command);
            }
        }
    }

    /**
     * Removes a given unit of work from the executor's pending queue.
     * @return true if the unit of work was enqueued and removed; false otherwise.
     */
    public boolean cancelPending(@NonNull Runnable command) {
        synchronized(pending) {
            return pending.remove(command);
        }
    }

    /**
     * Removes all pending units of work from the executor.
     */
    public void clearPending() {
        synchronized(pending) {
            pending.clear();
        }
    }

    /**
     * Binds a given unit of work to always be run through this state safe executor.
     */
    public @NonNull Runnable bind(@NonNull Runnable command) {
        return () -> execute(command);
    }


    /**
     * To be implemented by companion objects.
     * <p/>
     * Companion objects should also invoke {@link StateSafeExecutor#executePendingForResume()}
     * in their implementation of <code>void onResume()</code> so that any work enqueued
     * while the object is paused gets executed.
     */
    public interface Resumes {
        /**
         * Returns whether or not the companion object is currently resumed.
         */
        boolean isResumed();
    }
}
