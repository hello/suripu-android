package is.hello.sense.ui.widget.timeline;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    private final ImageButton history;
    private final ImageButton share;
    private final TextView title;

    private @Nullable ValueAnimator titleColorAnimator;
    private boolean titleDimmed;

    private boolean shareVisible;


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

        this.history = (ImageButton) findViewById(R.id.view_timeline_toolbar_history);
        this.share = (ImageButton) findViewById(R.id.view_timeline_toolbar_share);
        this.title = (TextView) findViewById(R.id.view_timeline_toolbar_title);

        share.setVisibility(INVISIBLE);
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

    public void setHistoryOnClickListener(@NonNull final OnClickListener onClickListener) {
        Views.setSafeOnClickListener(history, onClickListener);
    }

    public void setShareOnClickListener(@NonNull final OnClickListener onClickListener) {
        Views.setSafeOnClickListener(share, onClickListener);
    }

    public void setShareVisible(final boolean shareVisible) {
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
