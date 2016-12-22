package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;

/**
 * A wrapper around {@see android.view.View.OnClickListener} that incorporates dispatch
 * rate limiting to prevent the user from trigger the same action more than once by mistake.
 */
public class SafeOnClickListener implements View.OnClickListener {
    private final @Nullable StateSafeExecutor stateSafeExecutor;
    private final @NonNull View.OnClickListener target;
    private long lastInvocation;

    public SafeOnClickListener(@Nullable final StateSafeExecutor stateSafeExecutor,
                               @NonNull final View.OnClickListener target) {
        this.target = target;
        this.stateSafeExecutor = stateSafeExecutor;
    }

    @Override
    public void onClick(final View view) {
        if ((AnimationUtils.currentAnimationTimeMillis() - lastInvocation) > ViewConfiguration.getDoubleTapTimeout()) {
            if (stateSafeExecutor != null) {
                stateSafeExecutor.execute(() -> target.onClick(view));
            } else {
                target.onClick(view);
            }
            this.lastInvocation = AnimationUtils.currentAnimationTimeMillis();
        }
    }
}
