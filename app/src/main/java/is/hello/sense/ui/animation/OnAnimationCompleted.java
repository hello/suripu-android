package is.hello.sense.ui.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

public interface OnAnimationCompleted {
    void onAnimationCompleted(boolean finished);

    class Adapter extends AnimatorListenerAdapter {
        private final OnAnimationCompleted onAnimationCompleted;
        private boolean canceled = false;

        Adapter(OnAnimationCompleted onAnimationCompleted) {
            this.onAnimationCompleted = onAnimationCompleted;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            this.canceled = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            onAnimationCompleted.onAnimationCompleted(!canceled);
        }
    }
}
