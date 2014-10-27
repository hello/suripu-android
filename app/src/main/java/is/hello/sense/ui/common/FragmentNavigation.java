package is.hello.sense.ui.common;

import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface FragmentNavigation {
    void showFragment(@NonNull Fragment fragment,
                      @Nullable String title,
                      boolean wantsBackStackEntry);
}
