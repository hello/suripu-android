package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;

import is.hello.sense.util.Logger;
import is.hello.sense.util.Player;

public class DiagramVideoView extends ViewGroup implements Player.OnEventListener,
        TextureView.SurfaceTextureListener {
    private final Player player;

    private int clearColor = Color.TRANSPARENT;
    private @Nullable Drawable placeholder;

    private @Nullable SurfaceTexture surfaceTexture;
    private @Nullable Surface videoSurface;

    //region Lifecycle

    public DiagramVideoView(@NonNull Context context) {
        this(context, null);
    }

    public DiagramVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DiagramVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TextureView textureView = new TextureView(context);
        textureView.setOpaque(false);
        textureView.setSurfaceTextureListener(this);
        addView(textureView);

        this.player = new Player(context, this, null);
        player.setLooping(true);
        player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
    }

    public void destroy() {
        releaseVideoSurface();
        player.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(clearColor);
        if (placeholder != null) {
            placeholder.draw(canvas);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        if (placeholder != null) {
            placeholder.setBounds(0, 0, w, h);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (placeholder != null) {
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.AT_MOST) {
                float scaleFactor = (float) placeholder.getIntrinsicHeight() / (float) placeholder.getIntrinsicWidth();
                height = Math.round(width * scaleFactor);
            } else if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.EXACTLY) {
                float scaleFactor = (float) placeholder.getIntrinsicWidth() / (float) placeholder.getIntrinsicHeight();
                width = Math.round(height * scaleFactor);
            }
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0, size = getChildCount(); i < size; i++) {
            getChildAt(i).layout(l, t, r, b);
        }
    }

    //endregion


    //region Surfaces

    private void releaseVideoSurface() {
        if (videoSurface != null) {
            player.setSurface(null);
            videoSurface.release();
            this.videoSurface = null;
        }
    }

    private void ensureVideoSurface() {
        if (surfaceTexture == null) {
            return;
        }

        if (player.getState() >= Player.STATE_PLAYING) {
            if (videoSurface == null) {
                this.videoSurface = new Surface(surfaceTexture);
                player.setSurface(videoSurface);
            }
        }
    }

    private void clearIfNeeded() {
        if (videoSurface == null && surfaceTexture != null) {
            Surface clearSurface = new Surface(surfaceTexture);
            try {
                Canvas canvas = clearSurface.lockCanvas(null);
                canvas.drawColor(clearColor);
                clearSurface.unlockCanvasAndPost(canvas);
            } finally {
                clearSurface.release();
            }
        }
    }

    //endregion


    //region Callbacks

    @Override
    public void onPlaybackReady(@NonNull Player player) {
        if (isShown()) {
            player.startPlayback();
        }
    }

    @Override
    public void onPlaybackStarted(@NonNull Player player) {
        ensureVideoSurface();
    }

    @Override
    public void onPlaybackStopped(@NonNull Player player, boolean finished) {
        ensureVideoSurface();
    }

    @Override
    public void onPlaybackError(@NonNull Player player, @NonNull Throwable error) {
        ensureVideoSurface();
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Logger.debug(getClass().getSimpleName(), "onSurfaceTextureAvailable(" + surfaceTexture + ", " + width + ", " + height + ")");

        this.surfaceTexture = surfaceTexture;
        clearIfNeeded();

        player.startPlayback();
        ensureVideoSurface();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Logger.debug(getClass().getSimpleName(), "onSurfaceTextureSizeChanged(" + surfaceTexture + ", " + width + ", " + height + ")");
        clearIfNeeded();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Logger.debug(getClass().getSimpleName(), "onSurfaceTextureDestroyed(" + surfaceTexture + ")");

        player.pausePlayback();
        releaseVideoSurface();

        this.surfaceTexture = null;

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    //endregion


    public void setClearColor(int clearColor) {
        this.clearColor = clearColor;
        clearIfNeeded();
    }

    public void setPlaceholder(@DrawableRes int drawableRes) {
        final Drawable drawable = ResourcesCompat.getDrawable(getResources(), drawableRes, null);
        setPlaceholder(drawable);
    }

    public void setPlaceholder(@Nullable Drawable placeholder) {
        if (this.placeholder != null) {
            this.placeholder.setCallback(null);
        }

        this.placeholder = placeholder;

        if (placeholder != null) {
            placeholder.setCallback(this);
            placeholder.setBounds(0, 0, getWidth(), getHeight());
        }
        setWillNotDraw(placeholder == null);
        invalidate();
    }

    public void setDataSource(@NonNull Uri source) {
        player.setDataSource(source, false);
    }
}
