package is.hello.sense.ui.common;

import android.support.annotation.NonNull;

public interface OnBackPressedInterceptor {
    boolean onInterceptBackPressed(@NonNull Runnable defaultBehavior);
}
