package is.hello.sense.ui.common;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public final class UpdateTimer {
    private final int MSG_FIRE = 0x1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long delay;

    private @Nullable Runnable onUpdate;
    private boolean scheduled = false;

    public UpdateTimer(long delay, @NonNull TimeUnit timeUnit) {
        this.delay = timeUnit.toMillis(delay);
    }


    //region Update Action

    public void fire() {
        Log.i(getClass().getSimpleName(), "fire()");

        if (onUpdate != null) {
            onUpdate.run();
        }
    }

    public void setOnUpdate(@Nullable Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    //endregion


    //region Scheduling

    public boolean isScheduled() {
        return scheduled;
    }

    public void schedule() {
        if (onUpdate == null) {
            throw new IllegalStateException("update action required");
        }

        Log.i(getClass().getSimpleName(), "schedule()");
        if (!scheduled) {
            handler.sendMessageDelayed(acquireFireMessage(), delay);
            this.scheduled = true;
        }
    }

    public void unschedule() {
        Log.i(getClass().getSimpleName(), "unschedule()");

        this.scheduled = false;
        handler.removeMessages(MSG_FIRE);
    }

    //endregion


    private Message acquireFireMessage() {
        Message fireMessage = Message.obtain(handler, () -> {
            fire();
            if (isScheduled()) {
                schedule();
            }
        });
        fireMessage.what = MSG_FIRE;
        return fireMessage;
    }
}
