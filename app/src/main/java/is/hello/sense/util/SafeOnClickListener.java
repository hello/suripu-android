package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;

/**
 * A wrapper around {@see android.view.View.OnClickListener} that incorporates dispatch
 * rate limiting to prevent the user from trigger the same action more than once by mistake.
 */
public class SafeOnClickListener implements View.OnClickListener {
    private final View.OnClickListener target;
    private long lastInvocation;

    public SafeOnClickListener(@NonNull View.OnClickListener target) {
        this.target = target;
    }

    @Override
    public void onClick(View view) {
        if ((AnimationUtils.currentAnimationTimeMillis() - lastInvocation) > ViewConfiguration.getDoubleTapTimeout()) {
            target.onClick(view);
            this.lastInvocation = System.currentTimeMillis();
        }
    }
}
