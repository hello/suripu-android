package is.hello.sense.ui.common;

import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

public interface StatusBarColorProvider {
    @ColorInt int getStatusBarColor(@NonNull Resources resources);
    void onStatusBarTransitionBegan(@ColorInt int targetColor);
    void onStatusBarTransitionEnded(@ColorInt int finalColor);

    class EmptyProvider implements StatusBarColorProvider {

        public static EmptyProvider newInstance(){
            return new EmptyProvider();
        }

        @Override
        public int getStatusBarColor(@NonNull final Resources resources) {
            return 0;
        }

        @Override
        public void onStatusBarTransitionBegan(@ColorInt final int targetColor) {
            //do nothing
        }

        @Override
        public void onStatusBarTransitionEnded(@ColorInt final int finalColor) {
            //do nothing
        }
    }
}
