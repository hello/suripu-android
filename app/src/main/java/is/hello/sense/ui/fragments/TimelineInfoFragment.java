package is.hello.sense.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.graphics.Canvas;
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
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.PreSleepInsight;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.adapter.EmptyRecyclerAdapter;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.AnimatedInjectionFragment;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.DateFormatter;

public class TimelineInfoFragment extends AnimatedInjectionFragment {
    public static final String TAG = TimelineInfoFragment.class.getSimpleName();

    private static final String ARG_SUMMARY = TimelineInfoFragment.class.getName() + ".ARG_SUMMARY";
    private static final String ARG_SCORE = TimelineInfoFragment.class.getName() + ".ARG_SCORE";
    private static final String ARG_ITEMS = TimelineInfoFragment.class.getName() + ".ARG_ITEMS";
    private static final String ARG_SOURCE_VIEW_ID = TimelineInfoFragment.class.getName() + ".ARG_SOURCE_VIEW_ID";

    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;

    private @Nullable CharSequence summary;
    private int scoreColor;
    private ArrayList<Item> items;
    private @IdRes int sourceViewId;

    private RelativeLayout rootView;
    private View header;
    private RecyclerView recycler;
    private @Nullable ViewGroup.LayoutParams finalRecyclerLayoutParams;

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
            items.add(new DurationItem(R.string.timeline_breakdown_label_total_sleep, totalSleep));
        }

        if (statistics.getSoundSleep() != null) {
            int soundSleep = statistics.getSoundSleep();
            items.add(new DurationItem(R.string.timeline_breakdown_label_sound_sleep, soundSleep));
        }

        if (statistics.getTimeToSleep() != null) {
            int timeToSleep = statistics.getTimeToSleep();
            items.add(new DurationItem(R.string.timeline_breakdown_label_total_sleep, timeToSleep));
        }

        if (statistics.getTimesAwake() != null) {
            int timesAwake = statistics.getTimesAwake();
            items.add(new CountItem(R.string.timeline_breakdown_label_times_awake, timesAwake));
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
            items.add(new TimeItem(R.string.timeline_breakdown_label_sleep_time, fellAsleepTime));
        }

        if (wakeUpTime != null) {
            items.add(new TimeItem(R.string.timeline_breakdown_label_wake_up_time, wakeUpTime));
        }

        for (PreSleepInsight insight : timeline.getPreSleepInsights()) {
            items.add(new SensorItem(insight.getSensor(), insight.getCondition()));
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
        Views.setSafeOnClickListener(rootView, ignored -> {
            getFragmentManager().popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        });

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

        compound.setInterpolator(new FastOutSlowInInterpolator());
        compound.setDuration(Animation.DURATION_NORMAL);

        return compound;
    }

    @Override
    protected void onSkipEnterAnimator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            this.oldStatusBarColor = window.getStatusBarColor();
            int newStatusBarColor = Drawing.darkenColorBy(scoreColor, 0.2f);
            window.setStatusBarColor(newStatusBarColor);
        }

        header.setVisibility(View.VISIBLE);
        recycler.setVisibility(View.VISIBLE);
        rootView.setAlpha(1f);

        setUpRecycler();
    }

    @Override
    protected Animator onProvideExitAnimator() {
        AnimatorSet compound = new AnimatorSet();

        compound.play(createFadeOut())
                .with(createHeaderDismissal())
                .with(createRecyclerDismissal());

        compound.setInterpolator(new FastOutLinearInInterpolator());
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
                rootView.setAlpha(fraction);

                int statusBar = Drawing.interpolateColors(fraction, oldStatusBarColor, newStatusBarColor);
                window.setStatusBarColor(statusBar);
            });
        } else {
            fadeIn.addUpdateListener(animator -> {
                float fraction = animator.getAnimatedFraction();
                rootView.setAlpha(fraction);
            });
        }
        return fadeIn;
    }

    private Animator createHeaderReveal() {
        Animator animator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int centerX = header.getMeasuredWidth() / 2,
                centerY = 0;
            float startRadius = 0f,
                    endRadius = Math.max(header.getMeasuredWidth(), header.getMeasuredHeight());
            animator = ViewAnimationUtils.createCircularReveal(header, centerX, centerY, startRadius, endRadius);
        } else {
            animator = ObjectAnimator.ofFloat(header, "translationY", -header.getMeasuredHeight(), 0f);
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
        Animator dismissAnimator;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int centerX = header.getMeasuredWidth() / 2,
                centerY = 0;
            float startRadius = Math.max(header.getMeasuredWidth(), header.getMeasuredHeight()),
                    endRadius = 0f;
            dismissAnimator = ViewAnimationUtils.createCircularReveal(header, centerX, centerY, startRadius, endRadius);
            dismissAnimator.setStartDelay(Animation.DURATION_NORMAL / 2);
        } else {
            dismissAnimator = ObjectAnimator.ofFloat(header, "translationY", 0f, -header.getMeasuredHeight());
        }

        dismissAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                header.setVisibility(View.INVISIBLE);
            }
        });
        return dismissAnimator;
    }

    private Animator createFadeOut() {
        ValueAnimator fadeOut = ValueAnimator.ofFloat(0f, 1f);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            int oldStatusBarColor = window.getStatusBarColor();
            int newStatusBarColor = this.oldStatusBarColor;
            fadeOut.addUpdateListener(animator -> {
                float fraction = animator.getAnimatedFraction();
                rootView.setAlpha(1f - fraction);

                int statusBar = Drawing.interpolateColors(fraction, oldStatusBarColor, newStatusBarColor);
                window.setStatusBarColor(statusBar);
            });
        } else {
            fadeOut.addUpdateListener(animator -> {
                float fraction = animator.getAnimatedFraction();
                rootView.setAlpha(1f - fraction);
            });
        }

        return fadeOut;
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

            holder.titleText.setText(item.getTitleRes());
            holder.readingText.setText(item.getDisplayString(TimelineInfoFragment.this));

            int valueColorRes = item.getTintColorRes();
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

    public static abstract class Item implements Parcelable {
        private final int titleRes;

        protected Item(@StringRes int titleRes) {
            this.titleRes = titleRes;
        }

        public @StringRes int getTitleRes() {
            return titleRes;
        }

        public abstract CharSequence getDisplayString(@NonNull TimelineInfoFragment parent);

        public @ColorRes int getTintColorRes() {
            return 0;
        }


        @Override
        public int describeContents() {
            return 0;
        }
    }

    public static class DurationItem extends Item {
        private final int minutes;

        public DurationItem(@StringRes int titleRes, int minutes) {
            super(titleRes);
            this.minutes = minutes;
        }

        public DurationItem(@NonNull Parcel in) {
            this(in.readInt(), in.readInt());
        }


        @Override
        public CharSequence getDisplayString(@NonNull TimelineInfoFragment parent) {
            return parent.dateFormatter.formatDuration(parent.getActivity(), minutes, TimeUnit.MINUTES);
        }


        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(getTitleRes());
            out.writeInt(minutes);
        }

        public static final Creator<DurationItem> CREATOR = new Creator<DurationItem>() {
            @Override
            public DurationItem createFromParcel(Parcel source) {
                return new DurationItem(source);
            }

            @Override
            public DurationItem[] newArray(int size) {
                return new DurationItem[size];
            }
        };
    }

    public static class CountItem extends Item {
        private final int count;

        public CountItem(@StringRes int titleRes, int count) {
            super(titleRes);
            this.count = count;
        }

        public CountItem(@NonNull Parcel in) {
            this(in.readInt(), in.readInt());
        }


        @Override
        public CharSequence getDisplayString(@NonNull TimelineInfoFragment parent) {
            return Integer.toString(count);
        }


        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(getTitleRes());
            out.writeInt(count);
        }


        public static final Creator<CountItem> CREATOR = new Creator<CountItem>() {
            @Override
            public CountItem createFromParcel(Parcel source) {
                return new CountItem(source);
            }

            @Override
            public CountItem[] newArray(int size) {
                return new CountItem[size];
            }
        };
    }

    public static class TimeItem extends Item {
        private final DateTime dateTime;

        public TimeItem(@StringRes int titleRes, @NonNull DateTime dateTime) {
            super(titleRes);
            this.dateTime = dateTime;
        }

        public TimeItem(@NonNull Parcel in) {
            this(in.readInt(), new DateTime(in.readLong()));
        }


        @Override
        public CharSequence getDisplayString(@NonNull TimelineInfoFragment parent) {
            boolean use24Time = parent.preferences.getUse24Time();
            return parent.dateFormatter.formatForTimelineInfo(dateTime, use24Time);
        }


        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(getTitleRes());
            out.writeLong(dateTime.getMillis());
        }


        public static final Creator<TimeItem> CREATOR = new Creator<TimeItem>() {
            @Override
            public TimeItem createFromParcel(Parcel source) {
                return new TimeItem(source);
            }

            @Override
            public TimeItem[] newArray(int size) {
                return new TimeItem[size];
            }
        };
    }

    public static class SensorItem extends Item {
        private final PreSleepInsight.Sensor sensor;
        private final Condition condition;

        public SensorItem(@NonNull PreSleepInsight.Sensor sensor,
                          @NonNull Condition condition) {
            super(sensor.titleRes);
            this.sensor = sensor;
            this.condition = condition;
        }

        public SensorItem(@NonNull Parcel in) {
            this(PreSleepInsight.Sensor.values()[in.readInt()], Condition.values()[in.readInt()]);
        }


        @Override
        public CharSequence getDisplayString(@NonNull TimelineInfoFragment parent) {
            switch (condition) {
                case UNKNOWN: {
                    return parent.getString(R.string.missing_data_placeholder);
                }
                case ALERT: {
                    switch (sensor) {
                        case TEMPERATURE:
                            return parent.getString(R.string.condition_short_hand_alert_temperature);

                        case HUMIDITY:
                            return parent.getString(R.string.condition_short_hand_alert_humidity);

                        case PARTICULATES:
                            return parent.getString(R.string.condition_short_hand_alert_particulates);

                        case SOUND:
                            return parent.getString(R.string.condition_short_hand_alert_sound);

                        case LIGHT:
                            return parent.getString(R.string.condition_short_hand_alert_light);

                        case UNKNOWN:
                            return parent.getString(R.string.condition_short_hand_alert_generic);
                    }
                }
                case WARNING: {
                    return parent.getString(R.string.condition_short_hand_warning);
                }
                case IDEAL: {
                    return parent.getString(R.string.condition_short_hand_ideal);
                }
                default: {
                    throw new IllegalStateException("Unknown condition '" + condition + "'");
                }
            }
        }

        @Override
        public @ColorRes int getTintColorRes() {
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


        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(sensor.ordinal());
            out.writeInt(condition.ordinal());
        }


        public static final Creator<SensorItem> CREATOR = new Creator<SensorItem>() {
            @Override
            public SensorItem createFromParcel(Parcel source) {
                return new SensorItem(source);
            }

            @Override
            public SensorItem[] newArray(int size) {
                return new SensorItem[size];
            }
        };
    }
}
