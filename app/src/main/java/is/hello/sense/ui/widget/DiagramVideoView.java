package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Player;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class DiagramVideoView extends FrameLayout implements Player.OnEventListener,
        TextureView.SurfaceTextureListener {
    private final Player player;
    private final ProgressBar loadingIndicator;

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

        setSaveEnabled(false);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

        final TextureView textureView = new TextureView(context);
        textureView.setOpaque(false);
        textureView.setSurfaceTextureListener(this);
        addView(textureView, new LayoutParams(LayoutParams.MATCH_PARENT,
                                              LayoutParams.MATCH_PARENT));

        this.player = new Player(context, this, null);
        player.setLooping(true);
        player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

        this.loadingIndicator = new ProgressBar(context);
        final Drawable indeterminateDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.animated_progress_bar_small_grey, null);
        loadingIndicator.setIndeterminateDrawable(indeterminateDrawable);
        loadingIndicator.setIndeterminate(true);
        final LayoutParams loadingIndicatorLayoutParams = new LayoutParams(indeterminateDrawable.getIntrinsicWidth(),
                                                                           indeterminateDrawable.getIntrinsicHeight(),
                                                                           Gravity.TOP | Gravity.END);
        final int loadingIndicatorInset = getResources().getDimensionPixelSize(R.dimen.gap_small);
        loadingIndicatorLayoutParams.topMargin = loadingIndicatorInset;
        loadingIndicatorLayoutParams.setMarginEnd(loadingIndicatorInset);
        addView(loadingIndicator, loadingIndicatorLayoutParams);


        if (attrs != null) {
            final TypedArray styles =
                    context.obtainStyledAttributes(attrs, R.styleable.DiagramVideoView, defStyleAttr, 0);

            final Drawable placeholder = styles.getDrawable(R.styleable.DiagramVideoView_sensePlaceholder);
            setPlaceholder(placeholder);

            final String video = styles.getString(R.styleable.DiagramVideoView_senseDiagramVideo);
            if (!TextUtils.isEmpty(video)) {
                setDataSource(Uri.parse(video));
            }

            final boolean wantsLoadingIndicator =
                    styles.getBoolean(R.styleable.DiagramVideoView_senseWantsLoadingIndicator, true);
            setWantsLoadingIndicator(wantsLoadingIndicator);

            styles.recycle();
        }
    }

    public void destroy() {
        Logger.debug(getClass().getSimpleName(), "destroy()");

        releaseVideoSurface();
        player.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (placeholder != null) {
            placeholder.draw(canvas);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        if (placeholder != null) {
            placeholder.setBounds(getPaddingLeft(), getPaddingTop(),
                                  w - getPaddingRight(), h - getPaddingBottom());
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (placeholder != null) {
            int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                final float scaleFactor = (float) placeholder.getIntrinsicHeight() / (float) placeholder.getIntrinsicWidth();
                final int effectiveWidth = width + getPaddingLeft() + getPaddingRight();
                height = Math.round(effectiveWidth * scaleFactor);
                heightMode = MeasureSpec.EXACTLY;
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                final float scaleFactor = (float) placeholder.getIntrinsicWidth() / (float) placeholder.getIntrinsicHeight();
                final int effectiveHeight = height + getPaddingTop() + getPaddingBottom();
                width = Math.round(effectiveHeight * scaleFactor);
                widthMode = MeasureSpec.EXACTLY;
            }

            final int adjustedWidthSpec = MeasureSpec.makeMeasureSpec(width, widthMode);
            final int adjustedHeightSpec = MeasureSpec.makeMeasureSpec(height, heightMode);
            super.onMeasure(adjustedWidthSpec, adjustedHeightSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    //endregion


    //region Surfaces

    private void releaseVideoSurface() {
        if (videoSurface != null) {
            player.setVideoSurfaceAsync(null);
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
                player.setVideoSurfaceAsync(videoSurface);
            }
        }
    }

    private void clearIfNeeded() {
        if (videoSurface == null && surfaceTexture != null) {
            Surface clearSurface = new Surface(surfaceTexture);
            try {
                Canvas canvas = clearSurface.lockCanvas(null);
                canvas.drawColor(Color.TRANSPARENT);
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
            if (loadingIndicator.getVisibility() == VISIBLE) {
                animatorFor(loadingIndicator)
                        .setDuration(Anime.DURATION_FAST)
                        .fadeOut(GONE)
                        .start();
            }

            player.startPlayback();
        } else {
            Anime.cancelAll(loadingIndicator);
            loadingIndicator.setVisibility(GONE);
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


    //region Attributes

    public void setWantsLoadingIndicator(boolean wantsLoadingIndicator) {
        Anime.cancelAll(loadingIndicator);

        if (wantsLoadingIndicator) {
            if (player.getState() >= Player.STATE_LOADED) {
                loadingIndicator.setVisibility(GONE);
            } else {
                loadingIndicator.setAlpha(1f);
                loadingIndicator.setVisibility(VISIBLE);
            }
        } else {
            loadingIndicator.setVisibility(GONE);
        }
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
            placeholder.setBounds(getPaddingLeft(), getPaddingTop(),
                                  getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        }
        setWillNotDraw(placeholder == null);
        invalidate();
    }

    public void setDataSource(@NonNull Uri source) {
        player.setDataSource(source, false);
    }

    //endregion
}
