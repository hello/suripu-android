package is.hello.sense.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.RectEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.SenseAnimatedFragment;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public class TimelineInfoFragment extends SenseAnimatedFragment {
    public static final String TAG = TimelineInfoFragment.class.getSimpleName();

    private static final String ARG_SCORE = TimelineInfoFragment.class.getName() + ".ARG_SCORE";
    private static final String ARG_ITEMS = TimelineInfoFragment.class.getName() + ".ARG_ITEMS";
    private static final String ARG_FROM_RECT = TimelineInfoFragment.class.getName() + ".ARG_FROM_RECT";

    private int scoreColor;
    private ArrayList<Item> items;
    private Rect fromRect;

    private RelativeLayout rootView;
    private View header;
    private RecyclerView recycler;

    private int overlayColor;

    private @Nullable Window window;
    private int oldStatusBarColor;

    //region Lifecycle

    public static TimelineInfoFragment newInstance(int score, @NonNull ArrayList<Item> items, @Nullable Rect fromRect) {
        TimelineInfoFragment fragment = new TimelineInfoFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(ARG_SCORE, score);
        arguments.putParcelableArrayList(ARG_ITEMS, items);
        arguments.putParcelable(ARG_FROM_RECT, fromRect);
        fragment.setArguments(arguments);

        return fragment;
    }

    public static TimelineInfoFragment newInstance(@NonNull Timeline timeline, @Nullable Rect fromRect) {
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

        return newInstance(timeline.getScore(), items, fromRect);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int score = getArguments().getInt(ARG_SCORE);
        this.scoreColor = Styles.getSleepScoreColor(getActivity(), score);
        this.items = getArguments().getParcelableArrayList(ARG_ITEMS);
        this.fromRect = getArguments().getParcelable(ARG_FROM_RECT);

        this.overlayColor = getResources().getColor(R.color.background_dark_overlay);

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
        this.rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_timeline_info, container, false);
        rootView.setOnClickListener(ignored -> getFragmentManager().popBackStack());

        this.header = rootView.findViewById(R.id.fragment_timeline_info_header);
        header.setBackgroundColor(scoreColor);

        this.recycler = (RecyclerView) rootView.findViewById(R.id.fragment_timeline_info_recycler);
        recycler.setVisibility(View.INVISIBLE);
        recycler.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        recycler.addItemDecoration(new ItemDecoration(getResources()));

        return rootView;
    }

    @Override
    protected Animator onProvideEnterAnimator() {
        AnimatorSet compound = new AnimatorSet();

        compound.play(createFadeIn())
                .with(createHeaderReveal())
                .with(createRecyclerReveal());

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
        ValueAnimator animator = ValueAnimator.ofObject(new RectEvaluator(), fromRect, Views.copyFrame(recycler));
        animator.addUpdateListener(a -> {
            Rect current = (Rect) a.getAnimatedValue();
            Views.setFrame(recycler, current);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                Views.setFrame(recycler, fromRect);
                recycler.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setUpRecycler();
            }
        });
        return animator;
    }

    //endregion


    //region Hiding

    private Animator createRecyclerDismissal() {
        ValueAnimator animator = ValueAnimator.ofObject(new RectEvaluator(), Views.copyFrame(recycler), fromRect);
        tearDownRecycler();
        animator.addUpdateListener(a -> {
            Rect current = (Rect) a.getAnimatedValue();
            Views.setFrame(recycler, current);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                recycler.setVisibility(View.INVISIBLE);
            }
        });
        return animator;
    }

    private Animator createHeaderDismissal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int centerX = header.getMeasuredWidth() / 2,
                centerY = 0;
            float startRadius = Math.max(header.getMeasuredWidth(), header.getMeasuredHeight()),
                    endRadius = 0f;
            Animator revealAnimator = ViewAnimationUtils.createCircularReveal(header, centerX, centerY, startRadius, endRadius);
            revealAnimator.setStartDelay(Animation.DURATION_NORMAL / 2);
            revealAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    header.setVisibility(View.INVISIBLE);
                    header.setAlpha(1f);
                }
            });
            return revealAnimator;
        } else {
            // We fade the whole view out on KitKat and below.
            return Animation.createEmptyAnimator();
        }
    }

    private Animator createFadeOut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            ValueAnimator fadeOut = ValueAnimator.ofFloat(0f, 1f);

            int oldStatusBarColor = window.getStatusBarColor();
            int newStatusBarColor = this.oldStatusBarColor;
            fadeOut.addUpdateListener(animator -> {
                float fraction = animator.getAnimatedFraction();

                int background = Drawing.interpolateColors(fraction, overlayColor, Color.TRANSPARENT);
                rootView.setBackgroundColor(background);

                int statusBar = Drawing.interpolateColors(fraction, oldStatusBarColor, newStatusBarColor);
                window.setStatusBarColor(statusBar);
            });

            return fadeOut;
        } else {
            return ObjectAnimator.ofFloat(rootView, "alpha", 1f, 0f);
        }
    }

    //endregion


    //region Recycler

    private void setUpRecycler() {
        recycler.setAdapter(new Adapter());
    }

    private void tearDownRecycler() {
        recycler.setAdapter(null);
    }

    //endregion


    public static class ItemDecoration extends RecyclerView.ItemDecoration {
        private final Rect lineRect = new Rect();
        private final Paint linePaint = new Paint();
        private final int dividerHeight;
        private final int verticalDividerInset;

        public ItemDecoration(@NonNull Resources resources) {
            this.dividerHeight = resources.getDimensionPixelSize(R.dimen.divider_size);
            this.verticalDividerInset = resources.getDimensionPixelSize(R.dimen.gap_medium);

            int lineColor = resources.getColor(R.color.border);
            linePaint.setColor(lineColor);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.bottom += dividerHeight;
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            for (int i = 0, size = parent.getChildCount(); i < size; i++) {
                View child = parent.getChildAt(i);
                if ((i % 2) == 0) {
                    lineRect.set(child.getLeft() - dividerHeight, child.getTop() + verticalDividerInset,
                            child.getLeft(), child.getBottom() - verticalDividerInset);
                    c.drawRect(lineRect, linePaint);
                }

                lineRect.set(child.getLeft(), child.getBottom() - dividerHeight,
                        child.getRight(), child.getBottom());
                c.drawRect(lineRect, linePaint);
            }
        }
    }

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
            holder.readingText.setText(item.getFormattedValue());
            holder.readingText.setTextColor(scoreColor);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView titleText;
            final TextView readingText;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                this.titleText = (TextView) itemView.findViewById(R.id.item_timeline_info_title);
                this.readingText = (TextView) itemView.findViewById(R.id.item_timeline_info_reading);
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
