package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SpinnerImageView extends ImageView {
    private int deltaRotation = 5; // degrees
    private int spinnerInterval = 1; // ms
    private boolean wantsToSpin = false;
    final Runnable spinningRunnable = new Runnable() {
        @Override
        public void run() {
            if (wantsToSpin) {
                setRotation(getRotation() + deltaRotation);
                postDelayed(this, spinnerInterval);
            } else {
                setRotation(0);
            }
        }
    };

    public SpinnerImageView(final @NonNull Context context) {
        this(context, null);
    }

    public SpinnerImageView(final @NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpinnerImageView(final @NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void startSpinning() {
        wantsToSpin = true;
        this.post(spinningRunnable);
    }

    public void stopSpinning() {
        wantsToSpin = false;
        this.removeCallbacks(spinningRunnable);
    }

    public void setSpinnerInterval(int speed) {
        if (speed < 0) {
            speed = 0;
        }
        spinnerInterval = speed;
    }

    public void setDeltaRotation(final int rotation) {
        deltaRotation = rotation;
    }


}
