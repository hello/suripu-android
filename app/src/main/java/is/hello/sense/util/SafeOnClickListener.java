package is.hello.sense.util;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.View;

/**
 * A wrapper around {@see android.view.View.OnClickListener} that incorporates dispatch
 * rate limiting to prevent the user from trigger the same action more than once by mistake.
 */
public class SafeOnClickListener implements View.OnClickListener {
    private static final long ENABLE_DELAY_MS = 50;
    private static final Handler ENABLE_DEFER_HANDLER = new Handler(Looper.getMainLooper());
    private final View.OnClickListener target;

    private boolean enabled = true;

    public SafeOnClickListener(@NonNull View.OnClickListener target) {
        this.target = target;
    }

    @Override
    public void onClick(View view) {
        if (enabled) {
            target.onClick(view);
            disable();
        }
    }


    /**
     * Disables click forwarding on the listener, scheduling it to
     * be automatically re-enabled after {@see #ENABLE_DELAY_MS}.
     */
    public void disable() {
        this.enabled = false;
        ENABLE_DEFER_HANDLER.postDelayed(this::enable, ENABLE_DELAY_MS);
    }

    /**
     * Re-enables click forwarding on the listener
     */
    public void enable() {
        this.enabled = true;
    }
}
