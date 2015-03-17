package is.hello.sense.ui.common;

import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface FragmentNavigation {
    void pushFragment(@NonNull Fragment fragment,
                      @Nullable String title,
                      boolean wantsBackStackEntry);

    void popFragment(@NonNull Fragment fragment,
                     boolean immediate);

    @Nullable Fragment getTopFragment();

    interface BackInterceptingFragment {
        boolean onInterceptBack(@NonNull Runnable back);
    }
}
