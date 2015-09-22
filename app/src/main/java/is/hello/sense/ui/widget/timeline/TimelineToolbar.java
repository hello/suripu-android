package is.hello.sense.ui.widget.timeline;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class TimelineToolbar extends RelativeLayout {
    private final ImageButton overflow;
    private final ImageButton share;
    private final TextView title;

    private final TransitionDrawable overflowFadeDrawable;
    private final LayerDrawable overflowClosedIcon;
    private boolean overflowOpen;

    private @Nullable ValueAnimator titleColorAnimator;
    private boolean titleDimmed;

    private boolean shareVisible;
    private boolean unreadVisible;


    //region Lifecycle

    public TimelineToolbar(@NonNull Context context) {
        this(context, null);
    }

    public TimelineToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimelineToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setBackgroundResource(R.color.background_timeline);

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.view_timeline_toolbar, this, true);

        this.overflow = (ImageButton) findViewById(R.id.view_timeline_toolbar_overflow);
        this.share = (ImageButton) findViewById(R.id.view_timeline_toolbar_share);
        this.title = (TextView) findViewById(R.id.view_timeline_toolbar_title);

        final Resources resources = getResources();
        this.overflowClosedIcon = (LayerDrawable) ResourcesCompat.getDrawable(resources, R.drawable.icon_menu_closed_unread, null);
        final Drawable overflowOpenIcon = ResourcesCompat.getDrawable(resources, R.drawable.icon_menu_open, null);
        final Drawable[] overflowDrawables = { overflowClosedIcon, overflowOpenIcon, };
        this.overflowFadeDrawable = new TransitionDrawable(overflowDrawables);
        overflowFadeDrawable.setCrossFadeEnabled(true);
        overflow.setImageDrawable(overflowFadeDrawable);

        share.setVisibility(INVISIBLE);

        setUnreadVisible(false);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility != VISIBLE) {
            clearAnimation();
        }
    }

    //endregion


    //region Attributes

    @Override
    public void clearAnimation() {
        super.clearAnimation();

        if (titleColorAnimator != null) {
            titleColorAnimator.cancel();
        }
        Anime.cancelAll(share);
    }

    public void setOverflowOnClickListener(@NonNull OnClickListener onClickListener) {
        Views.setSafeOnClickListener(overflow, onClickListener);
    }

    public void setOverflowOpen(boolean overflowOpen) {
        if (overflowOpen == this.overflowOpen) {
            return;
        }

        this.overflowOpen = overflowOpen;

        final int duration;
        if (!ViewCompat.isAttachedToWindow(this)) {
            duration = 0;
        } else {
            duration = Anime.DURATION_FAST;
        }

        if (overflowOpen) {
            overflowFadeDrawable.startTransition(duration);
        } else {
            overflowFadeDrawable.reverseTransition(duration);
        }
    }

    public void setUnreadVisible(boolean unreadVisible) {
        final Drawable unreadLayer = overflowClosedIcon.findDrawableByLayerId(R.id.icon_menu_closed_unread_indicator);
        if (unreadVisible) {
            unreadLayer.setAlpha(255);
        } else {
            unreadLayer.setAlpha(0);
        }
    }

    public void setShareOnClickListener(@NonNull OnClickListener onClickListener) {
        Views.setSafeOnClickListener(share, onClickListener);
    }

    public void setShareVisible(boolean shareVisible) {
        if (shareVisible == this.shareVisible) {
            return;
        }

        this.shareVisible = shareVisible;

        if (!ViewCompat.isAttachedToWindow(this)) {
            if (shareVisible) {
                share.setVisibility(VISIBLE);
            } else {
                share.setVisibility(INVISIBLE);
            }
            return;
        }

        if (shareVisible) {
            animatorFor(share)
                    .withDuration(Anime.DURATION_FAST)
                    .fadeIn()
                    .addOnAnimationCompleted(finished -> {
                        if (!finished) {
                            share.setAlpha(1f);
                            share.setVisibility(VISIBLE);
                        }
                    })
                    .start();
        } else {
            animatorFor(share)
                    .withDuration(Anime.DURATION_FAST)
                    .fadeOut(INVISIBLE)
                    .addOnAnimationCompleted(finished -> {
                        if (!finished) {
                            share.setAlpha(0f);
                            share.setVisibility(INVISIBLE);
                        }
                    })
                    .start();
        }
    }

    public void setTitleOnClickListener(@NonNull OnClickListener onClickListener) {
        Views.setSafeOnClickListener(title, onClickListener);
    }

    public void setTitle(@Nullable CharSequence text) {
        title.setText(text);
    }

    public void setTitleDimmed(boolean titleDimmed) {
        if (titleDimmed == this.titleDimmed) {
            return;
        }

        if (titleColorAnimator != null) {
            titleColorAnimator.cancel();
        }

        this.titleDimmed = titleDimmed;

        final int startColor = title.getCurrentTextColor();
        final int endColor;
        if (titleDimmed) {
            endColor = getResources().getColor(R.color.text_dim);
        } else {
            endColor = getResources().getColor(R.color.text_dark);
        }

        if (!ViewCompat.isAttachedToWindow(this)) {
            title.setTextColor(endColor);
            return;
        }

        this.titleColorAnimator = AnimatorTemplate.DEFAULT.createColorAnimator(startColor, endColor);
        titleColorAnimator.setDuration(Anime.DURATION_FAST);
        titleColorAnimator.addUpdateListener(a -> {
            int color = (int) a.getAnimatedValue();
            title.setTextColor(color);
        });
        titleColorAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                title.setTextColor(endColor);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (titleColorAnimator == animation) {
                    TimelineToolbar.this.titleColorAnimator = null;
                }
            }
        });
        titleColorAnimator.start();
    }

    //endregion
}
