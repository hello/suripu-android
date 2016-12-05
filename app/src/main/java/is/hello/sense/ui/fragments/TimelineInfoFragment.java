package is.hello.sense.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineMetric;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.ui.adapter.EmptyRecyclerAdapter;
import is.hello.sense.ui.common.AnimatedInjectionFragment;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.ui.widget.util.Windows;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.markup.text.MarkupString;

public class TimelineInfoFragment extends AnimatedInjectionFragment {
    public static final String TAG = TimelineInfoFragment.class.getSimpleName();

    private static final String ARG_SUMMARY = TimelineInfoFragment.class.getName() + ".ARG_SUMMARY";
    private static final String ARG_SCORE_CONDITION = TimelineInfoFragment.class.getName() + ".ARG_SCORE_CONDITION";
    private static final String ARG_METRICS = TimelineInfoFragment.class.getName() + ".ARG_METRICS";
    private static final String ARG_SOURCE_VIEW_ID = TimelineInfoFragment.class.getName() + ".ARG_SOURCE_VIEW_ID";

    private static final int GRID_COLUMNS_PER_ROW = 2;

    @Inject DateFormatter dateFormatter;
    @Inject
    PreferencesInteractor preferences;

    private @Nullable MarkupString summary;
    private ScoreCondition scoreCondition;
    private int scoreColor, darkenedScoreColor;
    private ArrayList<TimelineMetric> metrics;
    private @IdRes int sourceViewId;

    private FrameLayout rootView;
    private View header;
    private RecyclerView recycler;
    private @Nullable ViewGroup.LayoutParams finalRecyclerLayoutParams;

    private int savedStatusBarColor;
    private TextView titleText;
    private TextView summaryText;

    //region Lifecycle

    public static TimelineInfoFragment newInstance(@NonNull final Timeline timeline,
                                                   @IdRes final int sourceViewId) {
        final TimelineInfoFragment fragment = new TimelineInfoFragment();

        final Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_SUMMARY, timeline.getMessage());
        arguments.putString(ARG_SCORE_CONDITION, timeline.getScoreCondition().toString());
        arguments.putParcelableArrayList(ARG_METRICS, timeline.getMetrics());
        arguments.putInt(ARG_SOURCE_VIEW_ID, sourceViewId);
        fragment.setArguments(arguments);

        return fragment;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle arguments = getArguments();

        //noinspection ConstantConditions
        this.scoreCondition = ScoreCondition.fromString(arguments.getString(ARG_SCORE_CONDITION));
        this.scoreColor = getResources().getColor(scoreCondition.colorRes);
        this.darkenedScoreColor = Drawing.darkenColorBy(scoreColor, 0.2f);
        this.metrics = arguments.getParcelableArrayList(ARG_METRICS);
        this.sourceViewId = arguments.getInt(ARG_SOURCE_VIEW_ID);
        this.summary = arguments.getParcelable(ARG_SUMMARY);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // For the negative space edge effect to have the right color. Yes, really.
            final int styleRes = Styles.getScoreConditionTintThemeRes(scoreCondition);
            final ContextThemeWrapper themeContext = new ContextThemeWrapper(getActivity(), styleRes);
            inflater = LayoutInflater.from(themeContext);
        }

        this.rootView = (FrameLayout) inflater.inflate(R.layout.fragment_timeline_info, container, false);

        this.header = rootView.findViewById(R.id.fragment_timeline_info_header);
        Views.setSafeOnClickListener(header, stateSafeExecutor, ignored -> {
            getFragmentManager().popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final RippleDrawable headerBackground =
                    new RippleDrawable(ColorStateList.valueOf(darkenedScoreColor),
                                       new ColorDrawable(scoreColor), null);
            header.setBackground(headerBackground);
        } else {
            final StateListDrawable headerBackground = new StateListDrawable();
            headerBackground.addState(new int[] { android.R.attr.state_pressed },
                                      new ColorDrawable(darkenedScoreColor));
            headerBackground.addState(new int[] {}, new ColorDrawable(scoreColor));
            header.setBackground(headerBackground);
        }

        this.titleText = (TextView) header.findViewById(R.id.fragment_timeline_info_title);
        this.summaryText = (TextView) header.findViewById(R.id.fragment_timeline_info_summary);
        summaryText.setText(summary);

        this.recycler = (RecyclerView) rootView.findViewById(R.id.fragment_timeline_info_recycler);
        recycler.setVisibility(View.INVISIBLE);
        recycler.setLayoutManager(new GridLayoutManager(getActivity(), GRID_COLUMNS_PER_ROW));
        recycler.addItemDecoration(new Decoration(getResources()));
        recycler.setAdapter(new EmptyRecyclerAdapter());
        recycler.addOnScrollListener(new CollapseHeaderScrollListener());

        return rootView;
    }

    @Override
    protected Animator onProvideEnterAnimator() {
        final AnimatorSet compound = new AnimatorSet();

        compound.play(createFadeIn())
                .with(createHeaderReveal())
                .with(createRecyclerReveal());

        compound.setInterpolator(new FastOutSlowInInterpolator());
        compound.setDuration(Anime.DURATION_NORMAL);

        return compound;
    }

    @Override
    protected void onSkipEnterAnimator() {
        final Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            this.savedStatusBarColor = Windows.getStatusBarColor(window);
            Windows.setStatusBarColor(window, darkenedScoreColor);
        }

        header.setVisibility(View.VISIBLE);
        recycler.setVisibility(View.VISIBLE);
        rootView.setAlpha(1f);

        setUpRecycler();
    }

    @Override
    protected Animator onProvideExitAnimator() {
        final AnimatorSet compound = new AnimatorSet();

        compound.play(createFadeOut())
                .with(createHeaderDismissal())
                .with(createRecyclerDismissal());

        compound.setInterpolator(new FastOutLinearInInterpolator());
        compound.setDuration(Anime.DURATION_NORMAL);

        return compound;
    }

    @Override
    protected void onExitAnimatorEnd() {
        super.onExitAnimatorEnd();
        getActivity().onBackPressed();
    }

    /*
     *  #onDestroyView() intentionally omitted.
     *  Don't clear any of the view fields, we need
     *  them around for the dismissal animator.
     */

    private @Nullable Window getWindow() {
        final Activity activity = getActivity();
        return (activity != null) ? activity.getWindow() : null;
    }

    //endregion


    //region Showing

    private Animator createFadeIn() {
        final ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);

        final Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            this.savedStatusBarColor = window.getStatusBarColor();
            fadeIn.addUpdateListener(animator -> {
                final float fraction = animator.getAnimatedFraction();
                rootView.setAlpha(fraction);

                final int statusBar = Anime.interpolateColors(fraction,
                                                              savedStatusBarColor,
                                                              darkenedScoreColor);
                window.setStatusBarColor(statusBar);
            });
        } else {
            fadeIn.addUpdateListener(animator -> {
                final float fraction = animator.getAnimatedFraction();
                rootView.setAlpha(fraction);
            });
        }
        return fadeIn;
    }

    private Animator createHeaderReveal() {
        final ObjectAnimator animator = ObjectAnimator.ofFloat(header, "translationY",
                                                               -header.getMeasuredHeight(), 0f);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                header.setVisibility(View.VISIBLE);
            }
        });
        return animator;
    }

    private Animator createRecyclerReveal() {
        final ValueAnimator animator;

        final View fromView = findSourceView();
        if (fromView != null) {
            final Rect initialRect = new Rect();
            Views.getFrameInWindow(fromView, initialRect);

            final Rect finalRect = Views.copyFrame(recycler);
            finalRect.top += header.getBottom();
            animator = Views.createFrameAnimator(recycler, initialRect, finalRect);

            this.finalRecyclerLayoutParams = recycler.getLayoutParams();

            @SuppressLint("RtlHardcoded")
            final FrameLayout.LayoutParams initialLayoutParams =
                    new FrameLayout.LayoutParams(initialRect.width(),
                                                 initialRect.height(),
                                                 Gravity.TOP | Gravity.LEFT);
            initialLayoutParams.leftMargin = initialRect.left;
            initialLayoutParams.topMargin = initialRect.top;
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
                if (isAdded()) {
                    setUpRecycler();
                }
            }
        });
        return animator;
    }

    //endregion


    //region Hiding

    private Animator createRecyclerDismissal() {
        final ValueAnimator animator;

        final View fromView = findSourceView();
        if (fromView != null) {
            final Rect finalRect = new Rect();
            Views.getFrameInWindow(fromView, finalRect);

            final Rect initialRect = Views.copyFrame(recycler);
            initialRect.top += (header.getBottom() + header.getTranslationY());
            animator = Views.createFrameAnimator(recycler, initialRect, finalRect);
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
        final ObjectAnimator animator = ObjectAnimator.ofFloat(header, "translationY",
                                                               header.getTranslationY(),
                                                               -header.getMeasuredHeight());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                header.setVisibility(View.INVISIBLE);
            }
        });
        return animator;
    }

    private Animator createFadeOut() {
        final ValueAnimator fadeOut = ValueAnimator.ofFloat(0f, 1f);

        final Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
            final int oldStatusBarColor = window.getStatusBarColor();
            final int newStatusBarColor = this.savedStatusBarColor;
            fadeOut.addUpdateListener(animator -> {
                final float fraction = animator.getAnimatedFraction();
                rootView.setAlpha(1f - fraction);

                final int statusBar = Anime.interpolateColors(fraction,
                                                              oldStatusBarColor,
                                                              newStatusBarColor);
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
        final Activity activity = getActivity();
        if (activity != null) {
            if (activity instanceof AnchorProvider) {
                return ((AnchorProvider) activity).findAnimationAnchorView(sourceViewId);
            } else {
                return activity.findViewById(sourceViewId);
            }
        }

        return null;
    }

    private void setUpRecycler() {
        if (finalRecyclerLayoutParams != null) {
            recycler.setLayoutParams(finalRecyclerLayoutParams);
            this.finalRecyclerLayoutParams = null;
        }
        recycler.setAdapter(new MetricAdapter());
    }

    private void tearDownRecycler() {
        // Clearing the adapter through the notifyItem* APIs
        // causes a large number of frames to be dropped in
        // the out animation. Swapping the adapter fixes it.
        recycler.setAdapter(new EmptyRecyclerAdapter());
    }

    //endregion


    private class CollapseHeaderScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            final RecyclerView.ViewHolder topViewHolder = recycler.findViewHolderForAdapterPosition(0);
            final float headerHeight = header.getMeasuredHeight();
            final float amount = (topViewHolder == null) ? 0f : topViewHolder.itemView.getTop() / headerHeight;
            header.setTranslationY(headerHeight * -(1f - amount));

            titleText.setAlpha(Anime.interpolateFloats(amount, 0, 1));
            summaryText.setAlpha(Anime.interpolateFloats(amount, 0, 1));
        }
    }
    
    private class Decoration extends RecyclerView.ItemDecoration {
        private final Rect lineRect = new Rect();
        private final Paint linePaint = new Paint();
        private final int dividerSize;
        private final int verticalDividerInset;

        private Decoration(@NonNull Resources resources) {
            this.dividerSize = resources.getDimensionPixelSize(R.dimen.divider_size);
            this.verticalDividerInset = resources.getDimensionPixelSize(R.dimen.gap_medium);

            final int lineColor = resources.getColor(R.color.border);
            linePaint.setColor(lineColor);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            final int adapterPosition = parent.getChildAdapterPosition(view);
            if (adapterPosition < GRID_COLUMNS_PER_ROW) {
                outRect.top += header.getMeasuredHeight();
            }

            if ((adapterPosition % GRID_COLUMNS_PER_ROW) == 0) {
                outRect.right += dividerSize;
            }

            outRect.bottom += dividerSize;
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            for (int i = 0, size = parent.getChildCount(); i < size; i++) {
                final View child = parent.getChildAt(i);
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

    private class MetricAdapter extends RecyclerView.Adapter<MetricAdapter.ViewHolder> {
        private final LayoutInflater inflater = LayoutInflater.from(getActivity());
        private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

        @Override
        public int getItemCount() {
            return metrics.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = inflater.inflate(R.layout.item_timeline_info, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final TimelineMetric metric = metrics.get(position);

            holder.titleText.setText(metric.getName().stringRes);
            final Long value = metric.getValue();
            if (value != null) {
                if (metric.getUnit() == TimelineMetric.Unit.MINUTES) {
                    holder.readingText.setText(dateFormatter.formatDuration(value, TimeUnit.MINUTES));
                } else if (metric.getUnit() == TimelineMetric.Unit.TIMESTAMP) {
                    DateTime date = new DateTime(value);
                    boolean use24Time = preferences.getUse24Time();
                    holder.readingText.setText(dateFormatter.formatForTimelineInfo(date, use24Time));
                } else {
                    holder.readingText.setText(numberFormat.format(value));
                }
            } else {
                if (metric.getUnit() == TimelineMetric.Unit.CONDITION) {
                    holder.readingText.setText(getConditionText(metric.getName(), metric.getCondition()));
                } else {
                    holder.readingText.setText(R.string.missing_data_placeholder);
                }
            }

            final int valueColorRes = metric.getCondition().colorRes;
            holder.readingText.setTextColor(getResources().getColor(valueColorRes));
        }

        private @StringRes int getConditionText(@NonNull TimelineMetric.Name sensor,
                                                @NonNull Condition condition) {
            switch (condition) {
                case ALERT: {
                    switch (sensor) {
                        case SOUND:
                            return R.string.condition_short_hand_alert_sound;
                        case PARTICULATES:
                            return R.string.condition_short_hand_alert_airquality;
                        case LIGHT:
                            return R.string.condition_short_hand_alert_light;
                        default:
                            return R.string.condition_short_hand_alert;
                    }
                }
                case WARNING: {
                    switch (sensor) {
                        case SOUND:
                            return R.string.condition_short_hand_warning_sound;
                        case PARTICULATES:
                            return R.string.condition_short_hand_warning_airquality;
                        case LIGHT:
                            return R.string.condition_short_hand_warning_light;
                        default:
                            return R.string.condition_short_hand_warning;
                    }
                }
                case IDEAL: {
                    return R.string.condition_short_hand_ideal;
                }
            }

            return R.string.missing_data_placeholder;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView titleText;
            final TextView readingText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);

                this.titleText = (TextView) itemView.findViewById(R.id.item_timeline_info_title);
                this.readingText = (TextView) itemView.findViewById(R.id.item_timeline_info_reading);
            }
        }
    }

    public interface AnchorProvider {
        @Nullable View findAnimationAnchorView(@IdRes int viewId);
    }
}
