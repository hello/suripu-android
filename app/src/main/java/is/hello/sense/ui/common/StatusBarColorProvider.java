package is.hello.sense.ui.common;

import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

public interface StatusBarColorProvider {
    @ColorInt int getStatusBarColor(@NonNull Resources resources);
    void onStatusBarTransitionBegan(@ColorInt int targetColor);
    void onStatusBarTransitionEnded(@ColorInt int finalColor);
}
