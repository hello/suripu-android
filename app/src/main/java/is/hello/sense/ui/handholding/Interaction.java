package is.hello.sense.ui.handholding;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;

import java.io.Serializable;

public class Interaction implements Serializable {
    public final @IdRes int anchorViewRes;
    public final Type type;

    public Interaction(@NonNull Type type,
                       @IdRes int anchorViewRes) {
        this.type = type;
        this.anchorViewRes = anchorViewRes;
    }


    public static enum Type {
        TAP,
        SLIDE_LEFT,
        SLIDE_RIGHT,
        SLIDE_UP,
        SLIDE_DOWN,
    }
}
