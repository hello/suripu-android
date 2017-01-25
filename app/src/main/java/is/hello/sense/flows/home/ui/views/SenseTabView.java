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

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.interactors.TimelineInteractor;
import is.hello.sense.ui.widget.graphing.drawables.SleepScoreIconDrawable;

public class SenseTabView extends FrameLayout {

    @NonNull
    private final ImageView imageView;

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

    }

    public SenseTabView setActive(final boolean active) {
        if (active) {
            this.imageView.setBackground(this.activeDrawable);
        } else {
            this.imageView.setBackground(this.normalDrawable);
        }
        return this;
    }

    public SenseTabView setDrawables(@DrawableRes final int normal,
                                     @DrawableRes final int active) {
        this.normalDrawable = ContextCompat.getDrawable(getContext(), normal);
        this.activeDrawable = ContextCompat.getDrawable(getContext(), active);
        return this;
    }

    public SenseTabView useSleepScoreIcon(@Nullable final Timeline timeline) {
        this.imageView.post(() -> useSleepScoreIcon(timeline,
                                                    SenseTabView.this.imageView.getMeasuredWidth(),
                                                    SenseTabView.this.imageView.getMeasuredHeight(),
                                                    true));
        return this;
    }

    public void updateSleepScoreIcon(@Nullable final Timeline timeline,
                                    final boolean active) {
        useSleepScoreIcon(timeline,
                          this.imageView.getMeasuredWidth(),
                          this.imageView.getMeasuredHeight(),
                          active);
    }

    private void useSleepScoreIcon(@Nullable final Timeline timeline,
                                   final int width,
                                   final int height,
                                   final boolean active) {
        final SleepScoreIconDrawable.Builder builder = new SleepScoreIconDrawable.Builder(getContext());
        if (timeline != null
                && timeline.getScore() != null
                && TimelineInteractor.hasValidCondition(timeline)) {
            builder.withText(timeline.getScore());
        }
        builder.withSize(width,
                         height);
        this.normalDrawable = builder.build();
        this.activeDrawable = builder.withSelected(true).build();
        setActive(active);
    }


}
