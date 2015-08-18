package is.hello.sense.ui.widget.timeline;

import android.support.v4.view.ViewPager;
import android.view.View;

import is.hello.go99.Anime;

public class PerspectiveTransformer implements ViewPager.PageTransformer {
    private static final float MINIMUM_SCALE = 0.8f;
    private static final float MAXIMUM_SCALE = 1.0f;

    private static final float MINIMUM_ALPHA = 0f;
    private static final float MAXIMUM_ALPHA = 1f;

    @Override
    public void transformPage(View page, float position) {
        final float fraction = Math.abs(position);

        final float scale = Anime.interpolateFloats(fraction, MAXIMUM_SCALE, MINIMUM_SCALE);
        page.setScaleX(scale);
        page.setScaleY(scale);

        final float alpha = Anime.interpolateFloats(fraction, MAXIMUM_ALPHA, MINIMUM_ALPHA);
        page.setAlpha(alpha);
    }
}
