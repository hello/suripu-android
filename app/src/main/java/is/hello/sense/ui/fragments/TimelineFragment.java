package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Feedback;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.adapter.TimelineAdapter;
import is.hello.sense.ui.animation.AnimatorContext;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayView;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.RotaryTimePickerDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.ui.widget.timeline.TimelineFadeItemAnimator;
import is.hello.sense.ui.widget.timeline.TimelineHeaderView;
import is.hello.sense.ui.widget.timeline.TimelineInfoPopup;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Share;

public class TimelineFragment extends InjectionFragment implements TimelineAdapter.OnItemClickListener {
    // !! Important: Do not use setTargetFragment on TimelineFragment.
    // It is not guaranteed to exist at the time of state restoration.

    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";
    private static final String ARG_CACHED_TIMELINE = TimelineFragment.class.getName() + ".ARG_CACHED_TIMELINE";
    private static final String ARG_IS_FIRST_TIMELINE = TimelineFragment.class.getName() + ".ARG_IS_FIRST_TIMELINE";

    private static final int ID_EVENT_CORRECT = 0;
    private static final int ID_EVENT_ADJUST_TIME = 1;
    private static final int ID_EVENT_REMOVE = 2;


    @Inject TimelinePresenter timelinePresenter;
    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;

    private HomeActivity homeActivity;

    private boolean firstTimeline;
    private boolean hasCreatedView = false;
    private boolean animationEnabled = true;

    private View contentShadow;
    private RecyclerView recyclerView;

    private LinearLayoutManager layoutManager;
    private TimelineHeaderView headerView;
    private TimelineAdapter adapter;
    private TimelineFadeItemAnimator itemAnimator;

    private boolean wantsShareButton = false;
    private boolean controlsSharedChrome = false;

    private @Nullable TutorialOverlayView tutorialOverlay;
    private @Nullable WeakReference<Dialog> activeDialog;

    private TimelineInfoPopup infoPopup;


    //region Lifecycle

    public static TimelineFragment newInstance(@NonNull DateTime date,
                                               @Nullable Timeline cachedTimeline,
                                               boolean isFirstTimeline) {
        TimelineFragment fragment = new TimelineFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DATE, date.withTimeAtStartOfDay());
        arguments.putSerializable(ARG_CACHED_TIMELINE, cachedTimeline);
        arguments.putBoolean(ARG_IS_FIRST_TIMELINE, isFirstTimeline);
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

        DateTime date = getDate();
        JSONObject properties = Analytics.createProperties(
            Analytics.Timeline.PROP_DATE, date.toString()
        );
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE, properties);

        this.firstTimeline = getArguments().getBoolean(ARG_IS_FIRST_TIMELINE, false);

        timelinePresenter.setDateWithTimeline(date, getCachedTimeline());
        addPresenter(timelinePresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);

        this.contentShadow = view.findViewById(R.id.fragment_timeline_content_shadow);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_timeline_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new ScrollListener());

        this.layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        this.animationEnabled = !hasCreatedView && !(firstTimeline && homeActivity.getWillShowUnderside());

        this.headerView = new TimelineHeaderView(getActivity());
        headerView.setAnimatorContext(getAnimatorContext());
        headerView.setAnimationEnabled(animationEnabled);
        headerView.setOnScoreClickListener(this::showBreakdown);
        headerView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                contentShadow.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                contentShadow.setVisibility(View.VISIBLE);
            }
        });

        this.itemAnimator = new TimelineFadeItemAnimator(getAnimatorContext());
        itemAnimator.setEnabled(animationEnabled);
        itemAnimator.addListener(headerView);
        recyclerView.setItemAnimator(itemAnimator);
        recyclerView.addItemDecoration(new BackgroundDecoration(getResources()));

        this.adapter = new TimelineAdapter(getActivity(), headerView, dateFormatter);
        adapter.setOnItemClickListener(stateSafeExecutor, this);
        recyclerView.setAdapter(adapter);

        this.hasCreatedView = true;

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stateSafeExecutor.execute(headerView::startPulsing);

        bindAndSubscribe(timelinePresenter.timeline,
                this::bindTimeline,
                this::timelineUnavailable);

        bindAndSubscribe(preferences.observableUse24Time(),
                adapter::setUse24Time,
                Functions.LOG_ERROR);
    }

    @Override
    public void onPause() {
        super.onPause();

        adapter.stopPlayback();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        headerView.clearAnimation();
        itemAnimator.removeAllListeners();

        recyclerView.setAdapter(null);

        this.headerView = null;
        this.recyclerView = null;
        this.layoutManager = null;
        this.adapter = null;
        this.itemAnimator = null;

        if (infoPopup != null) {
            infoPopup.dismiss();
            this.infoPopup = null;
        }

        if (tutorialOverlay != null) {
            tutorialOverlay.dismiss(false);
        }

        if (activeDialog != null) {
            Dialog dialog = activeDialog.get();
            if (dialog != null) {
                dialog.dismiss();
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        this.homeActivity = null;
    }

    //endregion


    //region Actions

    public void share() {
        Analytics.trackEvent(Analytics.Timeline.EVENT_SHARE, null);

        bindAndSubscribe(timelinePresenter.latest(),
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

    public void showBreakdown(@NonNull View sender) {
        bindAndSubscribe(timelinePresenter.latest(),
                         timeline -> {
                             TimelineInfoFragment infoOverlay = TimelineInfoFragment.newInstance(timeline, headerView.getCardViewId());
                             infoOverlay.show(getFragmentManager(), R.id.activity_home_container, TimelineInfoFragment.TAG);
                         },
                         Functions.LOG_ERROR);
    }

    //endregion


    //region Hooks

    public @NonNull DateTime getDate() {
        return (DateTime) getArguments().getSerializable(ARG_DATE);
    }

    public @Nullable Timeline getCachedTimeline() {
        return (Timeline) getArguments().getSerializable(ARG_CACHED_TIMELINE);
    }

    public @NonNull String getTitle() {
        return dateFormatter.formatAsTimelineDate(getDate());
    }

    public boolean getWantsShareButton() {
        return wantsShareButton;
    }

    public void setControlsSharedChrome(boolean controlsSharedChrome) {
        this.controlsSharedChrome = controlsSharedChrome;
    }

    public void scrollToTop() {
        recyclerView.smoothScrollToPosition(0);
    }

    public void update() {
        timelinePresenter.update();
    }

    //endregion


    //region Handholding

    private void showTutorial(@NonNull Tutorial tutorial) {
        if (tutorialOverlay != null) {
            return;
        }

        this.tutorialOverlay = new TutorialOverlayView(getActivity(), tutorial);
        tutorialOverlay.setOnDismiss(() -> {
            this.tutorialOverlay = null;
        });
        tutorialOverlay.setAnimatorContext(getAnimatorContext());
        tutorialOverlay.show(R.id.activity_home_container);
    }

    private void showHandholdingIfAppropriate() {
        if (WelcomeDialogFragment.isAnyVisible(getActivity())) {
            return;
        }

        if (homeActivity.getWillShowUnderside()) {
            WelcomeDialogFragment.markShown(homeActivity, R.xml.welcome_dialog_timeline);
        } else if (!homeActivity.isUndersideVisible()) {
            if (WelcomeDialogFragment.shouldShow(homeActivity, R.xml.welcome_dialog_timeline)) {
                WelcomeDialogFragment.show(homeActivity, R.xml.welcome_dialog_timeline);
            } else if (Tutorial.SWIPE_TIMELINE.shouldShow(getActivity())) {
                showTutorial(Tutorial.SWIPE_TIMELINE);
            }
        }
    }

    private class HandholdingOneShotListener implements TimelineFadeItemAnimator.Listener {
        @Override
        public void onTimelineAnimationWillStart(@NonNull AnimatorContext animatorContext, @NonNull AnimatorContext.TransactionFacade transactionFacade) {
        }

        @Override
        public void onTimelineAnimationDidEnd(boolean finished) {
            if (finished) {
                showHandholdingIfAppropriate();
            }

            itemAnimator.removeListener(this);
        }
    }

    //endregion


    //region Binding

    public void bindTimeline(@NonNull Timeline timeline) {
        // For timeline corrections
        LoadingDialogFragment.close(getFragmentManager());

        boolean hasSegments = !Lists.isEmpty(timeline.getSegments());
        Runnable continuation = stateSafeExecutor.bind(() -> {
            if (animationEnabled) {
                itemAnimator.addListener(new HandholdingOneShotListener());
            } else {
                getAnimatorContext().runWhenIdle(stateSafeExecutor.bind(this::showHandholdingIfAppropriate));
            }

            adapter.bindSegments(timeline.getSegments());

            if (controlsSharedChrome) {
                homeActivity.setShareButtonVisible(wantsShareButton);
            }
        });

        this.wantsShareButton = hasSegments;
        if (hasSegments) {
            headerView.bindScore(timeline.getScore(), continuation);
        } else {
            headerView.bindScore(TimelineHeaderView.NULL_SCORE, continuation);
        }

        headerView.bindMessage(timeline.getMessage());

        headerView.setScoreClickEnabled(timeline.getStatistics() != null && hasSegments);
    }

    public void timelineUnavailable(Throwable e) {
        // For timeline corrections
        LoadingDialogFragment.close(getFragmentManager());

        adapter.clear();
        headerView.bindError(e);
    }

    //endregion


    //region Acting on Items

    @Override
    public void onSegmentItemClicked(int position, View view, @NonNull TimelineSegment segment) {
        if (infoPopup != null) {
            infoPopup.dismiss();
        }

        this.infoPopup = new TimelineInfoPopup(getActivity());
        infoPopup.bindSegment(segment);
        infoPopup.show(view);

        Analytics.trackEvent(Analytics.Timeline.EVENT_LONG_PRESS_EVENT, null);
    }

    @Override
    public void onEventItemClicked(int segmentPosition, @NonNull TimelineSegment segment) {
        if (infoPopup != null) {
            infoPopup.dismiss();
        }

        SenseBottomSheet actions = new SenseBottomSheet(getActivity());

        actions.addOption(
                new SenseBottomSheet.Option(ID_EVENT_CORRECT)
                        .setTitle(R.string.action_timeline_mark_event_correct)
                        .setIcon(R.drawable.timeline_action_correct)
                        .setEnabled(false)
        );
        if (segment.isTimeAdjustable()) {
            actions.addOption(
                    new SenseBottomSheet.Option(ID_EVENT_ADJUST_TIME)
                            .setTitle(R.string.action_timeline_event_adjust_time)
                            .setIcon(R.drawable.timeline_action_adjust)
            );
        }
        actions.addOption(
                new SenseBottomSheet.Option(ID_EVENT_REMOVE)
                        .setTitle(R.string.action_timeline_event_remove)
                        .setIcon(R.drawable.timeline_action_remove)
                        .setEnabled(false)
        );

        actions.setWantsDividers(true);
        actions.setOnOptionSelectedListener((optionPosition, option) -> {
            switch (option.getOptionId()) {
                case ID_EVENT_CORRECT: {
                    markCorrect(segmentPosition);
                    break;
                }

                case ID_EVENT_ADJUST_TIME: {
                    adjustTime(segmentPosition);
                    break;
                }

                case ID_EVENT_REMOVE: {
                    removeEvent(segmentPosition);
                    break;
                }

                default: {
                    Logger.warn(getClass().getSimpleName(), "Unknown option " + option);
                    break;
                }
            }
        });
        actions.show();

        this.activeDialog = new WeakReference<>(actions);
    }

    private void adjustTime(int position) {
        TimelineSegment segment = adapter.getSegment(position);
        DateTime initialTime = segment.getShiftedTimestamp();
        RotaryTimePickerDialog.OnTimeSetListener listener = (pickerView, hourOfDay, minuteOfHour) -> {
            LocalTime newTime = new LocalTime(hourOfDay, minuteOfHour, 0);
            completeAdjustTime(position, newTime);
        };
        RotaryTimePickerDialog timePicker = new RotaryTimePickerDialog(
                getActivity(),
                listener,
                initialTime.getHourOfDay(),
                initialTime.getMinuteOfHour(),
                preferences.getUse24Time()
        );
        timePicker.show();

        this.activeDialog = new WeakReference<>(timePicker);
    }

    private void completeAdjustTime(int position, @NonNull LocalTime newTime) {
        TimelineSegment segment = adapter.getSegment(position);
        DateTime originalTime = segment.getShiftedTimestamp();

        Feedback feedback = new Feedback();
        feedback.setEventType(segment.getEventType());
        feedback.setNight(getDate().toLocalDate());
        feedback.setOldTime(originalTime.toLocalTime());
        feedback.setNewTime(newTime);

        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(timelinePresenter.submitCorrection(feedback),
                ignored -> {
                    // Loading dialog is dismissed in #bindRenderedTimeline
                    timelinePresenter.update();
                },
                e -> {
                    LoadingDialogFragment.close(getFragmentManager());
                    ErrorDialogFragment.presentBluetoothError(getFragmentManager(), e);
                });
    }

    private void markCorrect(int segmentPosition) {

    }

    private void removeEvent(int segmentPosition) {

    }

    //endregion


    private class ScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (headerView.getParent() != null) {
                float headerBottom = headerView.getBottom();
                float headerHeight = headerView.getMeasuredHeight();
                float headerFadeAmount = headerBottom / headerHeight;
                headerView.setChildFadeAmount(headerFadeAmount);
            }

            if (!itemAnimator.isRunning()) {
                int recyclerHeight = recyclerView.getMeasuredHeight(),
                    recyclerCenter = recyclerHeight / 2;
                for (int i = recyclerView.getChildCount() - 1; i >= 0; i--) {
                    View view = recyclerView.getChildAt(i);
                    RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
                    if (viewHolder instanceof TimelineAdapter.EventViewHolder) {
                        TimelineAdapter.EventViewHolder eventViewHolder = (TimelineAdapter.EventViewHolder) viewHolder;
                        int viewTop = view.getTop(),
                            viewBottom = view.getBottom(),
                            viewHeight = viewBottom - viewTop,
                            viewCenter = (viewTop + viewBottom) / 2;

                        float centerDistanceAmount = (viewCenter - recyclerCenter) / (float) recyclerCenter;
                        float bottomDistanceAmount;
                        if (viewBottom < recyclerHeight) {
                            bottomDistanceAmount = 1f;
                        } else {
                            float offScreen = viewBottom - recyclerHeight;
                            bottomDistanceAmount = 1f - (offScreen / viewHeight);
                        }
                        eventViewHolder.setDistanceAmounts(bottomDistanceAmount, centerDistanceAmount);
                    }
                }
            }

            if (controlsSharedChrome) {
                if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                    homeActivity.showAlarmShortcut();
                } else {
                    homeActivity.hideAlarmShortcut();
                }
            }
        }
    }

    static class BackgroundDecoration extends RecyclerView.ItemDecoration {
        private final Drawable background;
        private final int bottomPadding;

        public BackgroundDecoration(@NonNull Resources resources) {
            this.background = ResourcesCompat.getDrawable(resources, R.drawable.background_timeline_segment, null);
            this.bottomPadding = resources.getDimensionPixelSize(R.dimen.timeline_gap_bottom);
        }

        @Override
        public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            int left = 0,
                top = 0,
                right = canvas.getWidth(),
                bottom = canvas.getHeight();

            RecyclerView.ViewHolder headerView = parent.findViewHolderForAdapterPosition(0);
            if (headerView != null) {
                top = headerView.itemView.getBottom();
            }

            background.setBounds(left, top, right, bottom);
            background.draw(canvas);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position > 0 && position == parent.getAdapter().getItemCount() - 1) {
                outRect.bottom = bottomPadding;
            }
        }
    }
}
