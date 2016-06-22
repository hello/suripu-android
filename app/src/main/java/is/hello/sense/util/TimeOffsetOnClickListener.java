package is.hello.sense.util;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;

/**
 * Wrapper around {@link OnClickListener} that prevents other instances of {@link TimeOffsetOnClickListener}
 * from triggering their onClick methods too close to each other.
 */
public class TimeOffsetOnClickListener implements OnClickListener {
    private static Long LAST_CLICKED = 0L;
    private final OnClickListener target;

    public TimeOffsetOnClickListener(@NonNull final OnClickListener target){
        this.target = target;
    }

    @Override
    public void onClick(@NonNull final View view) {
        if ((AnimationUtils.currentAnimationTimeMillis() - LAST_CLICKED) > ViewConfiguration.getDoubleTapTimeout()) {
            target.onClick(view);
            LAST_CLICKED = AnimationUtils.currentAnimationTimeMillis();
        }
    }
}
