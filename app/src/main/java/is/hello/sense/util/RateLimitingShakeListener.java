package is.hello.sense.util;

import android.support.annotation.NonNull;

import com.squareup.seismic.ShakeDetector;

public class RateLimitingShakeListener implements ShakeDetector.Listener {
    private final Runnable action;
    private long lastShakeTime = 0;

    public RateLimitingShakeListener(@NonNull Runnable action) {
        this.action = action;
    }

    @Override
    public final void hearShake() {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastShakeTime) > 1000) {
            action.run();
            this.lastShakeTime = currentTime;
        }
    }
}
