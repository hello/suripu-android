package is.hello.sense.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
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
import is.hello.sense.ui.common.SenseAnimatedFragment;
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;

public class TimelineBreakdownFragment extends SenseAnimatedFragment {
    public static final String TAG = TimelineBreakdownFragment.class.getSimpleName();

    private static final String ARG_SCORE = TimelineBreakdownFragment.class.getName() + ".ARG_SCORE";
    private static final String ARG_ITEMS = TimelineBreakdownFragment.class.getName() + ".ARG_ITEMS";

    private int scoreColor;
    private ArrayList<Item> items;

    private RelativeLayout rootView;
    private View header;
    private RecyclerView recycler;

    private int overlayColor;
    private int headerOverlap;

    private @Nullable Window window;
    private int oldStatusBarColor;

    //region Lifecycle

    public static TimelineBreakdownFragment newInstance(int score, @NonNull ArrayList<Item> items) {
        TimelineBreakdownFragment fragment = new TimelineBreakdownFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(ARG_SCORE, score);
        arguments.putParcelableArrayList(ARG_ITEMS, items);
        fragment.setArguments(arguments);

        return fragment;
    }

    public static TimelineBreakdownFragment newInstance(@NonNull Timeline timeline) {
        Timeline.Statistics statistics = timeline.getStatistics();

        ArrayList<Item> items = new ArrayList<>();
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

        return newInstance(timeline.getScore(), items);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int score = getArguments().getInt(ARG_SCORE);
        this.scoreColor = Styles.getSleepScoreColor(getActivity(), score);
        this.items = getArguments().getParcelableArrayList(ARG_ITEMS);

        this.overlayColor = getResources().getColor(R.color.background_dark_overlay);
        this.headerOverlap = getResources().getDimensionPixelSize(R.dimen.gap_medium);

        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.window = activity.getWindow();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_timeline_breakdown, container, false);
        rootView.setOnClickListener(ignored -> getFragmentManager().popBackStack());

        this.header = rootView.findViewById(R.id.fragment_timeline_breakdown_header);
        header.setBackgroundColor(scoreColor);

        rootView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int headerBottom = header.getBottom();
            headerBottom += headerOverlap;
            header.setBottom(headerBottom);
        });

        this.recycler = (RecyclerView) rootView.findViewById(R.id.fragment_timeline_breakdown_recycler);
        recycler.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        recycler.setVisibility(View.INVISIBLE);

        Adapter adapter = new Adapter();
        recycler.setAdapter(adapter);

        return rootView;
    }

    @Override
    protected Animator onProvideEnterAnimator() {
        AnimatorSet compound = new AnimatorSet();

        compound.play(createFadeIn())
                .with(createHeaderReveal())
                .with(createRecyclerReveal());

        compound.setInterpolator(new AccelerateDecelerateInterpolator());
        compound.setDuration(Animation.DURATION_NORMAL);

        return compound;
    }

    @Override
    protected void onSkipEnterAnimator() {
        rootView.setBackgroundColor(overlayColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            this.oldStatusBarColor = window.getStatusBarColor();
            int newStatusBarColor = Drawing.darkenColorBy(scoreColor, 0.2f);
            window.setStatusBarColor(newStatusBarColor);
        }

        header.setVisibility(View.VISIBLE);
        header.setAlpha(1f);

        recycler.setTranslationY(0f);
        recycler.setVisibility(View.VISIBLE);
    }

    @Override
    protected Animator onProvideExitAnimator() {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Don't clear any of the view fields, we need
        // them around for the dismissal animator.
    }

    @Override
    public void onDetach() {
        super.onDetach();

        this.window = null;
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


    //region Showing

    private Animator createFadeIn() {
        ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            this.oldStatusBarColor = window.getStatusBarColor();
            int newStatusBarColor = Drawing.darkenColorBy(scoreColor, 0.2f);
            fadeIn.addUpdateListener(animator -> {
                float fraction = animator.getAnimatedFraction();

                int background = Drawing.interpolateColors(fraction, Color.TRANSPARENT, overlayColor);
                rootView.setBackgroundColor(background);

                int statusBar = Drawing.interpolateColors(fraction, oldStatusBarColor, newStatusBarColor);
                window.setStatusBarColor(statusBar);
            });
        } else {
            fadeIn.addUpdateListener(animator -> {
                float fraction = animator.getAnimatedFraction();

                int background = Drawing.interpolateColors(fraction, Color.TRANSPARENT, overlayColor);
                rootView.setBackgroundColor(background);
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
                header.setVisibility(View.VISIBLE);
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
                recycler.setVisibility(View.VISIBLE);
            }
        });
        return slideUp;
    }

    //endregion


    //region Hiding

    private Animator createRecyclerDismissal() {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) recycler.getLayoutParams();
        ObjectAnimator slideDown = ObjectAnimator.ofFloat(recycler, "translationY", 0f, recycler.getMeasuredHeight() + layoutParams.topMargin);
        slideDown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                recycler.setVisibility(View.INVISIBLE);
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
                header.setVisibility(View.INVISIBLE);
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
                rootView.setBackgroundColor(background);

                int statusBar = Drawing.interpolateColors(fraction, oldStatusBarColor, newStatusBarColor);
                window.setStatusBarColor(statusBar);
            });
        } else {
            fadeIn.addUpdateListener(animator -> {
                float fraction = animator.getAnimatedFraction();

                int background = Drawing.interpolateColors(fraction, overlayColor, Color.TRANSPARENT);
                rootView.setBackgroundColor(background);
            });
        }
        return fadeIn;
    }

    //endregion


    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private final LayoutInflater inflater = LayoutInflater.from(getActivity());

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
