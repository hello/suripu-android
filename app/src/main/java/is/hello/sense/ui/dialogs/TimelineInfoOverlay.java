package is.hello.sense.ui.dialogs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;

public class TimelineInfoOverlay extends RelativeLayout {
    private int score;
    private int scoreColor;
    private final ArrayList<Item> items = new ArrayList<>();

    private final View header;
    private final RecyclerView recycler;
    private final Adapter adapter;
    private final LayoutInflater inflater;

    private final int overlayColor;
    private final int headerOverlap;

    private @Nullable Window window;
    private int oldStatusBarColor;

    //region Lifecycle

    public TimelineInfoOverlay(@NonNull Context context) {
        super(context);

        this.inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.view_timeline_info_overlay, this, true);

        setClickable(true);

        this.overlayColor = getResources().getColor(R.color.background_dark_overlay);
        this.headerOverlap = getResources().getDimensionPixelSize(R.dimen.gap_medium);

        this.header = findViewById(R.id.view_timeline_info_overlay_header);

        this.recycler = (RecyclerView) findViewById(R.id.view_timeline_info_overlay_recycler);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(context));
        recycler.setVisibility(View.INVISIBLE);

        this.adapter = new Adapter();
        recycler.setAdapter(adapter);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        this.window = ((Activity) getContext()).getWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            window.setStatusBarColor(oldStatusBarColor);
        }

        this.window = null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        int headerBottom = header.getBottom();
        headerBottom += headerOverlap;
        header.setBottom(headerBottom);
    }

    //endregion


    private static void setProgressTint(@NonNull ProgressBar tint, int tintColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tint.setProgressTintList(ColorStateList.valueOf(tintColor));
        } else {
            Drawable drawable = tint.getProgressDrawable();
            if (drawable instanceof LayerDrawable) {
                Drawable fillDrawable = ((LayerDrawable) drawable).findDrawableByLayerId(android.R.id.progress);
                if (fillDrawable != null) {
                    Drawables.setTintColor(fillDrawable, tintColor);
                }
            }
        }
    }


    //region Attributes

    public void setScore(int score) {
        this.score = score;
        setScoreColor(Styles.getSleepScoreColor(getContext(), score));
    }

    public void setScoreColor(int scoreColor) {
        this.scoreColor = scoreColor;

        header.setBackgroundColor(scoreColor);
    }

    public void setTimeline(@NonNull Timeline timeline) {
        Timeline.Statistics statistics = timeline.getStatistics();

        items.clear();

        if (statistics.getTotalSleep() != null) {
            int totalSleep = statistics.getTotalSleep();
            items.add(new Item(R.string.timeline_breakdown_label_total_sleep, Item.Type.DURATION, totalSleep));
        }

        if (statistics.getSoundSleep() != null) {
            int soundSleep = statistics.getSoundSleep();
            items.add(new Item(R.string.timeline_breakdown_label_sound_sleep, Item.Type.DURATION, soundSleep));
        }

        if (statistics.getTimeToSleep() != null) {
            int timeToSleep = statistics.getTimeToSleep();
            items.add(new Item(R.string.timeline_breakdown_label_total_sleep, Item.Type.DURATION, timeToSleep));
        }

        if (statistics.getTimesAwake() != null) {
            int timesAwake = statistics.getTimesAwake();
            items.add(new Item(R.string.timeline_breakdown_label_times_awake, Item.Type.COUNT, timesAwake));
        }

        setScore(timeline.getScore());
    }

    //endregion


    //region Showing

    private void prepareForShowing() {
        setBackground(null);
        header.setVisibility(INVISIBLE);
        recycler.setVisibility(INVISIBLE);
    }

    private Animator createFadeIn() {
        ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            this.oldStatusBarColor = window.getStatusBarColor();
            int newStatusBarColor = Drawing.darkenColorBy(scoreColor, 0.2f);
            fadeIn.addUpdateListener(animator -> {
                float fraction = animator.getAnimatedFraction();

                int background = Drawing.interpolateColors(fraction, Color.TRANSPARENT, overlayColor);
                setBackgroundColor(background);

                int statusBar = Drawing.interpolateColors(fraction, oldStatusBarColor, newStatusBarColor);
                window.setStatusBarColor(statusBar);
            });
        } else {
            fadeIn.addUpdateListener(animator -> {
                float fraction = animator.getAnimatedFraction();

                int background = Drawing.interpolateColors(fraction, Color.TRANSPARENT, Color.BLACK);
                setBackgroundColor(background);
            });
        }
        return fadeIn;
    }

    private Animator createHeaderReveal() {
        Animator animator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int centerX = header.getMeasuredWidth() / 2,
                centerY = header.getMeasuredHeight();
            float startRadius = 0f,
                    endRadius = Math.max(header.getMeasuredWidth(), header.getMeasuredHeight());
            animator = ViewAnimationUtils.createCircularReveal(header, centerX, centerY, startRadius, endRadius);
        } else {
            animator = ObjectAnimator.ofFloat(header, "alpha", 0f, 1f);
        }
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                header.setVisibility(VISIBLE);
            }
        });
        return animator;
    }

    private Animator createRecyclerReveal() {
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(recycler, "translationY", recycler.getMeasuredHeight(), 0f);
        slideUp.setStartDelay(Animation.DURATION_NORMAL / 2);
        slideUp.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                recycler.setVisibility(VISIBLE);
            }
        });
        return slideUp;
    }

    private Animator createRevealAnimator() {
        AnimatorSet compound = new AnimatorSet();

        compound.play(createFadeIn())
                .with(createHeaderReveal())
                .with(createRecyclerReveal());

        compound.setInterpolator(new AccelerateDecelerateInterpolator());
        compound.setDuration(Animation.DURATION_NORMAL);

        return compound;
    }

    public void showIn(@NonNull ViewGroup parent) {
        prepareForShowing();

        parent.addView(this, new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        post(() -> {
            Animator reveal = createRevealAnimator();
            reveal.start();
        });
    }

    //endregion


    //region Hiding

    private Animator createRecyclerDismissal() {
        MarginLayoutParams layoutParams = (MarginLayoutParams) recycler.getLayoutParams();
        Animator slideDown = ObjectAnimator.ofFloat(recycler, "translationY", 0f, recycler.getMeasuredHeight() + layoutParams.topMargin);
        slideDown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                recycler.setVisibility(INVISIBLE);
                recycler.setTranslationY(0f);
            }
        });
        return slideDown;
    }

    private Animator createHeaderDismissal() {
        Animator animator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int centerX = header.getMeasuredWidth() / 2,
                centerY = 0;
            float startRadius = Math.max(header.getMeasuredWidth(), header.getMeasuredHeight()),
                    endRadius = 0f;
            animator = ViewAnimationUtils.createCircularReveal(header, centerX, centerY, startRadius, endRadius);
        } else {
            animator = ObjectAnimator.ofFloat(header, "alpha", 1f, 0f);
        }
        animator.setStartDelay(Animation.DURATION_NORMAL / 2);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                header.setVisibility(INVISIBLE);
                header.setAlpha(1f);
            }
        });
        return animator;
    }

    private Animator createFadeOut() {
        ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            int oldStatusBarColor = window.getStatusBarColor();
            int newStatusBarColor = this.oldStatusBarColor;
            fadeIn.addUpdateListener(animator -> {
                float fraction = animator.getAnimatedFraction();

                int background = Drawing.interpolateColors(fraction, overlayColor, Color.TRANSPARENT);
                setBackgroundColor(background);

                int statusBar = Drawing.interpolateColors(fraction, oldStatusBarColor, newStatusBarColor);
                window.setStatusBarColor(statusBar);
            });
        } else {
            fadeIn.addUpdateListener(animator -> {
                float fraction = animator.getAnimatedFraction();

                int background = Drawing.interpolateColors(fraction, overlayColor, Color.TRANSPARENT);
                setBackgroundColor(background);
            });
        }
        return fadeIn;
    }

    private Animator createDismissalAnimator() {
        AnimatorSet compound = new AnimatorSet();

        Animator recyclerDismissal = createRecyclerDismissal();
        compound.play(recyclerDismissal)
                .with(createHeaderDismissal());
        compound.play(createFadeOut())
                .after(recyclerDismissal);

        compound.setInterpolator(new AccelerateDecelerateInterpolator());
        compound.setDuration(Animation.DURATION_NORMAL);

        return compound;
    }

    public void removeFromParent() {
        ViewGroup parent = (ViewGroup) getParent();
        parent.removeView(this);
    }

    public void dismiss() {
        Animator dismissal = createDismissalAnimator();
        dismissal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                removeFromParent();
            }
        });
        dismissal.start();
    }

    //endregion


    //region Events

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP: {
                dismiss();

                break;
            }
        }

        return true;
    }

    //endregion


    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_timeline_info, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Item item = items.get(position);

            holder.titleText.setText(item.titleRes);
            holder.valueText.setText(item.getFormattedValue());
            holder.valueText.setTextColor(scoreColor);

            setProgressTint(holder.reading, scoreColor);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView valueText;
            final TextView averageText;
            final TextView titleText;
            final ProgressBar reading;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                this.valueText = (TextView) itemView.findViewById(R.id.item_timeline_info_value);
                this.averageText = (TextView) itemView.findViewById(R.id.item_timeline_info_average);
                this.titleText = (TextView) itemView.findViewById(R.id.item_timeline_info_title);
                this.reading = (ProgressBar) itemView.findViewById(R.id.item_timeline_info_reading);
            }
        }
    }

    public static class Item implements Parcelable {
        public final @StringRes int titleRes;
        public final Type type;
        public final int value;

        public Item(@StringRes int titleRes,
                    @NonNull Type type,
                    int value) {
            this.titleRes = titleRes;
            this.type = type;
            this.value = value;
        }

        public String getFormattedValue() {
            return type.format(value);
        }

        //region Serialization

        public Item(@NonNull Parcel in) {
            this(in.readInt(), Type.values()[in.readInt()], in.readInt());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(titleRes);
            out.writeInt(type.ordinal());
            out.writeInt(value);
        }

        public static final Parcelable.Creator<Item> CREATOR = new Creator<Item>() {
            @Override
            public Item createFromParcel(Parcel source) {
                return new Item(source);
            }

            @Override
            public Item[] newArray(int size) {
                return new Item[size];
            }
        };

        //endregion

        public enum Type {
            DURATION {
                @Override
                public String format(int value) {
                    if (value < 60) {
                        return value + "m";
                    } else {
                        int hours = value / 60;
                        int minutes = value % 60;

                        String reading = Integer.toString(hours);
                        if (minutes >= 30) {
                            reading += "." + minutes;
                        }

                        reading += "h";

                        return reading;
                    }
                }
            },
            COUNT {
                @Override
                public String format(int value) {
                    return Integer.toString(value);
                }
            };

            public abstract String format(int value);
        }
    }
}
