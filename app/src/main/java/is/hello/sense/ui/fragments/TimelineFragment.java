package is.hello.sense.ui.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Feedback;
import is.hello.sense.api.model.PreSleepInsight;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.adapter.TimelineSegmentAdapter;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.TimelineEventDialogFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.BlockableLinearLayout;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.TimelineHeaderDrawable;
import is.hello.sense.ui.widget.TimelineTooltipDrawable;
import is.hello.sense.ui.widget.util.ListViews;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;
import is.hello.sense.util.SafeOnClickListener;
import is.hello.sense.util.Share;
import rx.Observable;
import rx.functions.Action1;

import static is.hello.sense.ui.animation.Animation.Transition;

public class TimelineFragment extends InjectionFragment implements SlidingLayersView.OnInteractionListener, AdapterView.OnItemClickListener, SelectorLinearLayout.OnSelectionChangedListener, TimelineEventDialogFragment.AdjustTimeFragment, AdapterView.OnItemLongClickListener {
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";
    private static final String ARG_CACHED_TIMELINE = TimelineFragment.class.getName() + ".ARG_CACHED_TIMELINE";

    @Inject DateFormatter dateFormatter;
    @Inject TimelinePresenter timelinePresenter;
    @Inject PreferencesPresenter preferences;
    @Inject Markdown markdown;

    private ListView listView;
    private TimelineSegmentAdapter segmentAdapter;

    private ImageButton menuButton;
    private ImageButton shareButton;

    private BlockableLinearLayout headerView;
    private TextView dateText;
    private FrameLayout headerViewContainer;
    private HeaderViewMode headerMode;
    private SelectorLinearLayout headerModeSelector;

    private ScoreViewMode timelineScore;
    private BeforeSleepHeaderMode beforeSleep;
    private @Nullable BreakdownHeaderMode breakdownHeaderMode;

    private View timelineEventsHeader;

    private HomeActivity homeActivity;
    private boolean controlsAlarmShortcut = false;
    private TimelineHeaderDrawable headerTabsBackground;

    private @Nullable PopupWindow timelinePopup;


    public static TimelineFragment newInstance(@NonNull DateTime date, @Nullable Timeline cachedTimeline) {
        TimelineFragment fragment = new TimelineFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DATE, date.withTimeAtStartOfDay());
        arguments.putSerializable(ARG_CACHED_TIMELINE, cachedTimeline);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.homeActivity = (HomeActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        timelinePresenter.setDateWithTimeline(getDate(), getCachedTimeline());
        addPresenter(timelinePresenter);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        this.segmentAdapter = new TimelineSegmentAdapter(getActivity(), dateFormatter);

        Observable<Boolean> use24HourTime = preferences.observableUse24Time();
        track(use24HourTime.subscribe(segmentAdapter::setUse24Time));

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);


        this.headerView = (BlockableLinearLayout) inflater.inflate(R.layout.sub_fragment_timeline_header, listView, false);
        this.headerTabsBackground = new TimelineHeaderDrawable(getResources());

        this.dateText = (TextView) headerView.findViewById(R.id.fragment_timeline_date);
        dateText.setText(dateFormatter.formatAsTimelineDate(timelinePresenter.getDate()));
        dateText.setOnClickListener(ignored -> {
            Timeline timeline = (Timeline) dateText.getTag();
            homeActivity.showTimelineNavigator(getDate(), timeline);
        });

        this.headerViewContainer = (FrameLayout) headerView.findViewById(R.id.sub_fragment_timeline_header_container);

        this.headerModeSelector = (SelectorLinearLayout) headerView.findViewById(R.id.sub_fragment_timeline_header_mode);
        headerModeSelector.setVisibility(View.INVISIBLE);
        headerModeSelector.setSelectionAwareDrawable(headerTabsBackground);
        headerModeSelector.setOnSelectionChangedListener(this);
        headerModeSelector.setSelectedIndex(0);

        this.timelineScore = new ScoreViewMode(inflater, headerViewContainer);
        this.beforeSleep = new BeforeSleepHeaderMode(inflater, headerViewContainer);

        setHeaderMode(timelineScore, null);

        ListViews.addHeaderView(listView, headerView, null, false);


        this.timelineEventsHeader = new View(getActivity());
        timelineEventsHeader.setBackgroundResource(R.drawable.background_timeline_top);
        timelineEventsHeader.setVisibility(View.INVISIBLE);
        timelineEventsHeader.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.timeline_header_gradient_height));
        ListViews.addHeaderView(listView, timelineEventsHeader, null, false);


        this.menuButton = (ImageButton) headerView.findViewById(R.id.fragment_timeline_header_menu);
        Views.setSafeOnClickListener(menuButton, ignored -> {
            homeActivity.getSlidingLayersView().toggle();
            scrollToTop();
        });

        this.shareButton = (ImageButton) headerView.findViewById(R.id.fragment_timeline_header_share);
        shareButton.setVisibility(View.INVISIBLE);
        Views.setSafeOnClickListener(shareButton, this::share);

        listView.setAdapter(segmentAdapter);
        listView.setOnScrollListener(new TimelineScrollListener());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Timeline> boundMainTimeline = bind(timelinePresenter.timeline);
        subscribe(boundMainTimeline, this::bindTimeline, this::timelineUnavailable);

        Observable<List<TimelineSegment>> segments = boundMainTimeline.map(timeline -> {
            if (timeline != null) {
                return timeline.getSegments();
            } else {
                return Collections.emptyList();
            }
        });
        subscribe(segments, segmentAdapter::bindSegments, segmentAdapter::handleError);

        bindAndSubscribe(timelinePresenter.message,
                         timelineScore.messageText::setText,
                         Functions.LOG_ERROR);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.breakdownHeaderMode = null;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (timelinePopup != null) {
            timelinePopup.dismiss();
            this.timelinePopup = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        dateText.setText(dateFormatter.formatAsTimelineDate(timelinePresenter.getDate()));
    }


    //region Headers

    private void setHeaderMode(@NonNull HeaderViewMode headerMode,
                               @Nullable Transition<ViewGroup, ViewGroup.LayoutParams> transition) {
        if (this.headerMode == headerMode) {
            return;
        }

        View oldView = null;
        if (this.headerMode != null) {
            oldView = this.headerMode.view;
        }

        this.headerMode = headerMode;

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                             ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = headerMode.gravity;
        if (transition != null) {
            headerView.setTouchEnabled(false);
            transition.perform(headerViewContainer, headerMode.view, layoutParams, () -> {
                headerView.setTouchEnabled(true);
            });
        } else {
            if (oldView != null) {
                headerViewContainer.removeView(oldView);
            }
            headerViewContainer.addView(headerMode.view, layoutParams);
        }
    }

    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        switch (newSelectionIndex) {
            case 0:
                if (breakdownHeaderMode != null) {
                    setHeaderMode(timelineScore, this::hideBreakdownTransition);
                } else {
                    setHeaderMode(timelineScore, Animation::crossFade);
                }
                break;
            case 1:
                setHeaderMode(beforeSleep, Animation::crossFade);
                this.breakdownHeaderMode = null;
                break;
            default:
                break;
        }

    }

    public void showBreakdownTransition(@NonNull ViewGroup container,
                                        @Nullable View newView,
                                        @Nullable ViewGroup.LayoutParams layoutParams,
                                        @Nullable Runnable onCompletion) {
        if (newView == null || breakdownHeaderMode == null) {
            throw new IllegalArgumentException();
        }

        breakdownHeaderMode.leftItems.setAlpha(0f);
        breakdownHeaderMode.rightItems.setAlpha(0f);
        newView.setAlpha(0f);
        container.addView(newView, 0, layoutParams);

        AnimatorConfig config = AnimatorConfig.create();
        config.duration = Animation.DURATION_FAST;
        getAnimatorContext().transaction(config, f -> {
            f.animate(timelineScore.messageText).fadeOut(View.VISIBLE);
            f.animate(timelineScore.scoreTextLabel).fadeOut(View.VISIBLE);
        }, firstFinished -> {
            if (!firstFinished) {
                return;
            }

            Resources resources = getResources();
            float bigWidth = resources.getDimension(R.dimen.grand_sleep_summary_width);
            float smallWidth = resources.getDimension(R.dimen.little_sleep_summary_width);

            View bigScore = timelineScore.sleepScoreContainer;
            bigScore.setPivotX(bigWidth / 2f);
            bigScore.setPivotY(0.0f);

            View smallScore = breakdownHeaderMode.score;
            smallScore.setPivotX(smallWidth / 2f);
            smallScore.setPivotY(0.0f);

            smallScore.setScaleX(bigWidth / smallWidth);
            smallScore.setScaleY(bigWidth / smallWidth);

            getAnimatorContext().transaction(config, f -> {
                f.animate(bigScore).scale(smallWidth / bigWidth);
                f.animate(smallScore).scale(1f);
                f.animate(timelineScore.view).fadeOut(View.VISIBLE);
                f.animate(newView).fadeIn();
            }, finished -> {
                if (!finished) {
                    return;
                }

                bigScore.setScaleX(1f);
                bigScore.setScaleY(1f);

                timelineScore.messageText.setAlpha(1f);
                timelineScore.scoreTextLabel.setAlpha(1f);

                container.removeView(timelineScore.view);
                timelineScore.view.setAlpha(1f);

                float delta = resources.getDimension(R.dimen.gap_tiny);
                getAnimatorContext().transaction(f -> {
                    f.animate(breakdownHeaderMode.leftItems).slideXAndFade(delta, 0f, 0f, 1f);
                    f.animate(breakdownHeaderMode.rightItems).slideXAndFade(-delta, 0f, 0f, 1f);
                }, finishedLast -> {
                    if (finishedLast && onCompletion != null) {
                        onCompletion.run();
                    }
                });
            });
        });
    }

    public void hideBreakdownTransition(@NonNull ViewGroup container,
                                        @Nullable View newView,
                                        @Nullable ViewGroup.LayoutParams layoutParams,
                                        @Nullable Runnable onCompletion) {
        if (newView == null || breakdownHeaderMode == null) {
            throw new IllegalArgumentException();
        }

        newView.setAlpha(0f);
        container.addView(newView, 0, layoutParams);

        timelineScore.messageText.setAlpha(0f);
        timelineScore.scoreTextLabel.setAlpha(0f);

        Resources resources = getResources();
        float bigWidth = resources.getDimension(R.dimen.grand_sleep_summary_width);
        float smallWidth = resources.getDimension(R.dimen.little_sleep_summary_width);

        View bigScore = timelineScore.sleepScoreContainer;
        bigScore.setPivotX(bigWidth / 2f);
        bigScore.setPivotY(0.0f);

        View smallScore = breakdownHeaderMode.score;
        smallScore.setPivotX(smallWidth / 2f);
        smallScore.setPivotY(0.0f);

        bigScore.setScaleX(smallWidth / bigWidth);
        bigScore.setScaleY(smallWidth / bigWidth);

        AnimatorConfig config = AnimatorConfig.create();
        config.duration = Animation.DURATION_FAST;
        getAnimatorContext().transaction(config, f -> {
            f.animate(bigScore).scale(1f);
            f.animate(smallScore).scale(bigWidth / smallWidth);
            f.animate(breakdownHeaderMode.view).fadeOut(View.VISIBLE);
            f.animate(newView).fadeIn();
        }, finished -> {
            if (!finished) {
                return;
            }

            container.removeView(breakdownHeaderMode.view);
            this.breakdownHeaderMode = null;

            getAnimatorContext().transaction(f -> {
                f.animate(timelineScore.messageText).fadeIn();
                f.animate(timelineScore.scoreTextLabel).fadeIn();
            }, finishedLast -> {
                if (finishedLast && onCompletion != null) {
                    onCompletion.run();
                }
            });
        });
    }

    public void hideBreakdown(@NonNull View sender) {
        setHeaderMode(timelineScore, this::hideBreakdownTransition);
        headerModeSelector.setSelectedIndex(0);
    }

    public void showBreakdown(@NonNull View sender) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_SLEEP_SCORE_BREAKDOWN, null);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        this.breakdownHeaderMode = new BreakdownHeaderMode(inflater, headerViewContainer);
        bindAndSubscribe(timelinePresenter.timeline.take(1),
                         breakdownHeaderMode::bindTimeline,
                         breakdownHeaderMode::timelineUnavailable);
        setHeaderMode(breakdownHeaderMode, this::showBreakdownTransition);
		
        TutorialOverlayFragment.markShown(getActivity(), Tutorial.SLEEP_SCORE_BREAKDOWN);
    }

    public void share(@NonNull View sender) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_SHARE, null);

        Observable<Timeline> currentTimeline = timelinePresenter.timeline.take(1);
        bindAndSubscribe(currentTimeline,
                         timeline -> {
                             DateTime date = timelinePresenter.getDate();
                             String score = Integer.toString(timeline.getScore());
                             String shareCopy;
                             if (DateFormatter.isLastNight(date)) {
                                 shareCopy = getString(R.string.timeline_share_last_night_fmt, score);
                             } else {
                                 String dateString = dateFormatter.formatAsTimelineDate(date);
                                 shareCopy = getString(R.string.timeline_share_other_days_fmt, score, dateString);
                             }

                             Share.text(shareCopy)
                                  .withSubject(getString(R.string.app_name))
                                  .send(getActivity());
                         },
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Cannot bind for sharing", e);
                         });
    }

    //endregion


    private void showHandholdingIfAppropriate() {
        if (homeActivity.getWillShowUnderside()) {
            WelcomeDialogFragment.markShown(homeActivity, R.xml.welcome_dialog_timeline);
        } else {
            getAnimatorContext().runWhenIdle(coordinator.bind(() -> {
                if (WelcomeDialogFragment.shouldShow(homeActivity, R.xml.welcome_dialog_timeline)) {
                    WelcomeDialogFragment.show(homeActivity, R.xml.welcome_dialog_timeline);
                } else if (TutorialOverlayFragment.shouldShow(getActivity(), Tutorial.SLEEP_SCORE_BREAKDOWN)) {
                    TutorialOverlayFragment.show(getFragmentManager(), Tutorial.SLEEP_SCORE_BREAKDOWN);
                } else if (TutorialOverlayFragment.shouldShow(getActivity(), Tutorial.SWIPE_TIMELINE)) {
                    TutorialOverlayFragment.show(getFragmentManager(), Tutorial.SWIPE_TIMELINE);
                }
            }));
        }
    }

    public void bindTimeline(@Nullable Timeline timeline) {
        if (timeline != null) {
            boolean hasSegments = !Lists.isEmpty(timeline.getSegments());
            timelineScore.showSleepScore(hasSegments ? timeline.getScore() : -1);

            if (hasSegments) {
                timelineEventsHeader.setVisibility(View.VISIBLE);
                shareButton.setVisibility(View.VISIBLE);

                headerView.setBackground(headerTabsBackground);
                headerModeSelector.setVisibility(View.VISIBLE);

                showHandholdingIfAppropriate();
            } else {
                timelineEventsHeader.setVisibility(View.INVISIBLE);
                shareButton.setVisibility(View.INVISIBLE);
            }
        } else {
            timelineScore.showSleepScore(-1);
            timelineEventsHeader.setVisibility(View.INVISIBLE);
            shareButton.setVisibility(View.INVISIBLE);
        }

        beforeSleep.bindTimeline(timeline);
        dateText.setTag(timeline);
    }

    public void timelineUnavailable(@Nullable Throwable e) {
        timelineScore.presentError(e);
        beforeSleep.presentError(e);
        shareButton.setVisibility(View.INVISIBLE);
        dateText.setTag(null);
    }


    public @NonNull DateTime getDate() {
        return (DateTime) getArguments().getSerializable(ARG_DATE);
    }

    public @Nullable Timeline getCachedTimeline() {
        return (Timeline) getArguments().getSerializable(ARG_CACHED_TIMELINE);
    }


    @Override
    public void onUserWillPullDownTopView() {
        menuButton.setImageResource(R.drawable.icon_menu_open);
        dateText.setTextColor(getResources().getColor(R.color.text_dim));
        shareButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onUserDidPushUpTopView() {
        menuButton.setImageResource(R.drawable.icon_menu_closed);
        dateText.setTextColor(getResources().getColor(R.color.text_dark));
        if (segmentAdapter.getCount() > 0) {
            shareButton.setVisibility(View.VISIBLE);
        }
    }

    public void scrollToTop() {
        listView.smoothScrollToPositionFromTop(0, 0);
    }


    //region Event Details

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        TimelineSegment segment = (TimelineSegment) adapterView.getItemAtPosition(position);
        if (segment.hasEventInfo()) {
            Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_EVENT_TAPPED, null);

            TimelineEventDialogFragment dialogFragment = TimelineEventDialogFragment.newInstance(segment);
            dialogFragment.setTargetFragment(this, 0x00);
            dialogFragment.show(getFragmentManager(), TimelineEventDialogFragment.TAG);
        }

        Analytics.trackEvent(Analytics.Timeline.EVENT_TAP, null);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (timelinePopup != null) {
            timelinePopup.dismiss();
            this.timelinePopup = null;
        }

        TimelineSegment segment = (TimelineSegment) parent.getItemAtPosition(position);
        if (segment == null) {
            return false;
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        TextView contents = (TextView) inflater.inflate(R.layout.tooltip_timeline_overlay, parent, false);
        contents.setBackground(new TimelineTooltipDrawable(getResources()));

        if (segment.getEventType() == null) {
            contents.setText(Styles.getWakingDepthStringRes(segment.getSleepDepth()));
        } else {
            String sleepDepthSummary = getString(Styles.getSleepDepthStringRes(segment.getSleepDepth()));
            String tooltipHtml = getString(R.string.tooltip_timeline_html_fmt, sleepDepthSummary);
            contents.setText(Html.fromHtml(tooltipHtml));
        }

        this.timelinePopup = new PopupWindow(contents);
        timelinePopup.setAnimationStyle(R.style.WindowAnimations_PopSlideAndFade);
        timelinePopup.setTouchable(true);
        timelinePopup.setOutsideTouchable(true);
        timelinePopup.setBackgroundDrawable(new ColorDrawable()); // Required for touch to dismiss
        timelinePopup.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Lollipop does not correctly inset popup windows for the soft
        // navigation bar on the bottom of the screen, we have to do it
        // ourselves. So stupid.
        int navigationBarHeight = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Display display = getActivity().getWindowManager().getDefaultDisplay();

            Point realSize = new Point();
            display.getRealSize(realSize);

            Point visibleArea = new Point();
            display.getSize(visibleArea);

            // Status bar is counted as part of the display's height,
            // so the delta just gives us the navigation bar height.
            navigationBarHeight = realSize.y - visibleArea.y;
        }

        int parentHeight = parent.getMeasuredHeight();
        int bottomInset = parentHeight - view.getTop() + navigationBarHeight;
        timelinePopup.showAtLocation(parent, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, bottomInset);
        parent.setOnTouchListener((ignored, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (timelinePopup != null) {
                    contents.postDelayed(timelinePopup::dismiss, 1000);
                }
                parent.setOnTouchListener(null);
            }
            return false;
        });

        Analytics.trackEvent(Analytics.Timeline.EVENT_LONG_PRESS_EVENT, null);

        return true;
    }

    @Override
    public void onAdjustSegmentTime(@NonNull TimelineSegment segment,
                                    @NonNull LocalTime newTime,
                                    @NonNull Action1<Boolean> continuation) {
        Feedback correction = new Feedback();
        correction.setEventType(segment.getEventType());
        correction.setNight(getDate().toLocalDate());
        correction.setOldTime(segment.getShiftedTimestamp().toLocalTime());
        correction.setNewTime(newTime);
        bindAndSubscribe(timelinePresenter.submitCorrection(correction),
                ignored -> {
                    continuation.call(true);
                }, e -> {
                    ErrorDialogFragment.presentError(getFragmentManager(), e);
                    continuation.call(false);
                });
    }

    //endregion


    //region Alarm Button

    public void setControlsAlarmShortcut(boolean controlsAlarmShortcut) {
        this.controlsAlarmShortcut = controlsAlarmShortcut;
    }

    private class TimelineScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScroll(AbsListView listView, int firstVisiblePosition, int visibleItemCount, int totalItemCount) {
            if (!controlsAlarmShortcut) {
                return;
            }

            if (firstVisiblePosition == 0 && ListViews.getEstimatedScrollY(listView) == 0) {
                homeActivity.showAlarmShortcut();
            } else {
                homeActivity.hideAlarmShortcut();
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    }

    //endregion


    //region Header Modes

    class HeaderViewMode {
        final View view;
        final int gravity;

        HeaderViewMode(@LayoutRes int layoutRes,
                       @NonNull LayoutInflater inflater,
                       @NonNull ViewGroup container,
                       int gravity) {
            this.view = inflater.inflate(layoutRes, container, false);
            this.gravity = gravity;
        }
    }

    class ScoreViewMode extends HeaderViewMode {
        final LinearLayout sleepScoreContainer;
        final SleepScoreDrawable scoreGraph;
        final TextView scoreTextLabel;
        final TextView scoreText;
        final TextView messageText;

        ScoreViewMode(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
            super(R.layout.sub_fragment_timeline_score, inflater, container, Gravity.TOP);

            this.sleepScoreContainer = (LinearLayout) view.findViewById(R.id.fragment_timeline_sleep_score_chart);
            Views.setSafeOnClickListener(sleepScoreContainer, TimelineFragment.this::showBreakdown);
            sleepScoreContainer.setClickable(false);

            this.scoreTextLabel = (TextView) sleepScoreContainer.findViewById(R.id.fragment_timeline_sleep_score_label);
            this.scoreText = (TextView) sleepScoreContainer.findViewById(R.id.fragment_timeline_sleep_score);
            this.messageText = (TextView) view.findViewById(R.id.fragment_timeline_message);
            Views.makeTextViewLinksClickable(messageText);

            this.scoreGraph = new SleepScoreDrawable(getResources());
            sleepScoreContainer.setBackground(scoreGraph);
        }


        void showSleepScore(int sleepScore) {
            if (sleepScore < 0) {
                sleepScoreContainer.setClickable(false);
                scoreGraph.setFillColor(getResources().getColor(R.color.sensor_unknown));
                scoreText.setText(R.string.missing_data_placeholder);
                scoreGraph.setValue(0);
            } else {
                sleepScoreContainer.setClickable(true);

                if (sleepScore != scoreGraph.getValue()) {
                    ValueAnimator updateAnimation = ValueAnimator.ofInt(scoreGraph.getValue(), sleepScore);
                    AnimatorConfig.createWithDelay(250).apply(updateAnimation);

                    ArgbEvaluator colorEvaluator = new ArgbEvaluator();
                    int startColor = Styles.getSleepScoreColor(getActivity(), scoreGraph.getValue());
                    int endColor = Styles.getSleepScoreColor(getActivity(), sleepScore);
                    updateAnimation.addUpdateListener(a -> {
                        Integer score = (Integer) a.getAnimatedValue();
                        int color = (int) colorEvaluator.evaluate(a.getAnimatedFraction(), startColor, endColor);

                        scoreGraph.setValue(score);
                        scoreGraph.setFillColor(color);

                        scoreText.setText(score.toString());
                        scoreText.setTextColor(color);
                    });
                    updateAnimation.addListener(getAnimatorContext());

                    getAnimatorContext().runWhenIdle(updateAnimation::start);
                }
            }
        }

        void presentError(Throwable e) {
            sleepScoreContainer.setClickable(false);

            scoreGraph.setTrackColor(getResources().getColor(R.color.border));
            scoreGraph.setValue(0);

            scoreText.setText(R.string.missing_data_placeholder);
            scoreText.setTextColor(getResources().getColor(R.color.text_dark));

            if (e != null) {
                messageText.setText(getString(R.string.timeline_error_message, e.getMessage()));
            } else {
                messageText.setText(R.string.missing_data_placeholder);
            }
        }
    }

    class BeforeSleepHeaderMode extends HeaderViewMode {
        final LinearLayout container;
        final LayoutInflater inflater;

        BeforeSleepHeaderMode(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
            super(R.layout.sub_fragment_timeline_before_sleep, inflater, container, Gravity.CENTER_VERTICAL);

            this.container = (LinearLayout) view.findViewById(R.id.sub_fragment_timeline_before_sleep_container);
            this.inflater = inflater;
        }


        TextView inflateNewItem() {
            return (TextView) inflater.inflate(R.layout.item_before_sleep_insight, container, false);
        }

        void bindTimeline(@Nullable Timeline timeline) {
            container.removeAllViews();

            if (timeline != null && !Lists.isEmpty(timeline.getPreSleepInsights())) {
                Context context = getActivity();
                View.OnClickListener onClick = new SafeOnClickListener(ignored -> {
                    Analytics.trackEvent(Analytics.Timeline.EVENT_BEFORE_SLEEP_EVENT_TAPPED, null);
                });
                for (PreSleepInsight insight : timeline.getPreSleepInsights()) {
                    TextView text = inflateNewItem();
                    text.setCompoundDrawablesRelativeWithIntrinsicBounds(insight.getIcon(context), null, null, null);
                    text.setText(insight.getMessage());
                    text.setOnClickListener(onClick);
                    markdown.renderInto(text, insight.getMessage());
                    container.addView(text);
                }
            } else {
                TextView text = inflateNewItem();
                text.setGravity(Gravity.CENTER);
                text.setText(R.string.placeholder_no_before_sleep_insights);
                container.addView(text);
            }
        }

        void presentError(@Nullable Throwable e) {
            container.removeAllViews();

            TextView text = inflateNewItem();
            text.setGravity(Gravity.CENTER);
            if (e != null) {
                text.setText(getString(R.string.timeline_error_message, e.getMessage()));
            } else {
                text.setText(R.string.missing_data_placeholder);
            }
            container.addView(text);
        }
    }

    class BreakdownHeaderMode extends HeaderViewMode {
        final TextView score;

        final View leftItems;
        final TextView totalSleep;
        final TextView timesAwake;

        final View rightItems;
        final TextView soundSleep;
        final TextView timeToSleep;

        final SleepScoreDrawable sleepScorePie;

        BreakdownHeaderMode(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
            super(R.layout.sub_fragment_timeline_breakdown, inflater, container, Gravity.TOP);

            this.score = (TextView) view.findViewById(R.id.sub_fragment_timeline_breakdown_score);

            this.leftItems = view.findViewById(R.id.sub_fragment_timeline_breakdown_left);
            this.totalSleep = (TextView) view.findViewById(R.id.sub_fragment_timeline_breakdown_total);
            this.timesAwake = (TextView) view.findViewById(R.id.sub_fragment_timeline_breakdown_awake);

            this.rightItems = view.findViewById(R.id.sub_fragment_timeline_breakdown_right);
            this.soundSleep = (TextView) view.findViewById(R.id.sub_fragment_timeline_breakdown_sound);
            this.timeToSleep = (TextView) view.findViewById(R.id.sub_fragment_timeline_breakdown_time);

            this.sleepScorePie = new SleepScoreDrawable(getResources());
            score.setBackground(sleepScorePie);
            Views.setSafeOnClickListener(score, TimelineFragment.this::hideBreakdown);
        }

        @NonNull String formatTime(@Nullable Integer time) {
            if (time == null) {
                return getString(R.string.missing_data_placeholder);
            } else {
                int hours = time / 60;
                int seconds = time % 60;
                if (seconds > 0) {
                    return hours + "." + seconds + "h";
                } else {
                    return hours + "h";
                }
            }
        }

        void bindTimeline(@NonNull Timeline timeline) {
            int sleepScore = timeline.getScore();
            score.setText(Integer.toString(sleepScore));

            int color = Styles.getSleepScoreColor(getActivity(), sleepScore);
            sleepScorePie.setTrackColor(color);
            score.setTextColor(color);

            Timeline.Statistics statistics = timeline.getStatistics();
            if (statistics != null) {
                totalSleep.setTextColor(color);
                totalSleep.setText(formatTime(statistics.getTotalSleep()));

                timesAwake.setTextColor(color);
                if (statistics.getTimesAwake() != null) {
                    timesAwake.setText(statistics.getTimesAwake().toString());
                } else {
                    timesAwake.setText(R.string.missing_data_placeholder);
                }

                soundSleep.setTextColor(color);
                soundSleep.setText(formatTime(statistics.getSoundSleep()));

                timeToSleep.setText(formatTime(statistics.getTimeToSleep()));
                timeToSleep.setTextColor(color);
            } else {
                int noDataColor = getResources().getColor(R.color.sensor_unknown);

                totalSleep.setTextColor(noDataColor);
                totalSleep.setText(R.string.missing_data_placeholder);

                timesAwake.setTextColor(noDataColor);
                timesAwake.setText(R.string.missing_data_placeholder);

                soundSleep.setTextColor(noDataColor);
                soundSleep.setText(R.string.missing_data_placeholder);

                timeToSleep.setTextColor(noDataColor);
                timeToSleep.setText(R.string.missing_data_placeholder);
            }
        }

        void timelineUnavailable(Throwable e) {
            sleepScorePie.setValue(0);
            score.setText(R.string.missing_data_placeholder);

            totalSleep.setText(R.string.missing_data_placeholder);
            timesAwake.setText(R.string.missing_data_placeholder);
            soundSleep.setText(R.string.missing_data_placeholder);
            timeToSleep.setText(R.string.missing_data_placeholder);
        }
    }

    //endregion
}
