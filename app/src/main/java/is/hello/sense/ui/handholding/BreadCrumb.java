package is.hello.sense.ui.handholding;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import is.hello.sense.R;


@SuppressLint("ViewConstructor")
public class BreadCrumb extends RelativeLayout {
    private final WindowManager windowManager;

    private BreadCrumb(@NonNull final Activity activity, final float x, final float y) {
        super(activity);
        final Bitmap breadCrumbBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.unread_indicator);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        windowManager = (WindowManager) activity.getBaseContext().getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.FIRST_SUB_WINDOW);
        layoutParams.width = breadCrumbBitmap.getWidth();
        layoutParams.height = breadCrumbBitmap.getHeight();
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = (int) x - breadCrumbBitmap.getWidth() / 2;
        layoutParams.y = (int) y - breadCrumbBitmap.getHeight() / 2;
        layoutParams.format = PixelFormat.RGBA_8888;
        setBackground(new BitmapDrawable(getResources(), breadCrumbBitmap));
        layoutParams.flags =
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | PixelFormat.TRANSLUCENT;
        layoutParams.token = activity.getWindow().getDecorView().getRootView().getWindowToken();
        windowManager.addView(this, layoutParams);
    }

    public void destroy() {
        if (windowManager != null) {
            windowManager.removeView(this);
        }
    }

    private static abstract class Builder {
        protected final Activity activity;
        protected float dX = 0;
        protected float dY = 0;
        protected BreadCrumb breadCrumb = null;

        public Builder(@NonNull Activity activity) {
            this.activity = activity;
        }

        public abstract BreadCrumb build();

        public void destroyBreadCrumb() {
            if (breadCrumb != null) {
                breadCrumb.destroy();
                breadCrumb = null;
            }
        }

        public boolean isShowing() {
            return breadCrumb != null;
        }

    }
/*
    // todo finish this class when needed
    public static class TargetBuilder extends Builder {
        @IdRes
        private int targetId;

        public TargetBuilder(@NonNull Activity activity) {
            super(activity);
        }


        public TargetBuilder setTarget(@IdRes int targetId) {
            this.targetId = targetId;
            return this;
        }

        public TargetBuilder offsetX(float distance) {
            this.dX = distance;
            return this;
        }

        public TargetBuilder offsetY(float distance) {
            this.dY = distance;
            return this;
        }

        public void build(@NonNull ViewGroup view) {
            View target = view.findViewById(targetId);
            if (target == null) {
                throw new Error("BreadCrumb target: " + targetId + " not found");
            }

        }
    }
*/

    public static class CoordinateBuilder extends Builder {
        private float targetX;
        private float targetY;

        public CoordinateBuilder(@NonNull final Activity activity) {
            super(activity);
        }

        public CoordinateBuilder setTarget(final float targetX, final float targetY) {
            this.targetX = targetX;
            this.targetY = targetY;
            return this;
        }

        public CoordinateBuilder offsetX(final float distance) {
            this.dX = distance;
            return this;
        }

        public CoordinateBuilder offsetY(final float distance) {
            this.dY = distance;
            return this;
        }

        public void moveTo(float x, float y){
            if (breadCrumb != null){
                WindowManager.LayoutParams layoutParams =(WindowManager.LayoutParams) breadCrumb.getLayoutParams();
                layoutParams.x = (int)x;
                layoutParams.x = (int)y;
            }
        }

        public BreadCrumb build() {
            destroyBreadCrumb();
            this.breadCrumb = new BreadCrumb(activity, targetX + dX, targetY + dY);
            return breadCrumb;
        }
    }


    public interface Listener {
        void refresh(boolean forceReset);

        void removeBreadCrumb();
    }

}
