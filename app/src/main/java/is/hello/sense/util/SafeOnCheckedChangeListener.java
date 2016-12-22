package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;

/**
 * A wrapper around {@see android.view.CompoundButton.OnCheckedChangeListener} that incorporates dispatch
 * rate limiting to prevent the user from trigger the same action more than once by mistake.
 */
public class SafeOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
    private final @Nullable StateSafeExecutor stateSafeExecutor;
    private final @NonNull
    CompoundButton.OnCheckedChangeListener target;
    private long lastInvocation;

    public SafeOnCheckedChangeListener(@Nullable final StateSafeExecutor stateSafeExecutor,
                                       @NonNull final CompoundButton.OnCheckedChangeListener target) {
        this.target = target;
        this.stateSafeExecutor = stateSafeExecutor;
    }

    @Override
    public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
        if ((AnimationUtils.currentAnimationTimeMillis() - lastInvocation) > ViewConfiguration.getDoubleTapTimeout()) {
            if (stateSafeExecutor != null) {
                stateSafeExecutor.execute(() -> target.onCheckedChanged(compoundButton, isChecked));
            } else {
                target.onCheckedChanged(compoundButton, isChecked);
            }
            this.lastInvocation = AnimationUtils.currentAnimationTimeMillis();
        }
    }
}
