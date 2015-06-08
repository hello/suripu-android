package is.hello.sense.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.PreSleepInsight;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.adapter.EmptyRecyclerAdapter;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.SenseAnimatedFragment;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public class TimelineInfoFragment extends SenseAnimatedFragment {
    public static final String TAG = TimelineInfoFragment.class.getSimpleName();

    private static final String ARG_SUMMARY = TimelineInfoFragment.class.getName() + ".ARG_SUMMARY";
    private static final String ARG_SCORE = TimelineInfoFragment.class.getName() + ".ARG_SCORE";
    private static final String ARG_ITEMS = TimelineInfoFragment.class.getName() + ".ARG_ITEMS";
    private static final String ARG_SOURCE_VIEW_ID = TimelineInfoFragment.class.getName() + ".ARG_SOURCE_VIEW_ID";

    private @Nullable CharSequence summary;
    private int scoreColor;
    private ArrayList<Item> items;
    private @IdRes int sourceViewId;

    private RelativeLayout rootView;
    private View header;
    private RecyclerView recycler;
    private @Nullable ViewGroup.LayoutParams finalRecyclerLayoutParams;

    private int overlayColor;

    private @Nullable Window window;
    private int oldStatusBarColor;

    //region Lifecycle

    public static TimelineInfoFragment newInstance(@Nullable CharSequence message,
                                                   int score,
                                                   @NonNull ArrayList<Item> items,
                                                   @IdRes int sourceViewId) {
        TimelineInfoFragment fragment = new TimelineInfoFragment();

        Bundle arguments = new Bundle();
        if (!TextUtils.isEmpty(message)) {
            Parcel messageParcel = Parcel.obtain();
            try {
                TextUtils.writeToParcel(message, messageParcel, 0);
                arguments.putByteArray(ARG_SUMMARY, messageParcel.marshall());
            } finally {
                messageParcel.recycle();
            }
        }
        arguments.putInt(ARG_SCORE, score);
        arguments.putParcelableArrayList(ARG_ITEMS, items);
        arguments.putInt(ARG_SOURCE_VIEW_ID, sourceViewId);
        fragment.setArguments(arguments);

        return fragment;
    }

    public static TimelineInfoFragment newInstance(@NonNull Timeline timeline,
                                                   @Nullable CharSequence message,
                                                   @IdRes int sourceViewId) {
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

        DateTime fellAsleepTime = null,
                 wakeUpTime = null;
        for (TimelineSegment segment : timeline.getSegments()) {
            TimelineSegment.EventType eventType = segment.getEventType();
            if (fellAsleepTime == null && eventType == TimelineSegment.EventType.SLEEP) {
                fellAsleepTime = segment.getShiftedTimestamp();
            }

            if (eventType == TimelineSegment.EventType.WAKE_UP) {
                wakeUpTime = segment.getShiftedTimestamp();
            }
        }

        if (fellAsleepTime != null) {
            items.add(new Item(R.string.timeline_breakdown_label_sleep_time, Item.Type.TIME, fellAsleepTime.getMillis()));
        }

        if (wakeUpTime != null) {
            items.add(new Item(R.string.timeline_breakdown_label_wake_up_time, Item.Type.TIME, wakeUpTime.getMillis()));
        }

        for (PreSleepInsight insight : timeline.getPreSleepInsights()) {
            items.add(new Item(insight.getSensor().titleRes, Item.Type.SENSOR, insight.getCondition().ordinal()));
        }

        return newInstance(message, timeline.getScore(), items, sourceViewId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();

        int score = arguments.getInt(ARG_SCORE);
        this.scoreColor = Styles.getSleepScoreColor(getActivity(), score);
        this.items = arguments.getParcelableArrayList(ARG_ITEMS);
        this.sourceViewId = arguments.getInt(ARG_SOURCE_VIEW_ID);

        byte[] summaryBytes = arguments.getByteArray(ARG_SUMMARY);
        if (summaryBytes != null) {
            Parcel messageParcel = Parcel.obtain();
            try {
                messageParcel.unmarshall(summaryBytes, 0, summaryBytes.length);
                messageParcel.setDataPosition(0);
                this.summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(messageParcel);
            } finally {
                messageParcel.recycle();
            }
        }

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

        TextView summaryText = (TextView) header.findViewById(R.id.fragment_timeline_info_summary);
        summaryText.setText(summary);

        this.recycler = (RecyclerView) rootView.findViewById(R.id.fragment_timeline_info_recycler);
        recycler.setVisibility(View.INVISIBLE);
        recycler.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        recycler.addItemDecoration(new ItemDecoration(getResources()));
        recycler.setAdapter(new EmptyRecyclerAdapter());

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
        ValueAnimator animator;

        View fromView = findSourceView();
        if (fromView != null) {
            Rect fromRect = new Rect();
            Views.getFrameInWindow(fromView, fromRect);

            animator = Animation.createViewFrameAnimator(recycler, fromRect, Views.copyFrame(recycler));

            this.finalRecyclerLayoutParams = recycler.getLayoutParams();

            RelativeLayout.LayoutParams initialLayoutParams = new RelativeLayout.LayoutParams(fromRect.width(), fromRect.height());
            initialLayoutParams.leftMargin = fromRect.left;
            initialLayoutParams.topMargin = fromRect.top;
            recycler.setLayoutParams(initialLayoutParams);
        } else {
            animator = ObjectAnimator.ofFloat(recycler, "alpha", 0f, 1f);
        }
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
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
        ValueAnimator animator;

        View fromView = findSourceView();
        if (fromView != null) {
            Rect fromRect = new Rect();
            Views.getFrameInWindow(fromView, fromRect);

            animator = Animation.createViewFrameAnimator(recycler, Views.copyFrame(recycler), fromRect);
        } else {
            animator = ObjectAnimator.ofFloat(recycler, "alpha", 1f, 0f);
        }

        tearDownRecycler();

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

    private @Nullable View findSourceView() {
        Activity activity = getActivity();
        if (activity != null) {
            return activity.findViewById(sourceViewId);
        } else {
            return null;
        }
    }

    private void setUpRecycler() {
        if (finalRecyclerLayoutParams != null) {
            recycler.setLayoutParams(finalRecyclerLayoutParams);
            this.finalRecyclerLayoutParams = null;
        }
        recycler.setAdapter(new ItemAdapter());
    }

    private void tearDownRecycler() {
        // Clearing the adapter through the notifyItem* APIs
        // causes a large number of frames to be dropped in
        // the out animation. Swapping the adapter fixes it.
        recycler.setAdapter(new EmptyRecyclerAdapter());
    }

    //endregion


    public static class ItemDecoration extends RecyclerView.ItemDecoration {
        private final Rect lineRect = new Rect();
        private final Paint linePaint = new Paint();
        private final int dividerSize;
        private final int verticalDividerInset;

        public ItemDecoration(@NonNull Resources resources) {
            this.dividerSize = resources.getDimensionPixelSize(R.dimen.divider_size);
            this.verticalDividerInset = resources.getDimensionPixelSize(R.dimen.gap_medium);

            int lineColor = resources.getColor(R.color.border);
            linePaint.setColor(lineColor);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if ((parent.getChildAdapterPosition(view) % 2) == 0) {
                outRect.right += dividerSize;
            }

            outRect.bottom += dividerSize;
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            for (int i = 0, size = parent.getChildCount(); i < size; i++) {
                View child = parent.getChildAt(i);
                if ((i % 2) == 0) {
                    lineRect.set(child.getRight() - dividerSize, child.getTop() + verticalDividerInset,
                            child.getRight(), child.getBottom() - verticalDividerInset);
                    c.drawRect(lineRect, linePaint);
                }

                lineRect.set(child.getLeft(), child.getBottom() - dividerSize,
                        child.getRight(), child.getBottom());
                c.drawRect(lineRect, linePaint);
            }
        }
    }

    public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
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
            holder.readingText.setText(item.getFormattedValue(getActivity()));

            int valueColorRes = item.getValueColorRes();
            if (valueColorRes == 0) {
                holder.readingText.setTextColor(scoreColor);
            } else {
                holder.readingText.setTextColor(getResources().getColor(valueColorRes));
            }
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
        public final long value;

        public Item(@StringRes int titleRes,
                    @NonNull Type type,
                    long value) {
            this.titleRes = titleRes;
            this.type = type;
            this.value = value;
        }

        public String getFormattedValue(@NonNull Context context) {
            return type.format(context, value);
        }

        public @ColorRes int getValueColorRes() {
            return type.getValueColor(value);
        }

        //region Serialization

        public Item(@NonNull Parcel in) {
            this(in.readInt(), Type.values()[in.readInt()], in.readLong());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(titleRes);
            out.writeInt(type.ordinal());
            out.writeLong(value);
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
                public String format(@NonNull Context context, long value) {
                    if (value < 60) {
                        return value + "m";
                    } else {
                        long hours = value / 60;
                        long minutes = value % 60;

                        String reading = Long.toString(hours);
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
                public String format(@NonNull Context context, long value) {
                    return Long.toString(value);
                }
            },
            TIME {
                @Override
                public String format(@NonNull Context context, long value) {
                    return new DateTime(value).toString(DateTimeFormat.shortTime());
                }
            },
            SENSOR {
                @Override
                public String format(@NonNull Context context, long value) {
                    Condition condition = Condition.values()[(int) value];
                    switch (condition) {
                        case UNKNOWN:
                            return context.getString(R.string.missing_data_placeholder);

                        case ALERT:
                            return "was bad";

                        case WARNING:
                            return "a bit too high";

                        case IDEAL:
                            return "was ideal";

                        default:
                            throw new IllegalStateException("Unknown condition '" + condition + "'");
                    }
                }

                @Override
                public int getValueColor(long value) {
                    Condition condition = Condition.values()[(int) value];
                    switch (condition) {
                        case UNKNOWN:
                            return R.color.sensor_unknown;

                        case ALERT:
                            return R.color.sensor_alert;

                        case WARNING:
                            return R.color.sensor_warning;

                        case IDEAL:
                            return R.color.sensor_ideal;

                        default:
                            throw new IllegalStateException("Unknown condition '" + condition + "'");
                    }
                }
            };

            public abstract String format(@NonNull Context context, long value);

            public @ColorRes int getValueColor(long value) {
                return 0;
            }
        }
    }
}
