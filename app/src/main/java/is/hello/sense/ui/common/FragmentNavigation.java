package is.hello.sense.ui.common;

import android.app.Fragment;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface FragmentNavigation {
    void pushFragment(@NonNull Fragment fragment,
                      @Nullable String title,
                      boolean wantsBackStackEntry);

    void pushFragmentAllowingStateLoss(@NonNull Fragment fragment,
                                       @Nullable String title,
                                       boolean wantsBackStackEntry);

    void popFragment(@NonNull Fragment fragment,
                     boolean immediate);

    //todo only for mvvm testing because view models don't keep reference to fragment
    void flowFinished(@Nullable Fragment fragment,
                      int responseCode,
                      @Nullable Intent result);

    @Nullable Fragment getTopFragment();
}
