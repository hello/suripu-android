package is.hello.sense.flows.home.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.graphing.drawables.SleepScoreIconDrawable;

public class SenseTabView extends FrameLayout {

    @NonNull
    private final ImageView imageView;

    @NonNull
    private final TextView indicator;

    @Nullable
    public Drawable activeDrawable = null;

    @Nullable
    public Drawable normalDrawable = null;

    public SenseTabView(final Context context) {
        this(context, null, 0);
    }

    public SenseTabView(final Context context,
                        final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SenseTabView(final Context context,
                        final AttributeSet attrs,
                        final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_tab, this);
        this.imageView = (ImageView) findViewById(R.id.view_tab_icon);
        this.indicator = (TextView) findViewById(R.id.view_tab_indicator);
    }

    public SenseTabView setActive(final boolean active) {
        if (active) {
            this.indicator.setVisibility(GONE);
            this.imageView.setBackground(this.activeDrawable);
        } else {
            this.imageView.setBackground(this.normalDrawable);
        }
        return this;
    }

    public void setIndicatorVisible(final boolean visible) {
        this.indicator.setVisibility(visible ? VISIBLE : GONE);
    }

    public SenseTabView setDrawables(@DrawableRes final int normal,
                                     @DrawableRes final int active) {
        return setDrawables(ContextCompat.getDrawable(getContext(), normal),
                            ContextCompat.getDrawable(getContext(), active));
    }

    public SenseTabView setDrawables(@NonNull final Drawable normal,
                                     @NonNull final Drawable active) {
        this.normalDrawable = normal;
        this.activeDrawable = active;
        return this;
    }

    public SenseTabView useSleepScoreIcon(@Nullable final Integer score) {
        updateSleepScoreIcon(score);
        return this;
    }

    private void updateSleepScoreIcon(@Nullable final Integer score) {
        useSleepScoreIcon(score,
                          this.imageView.getMinimumWidth(),
                          this.imageView.getMinimumHeight());
    }

    private void useSleepScoreIcon(@Nullable final Integer score,
                                   final int width,
                                   final int height) {
        final SleepScoreIconDrawable.Builder builder = new SleepScoreIconDrawable.Builder(getContext());
        if (score != null) {
            builder.withText(score);
        }
        builder.withSize(width,
                         height);
        this.normalDrawable = builder.build();
        this.activeDrawable = builder.withSelected(true).build();
    }


}
