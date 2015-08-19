package is.hello.sense.ui.fragments;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentCallbacks2;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.adapter.TimelineAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayView;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.ExtendedItemAnimator;
import is.hello.sense.ui.recycler.StaggeredFadeItemAnimator;
import is.hello.sense.ui.widget.LoadingView;
import is.hello.sense.ui.widget.RotaryTimePickerDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.graphing.ColorDrawableCompat;
import is.hello.sense.ui.widget.timeline.TimelineHeaderView;
import is.hello.sense.ui.widget.timeline.TimelineInfoOverlay;
import is.hello.sense.ui.widget.timeline.TimelineNoDataHeaderView;
import is.hello.sense.ui.widget.timeline.TimelineToolbar;
import is.hello.sense.ui.widget.util.Dialogs;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Share;
import rx.Observable;
import rx.functions.Action1;

public class TimelineFragment extends InjectionFragment
        implements TimelineAdapter.OnItemClickListener, SlidingLayersView.Listener {
    // !! Important: Do not use setTargetFragment on TimelineFragment.
    // It is not guaranteed to exist at the time of state restoration.

    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";
    private static final String ARG_CACHED_TIMELINE = TimelineFragment.class.getName() + ".ARG_CACHED_TIMELINE";
    private static final String ARG_IS_FIRST_TIMELINE = TimelineFragment.class.getName() + ".ARG_IS_FIRST_TIMELINE";

    private static final int ID_EVENT_CORRECT = 0;
    private static final int ID_EVENT_ADJUST_TIME = 1;
    private static final int ID_EVENT_REMOVE = 2;
    private static final int ID_EVENT_INCORRECT = 3;


    @Inject TimelinePresenter timelinePresenter;
    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;
    @Inject LocalUsageTracker localUsageTracker;

    private HomeActivity homeActivity;

    private boolean firstTimeline;
    private boolean hasCreatedView = false;
    private boolean animationEnabled = true;

    private RecyclerView recyclerView;

    private LinearLayoutManager layoutManager;
    private TimelineToolbar toolbar;
    private TimelineHeaderView headerView;
    private TimelineAdapter adapter;
    private StaggeredFadeItemAnimator itemAnimator;
    private ColorDrawableCompat backgroundFill;

    private boolean controlsSharedChrome = false;

    private @Nullable TutorialOverlayView tutorialOverlay;
    private @Nullable WeakReference<Dialog> activeDialog;

    private TimelineInfoOverlay infoOverlay;


    //region Lifecycle

    public static TimelineFragment newInstance(@NonNull LocalDate date,
                                               @Nullable Timeline cachedTimeline,
                                               boolean isFirstTimeline) {
        TimelineFragment fragment = new TimelineFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DATE, date);
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

        LocalDate date = getDate();
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

        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_timeline_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.addOnScrollListener(new ScrollListener());

        this.layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        this.animationEnabled = !hasCreatedView && !(firstTimeline && homeActivity.getWillShowUnderside());

        this.toolbar = new TimelineToolbar(getActivity());
        toolbar.setOverflowOnClickListener(ignored -> homeActivity.toggleUndersideVisible());
        toolbar.setOverflowOpen(homeActivity.isUndersideVisible());

        toolbar.setTitleOnClickListener(ignored -> {
            Tutorial.ZOOM_OUT_TIMELINE.markShown(getActivity());
            homeActivity.showTimelineNavigator(getDate(), getCachedTimeline());
        });
        toolbar.setTitle(getTitle());
        toolbar.setTitleDimmed(homeActivity.isUndersideVisible());

        toolbar.setShareOnClickListener(this::share);
        toolbar.setShareVisible(false);


        this.headerView = new TimelineHeaderView(getActivity());
        headerView.setAnimatorContext(getAnimatorContext());
        headerView.setAnimationEnabled(animationEnabled);
        headerView.setOnScoreClickListener(this::showBreakdown);

        View[] headers = { toolbar, headerView };

        this.itemAnimator = new StaggeredFadeItemAnimator(getAnimatorContext());
        itemAnimator.setEnabled(ExtendedItemAnimator.Action.ADD, animationEnabled);
        recyclerView.setItemAnimator(itemAnimator);
        recyclerView.addItemDecoration(new BottomInsetDecoration(getResources(), headers.length));

        int backgroundFillColor = getResources().getColor(R.color.background_timeline);
        this.backgroundFill = new ColorDrawableCompat(backgroundFillColor);
        recyclerView.setBackground(backgroundFill);

        this.adapter = new TimelineAdapter(getActivity(), dateFormatter, headers);
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
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW &&
                adapter != null && adapter.isSoundPlayerDisposable()) {
            adapter.destroySoundPlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (infoOverlay != null) {
            infoOverlay.dismiss(false);
        }

        adapter.stopSoundPlayer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        headerView.clearAnimation();
        itemAnimator.endAnimations();
        itemAnimator.removeAllListeners();

        recyclerView.setAdapter(null);

        this.headerView = null;
        this.recyclerView = null;
        this.layoutManager = null;
        this.adapter = null;
        this.itemAnimator = null;
        this.backgroundFill = null;

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

    @Override
    public void onTopViewWillSlideDown() {
        if (infoOverlay != null) {
            infoOverlay.dismiss(false);
        }

        toolbar.setOverflowOpen(true);
        toolbar.setTitleDimmed(true);
        toolbar.setShareVisible(false);
    }

    @Override
    public void onTopViewDidSlideUp() {
        toolbar.setOverflowOpen(false);
        toolbar.setTitleDimmed(false);
        toolbar.setShareVisible(adapter.hasEvents());
    }

    public void share(@NonNull View sender) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_SHARE, null);

        if (infoOverlay != null) {
            infoOverlay.dismiss(false);
        }

        bindAndSubscribe(timelinePresenter.latest(),
                timeline -> {
                    Integer score = timeline.getScore();
                    if (score == null) {
                        return;
                    }

                    LocalDate date = timelinePresenter.getDate();
                    String scoreString = score.toString();
                    String shareCopy;
                    if (DateFormatter.isLastNight(date)) {
                        shareCopy = getString(R.string.timeline_share_last_night_fmt, scoreString);
                    } else {
                        String dateString = dateFormatter.formatAsTimelineDate(date);
                        shareCopy = getString(R.string.timeline_share_other_days_fmt, scoreString, dateString);
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
        Analytics.trackEvent(Analytics.Timeline.EVENT_SLEEP_SCORE_BREAKDOWN, null);

        if (infoOverlay != null) {
            infoOverlay.dismiss(false);
        }

        bindAndSubscribe(timelinePresenter.latest(),
                         timeline -> {
                             TimelineInfoFragment infoOverlay = TimelineInfoFragment.newInstance(timeline, headerView.getCardViewId());
                             infoOverlay.show(getFragmentManager(), R.id.activity_home_container, TimelineInfoFragment.TAG);
                         },
                         Functions.LOG_ERROR);
    }

    //endregion


    //region Hooks

    @SuppressWarnings("ConstantConditions")
    public @NonNull LocalDate getDate() {
        return (LocalDate) getArguments().getSerializable(ARG_DATE);
    }

    public @Nullable Timeline getCachedTimeline() {
        return (Timeline) getArguments().getSerializable(ARG_CACHED_TIMELINE);
    }

    public @NonNull String getTitle() {
        return dateFormatter.formatAsTimelineDate(getDate());
    }

    public void setControlsSharedChrome(boolean controlsSharedChrome) {
        this.controlsSharedChrome = controlsSharedChrome;
    }

    public void scrollToTop() {
        recyclerView.smoothScrollToPosition(0);
    }

    public void update() {
        transitionOutOfNoDataState();
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
        tutorialOverlay.show(R.id.activity_home_container);
    }

    private void showHandholdingIfAppropriate() {
        if (WelcomeDialogFragment.isAnyVisible(getActivity())) {
            return;
        }

        boolean showZoomOutTutorial = Tutorial.ZOOM_OUT_TIMELINE.shouldShow(getActivity());
        if (firstTimeline && showZoomOutTutorial) {
            SharedPreferences preferences = getActivity().getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
            int numberTimelinesShown = preferences.getInt(Constants.HANDHOLDING_NUMBER_TIMELINES_SHOWN, 0);
            if (numberTimelinesShown < 5) {
                Logger.debug(getClass().getSimpleName(), "Incrementing timelines shown to " + (numberTimelinesShown + 1));

                preferences.edit()
                        .putInt(Constants.HANDHOLDING_NUMBER_TIMELINES_SHOWN, numberTimelinesShown + 1)
                        .apply();

                showZoomOutTutorial = false;
            }
        }

        if (homeActivity.getWillShowUnderside()) {
            WelcomeDialogFragment.markShown(homeActivity, R.xml.welcome_dialog_timeline);
        } else if (!homeActivity.isUndersideVisible()) {
            if (WelcomeDialogFragment.shouldShow(homeActivity, R.xml.welcome_dialog_timeline)) {
                WelcomeDialogFragment.show(homeActivity, R.xml.welcome_dialog_timeline);
            } else if (Tutorial.SWIPE_TIMELINE.shouldShow(getActivity())) {
                showTutorial(Tutorial.SWIPE_TIMELINE);
            } else if (showZoomOutTutorial) {
                showTutorial(Tutorial.ZOOM_OUT_TIMELINE);
            }
        }
    }

    private class HandholdingOneShotListener implements StaggeredFadeItemAnimator.Listener {
        @Override
        public void onItemAnimatorWillStart(@NonNull AnimatorContext.Transaction transaction) {
        }

        @Override
        public void onItemAnimatorDidStop(boolean finished) {
            if (finished) {
                showHandholdingIfAppropriate();
            }

            itemAnimator.removeListener(this);
        }
    }

    //endregion


    //region Binding

    private void transitionIntoNoDataState(@NonNull Action1<TimelineNoDataHeaderView> configurer) {
        TimelineNoDataHeaderView newHeader = new TimelineNoDataHeaderView(getActivity());
        configurer.call(newHeader);

        if (animationEnabled && ViewCompat.isLaidOut(recyclerView)) {
            itemAnimator.setDelayEnabled(false);
            itemAnimator.setEnabled(ExtendedItemAnimator.Action.REMOVE, true);
            itemAnimator.setTemplate(itemAnimator.getTemplate()
                                                 .withDuration(Anime.DURATION_SLOW));
            itemAnimator.addListener(new ExtendedItemAnimator.Listener() {
                final Animator crossFade = backgroundFill.colorAnimator(
                        getResources().getColor(R.color.background_timeline));

                @Override
                public void onItemAnimatorWillStart(@NonNull AnimatorContext.Transaction transaction) {
                    transaction.takeOwnership(crossFade);
                }

                @Override
                public void onItemAnimatorDidStop(boolean finished) {
                    crossFade.cancel();

                    itemAnimator.removeListener(this);
                }
            });
        } else {
            backgroundFill.setColor(getResources().getColor(R.color.background_timeline));
        }

        toolbar.setShareVisible(false);
        headerView.stopPulsing();
        adapter.replaceHeader(1, newHeader);
    }

    private void transitionOutOfNoDataState() {
        if (adapter.getHeader(1) == headerView) {
            return;
        }

        itemAnimator.setEnabled(ExtendedItemAnimator.Action.ADD, false);
        itemAnimator.setEnabled(ExtendedItemAnimator.Action.REMOVE, false);
        itemAnimator.setTemplate(itemAnimator.getTemplate()
                                             .withDuration(Anime.DURATION_NORMAL));

        adapter.replaceHeader(1, headerView);
        headerView.setBackgroundSolid(false, 0);
        headerView.startPulsing();

        // Run after change listener
        recyclerView.post(() -> {
            itemAnimator.setDelayEnabled(true);
            itemAnimator.setEnabled(ExtendedItemAnimator.Action.ADD, true);
        });
    }

    public void bindTimeline(@NonNull Timeline timeline) {
        boolean hasEvents = !Lists.isEmpty(timeline.getEvents());
        if (hasEvents) {
            Runnable backgroundAnimations = stateSafeExecutor.bind(() -> {
                int targetColor = getResources().getColor(R.color.timeline_background_fill);
                Animator backgroundFade = backgroundFill.colorAnimator(targetColor);
                backgroundFade.start();

                toolbar.setShareVisible(!homeActivity.isUndersideVisible());
            });
            Runnable adapterAnimations = stateSafeExecutor.bind(() -> {
                if (animationEnabled) {
                    itemAnimator.addListener(new HandholdingOneShotListener());
                } else {
                    getAnimatorContext().runWhenIdle(stateSafeExecutor.bind(this::showHandholdingIfAppropriate));
                }

                adapter.bindEvents(timeline.getEvents());
            });
            headerView.bindTimeline(timeline, backgroundAnimations, adapterAnimations);

            if (animationEnabled) {
                localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.TIMELINE_SHOWN_WITH_DATA);
            }
        } else {
            transitionIntoNoDataState(header -> {
                // Indicates on-boarding just ended
                if (homeActivity.getWillShowUnderside()) {
                    header.setDiagramResource(R.drawable.timeline_state_first_night);
                    header.setTitle(R.string.title_timeline_not_enough_data);
                    header.setMessage(R.string.message_timeline_first_night);
                } else {
                    header.setDiagramResource(R.drawable.timeline_state_no_data);
                    header.setTitle(R.string.title_timeline_not_enough_data);
                    header.setMessage(timeline.getMessage());
                }
            });
        }

        headerView.setScoreClickEnabled(!Lists.isEmpty(timeline.getMetrics()) && hasEvents);
    }

    public void timelineUnavailable(Throwable e) {
        Analytics.trackError(e, "Loading Timeline");
        CharSequence message = getString(R.string.timeline_error_message);
        if (adapter.hasEvents()) {
            Toast toast = new Toast(homeActivity.getApplicationContext());
            @SuppressLint("InflateParams")
            TextView text = (TextView) homeActivity.getLayoutInflater().inflate(R.layout.toast_text, null);
            text.setText(message);
            toast.setView(text);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();
        } else {
            transitionIntoNoDataState(header -> {
                header.setDiagramResource(R.drawable.timeline_state_error);
                header.setTitle(R.string.timeline_error_title);
                header.setMessage(message);
            });
        }
    }

    //endregion


    //region Acting on Items

    @Override
    public void onSegmentItemClicked(int position, View view, @NonNull TimelineEvent event) {
        boolean animateShow = true;
        if (infoOverlay != null) {
            animateShow = false;
            infoOverlay.dismiss(false);
        }

        this.infoOverlay = new TimelineInfoOverlay(getActivity(), getAnimatorContext());
        infoOverlay.setOnDismiss(sender -> {
            if (sender == infoOverlay) {
                this.infoOverlay = null;
            }
        });
        infoOverlay.bindEvent(event);
        infoOverlay.show(view, animateShow);

        Analytics.trackEvent(Analytics.Timeline.EVENT_TAP, null);
        Analytics.trackEvent(Analytics.Timeline.EVENT_LONG_PRESS_EVENT, null);
    }

    @Override
    public void onEventItemClicked(int eventPosition, @NonNull TimelineEvent event) {
        if (infoOverlay != null) {
            infoOverlay.dismiss(true);
        }

        if (event.hasActions()) {
            showAvailableActions(event);
        } else {
            showNoActionsAvailable();
        }

        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_EVENT_TAPPED, null);
    }

    private void showNoActionsAvailable() {
        SenseBottomSheet noActions = new SenseBottomSheet(getActivity());
        noActions.setTitle(R.string.message_timeline_no_actions_title);
        noActions.setMessage(R.string.message_timeline_no_actions_body);
        noActions.setWantsBigTitle(true);
        noActions.show();

        this.activeDialog = new WeakReference<>(noActions);
    }

    private void showAvailableActions(@NonNull TimelineEvent event) {
        SharedPreferences preferences = homeActivity.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);

        SenseBottomSheet actions = new SenseBottomSheet(getActivity());
        if (!preferences.getBoolean(Constants.HANDHOLDING_HAS_SHOWN_TIMELINE_ADJUST_INTRO, false)) {
            actions.setTitle(R.string.timeline_actions_intro_title);
            actions.setMessage(R.string.timeline_actions_intro_message);
            actions.setWantsBigTitle(true);
        }
        actions.setWantsDividers(true);

        if (event.supportsAction(TimelineEvent.Action.VERIFY)) {
            actions.addOption(
                    new SenseBottomSheet.Option(ID_EVENT_CORRECT)
                            .setTitle(R.string.action_timeline_mark_event_correct)
                            .setIcon(R.drawable.timeline_action_correct)
            );
        }
        if (event.supportsAction(TimelineEvent.Action.ADJUST_TIME)) {
            actions.addOption(
                    new SenseBottomSheet.Option(ID_EVENT_ADJUST_TIME)
                            .setTitle(R.string.action_timeline_event_adjust_time)
                            .setIcon(R.drawable.timeline_action_adjust)
            );
        }
        if (event.supportsAction(TimelineEvent.Action.REMOVE)) {
            actions.addOption(
                    new SenseBottomSheet.Option(ID_EVENT_REMOVE)
                            .setTitle(R.string.action_timeline_event_remove)
                            .setIcon(R.drawable.timeline_action_remove)
            );
        }
        if (event.supportsAction(TimelineEvent.Action.INCORRECT)) {
            actions.addOption(
                    new SenseBottomSheet.Option(ID_EVENT_INCORRECT)
                            .setTitle(R.string.action_timeline_event_incorrect)
                            .setIcon(R.drawable.timeline_action_remove)
            );
        }

        actions.setOnOptionSelectedListener((option) -> {
            preferences.edit()
                       .putBoolean(Constants.HANDHOLDING_HAS_SHOWN_TIMELINE_ADJUST_INTRO, true)
                       .apply();

            JSONObject properties = Analytics.createProperties(
                Analytics.Timeline.PROP_TYPE, event.getType().toString()
            );
            switch (option.getOptionId()) {
                case ID_EVENT_CORRECT: {
                    doEventAction(actions, timelinePresenter.verifyEvent(event));
                    Analytics.trackEvent(Analytics.Timeline.EVENT_CORRECT, properties);
                    return false;
                }
                case ID_EVENT_ADJUST_TIME: {
                    adjustTime(event);
                    Analytics.trackEvent(Analytics.Timeline.EVENT_ADJUST_TIME, properties);
                    return true;
                }
                case ID_EVENT_INCORRECT:  {
                    doEventAction(actions, timelinePresenter.deleteEvent(event));
                    Analytics.trackEvent(Analytics.Timeline.EVENT_INCORRECT, properties);
                    return false;
                }
                case ID_EVENT_REMOVE: {
                    doEventAction(actions, timelinePresenter.deleteEvent(event));
                    Analytics.trackEvent(Analytics.Timeline.EVENT_REMOVE, properties);
                    return false;
                }
                default: {
                    Logger.warn(getClass().getSimpleName(), "Unknown option " + option);
                    return true;
                }
            }
        });
        actions.show();

        this.activeDialog = new WeakReference<>(actions);
    }

    private void adjustTime(@NonNull TimelineEvent event) {
        DateTime initialTime = event.getShiftedTimestamp();
        RotaryTimePickerDialog.OnTimeSetListener listener = (hourOfDay, minuteOfHour) -> {
            LocalTime newTime = new LocalTime(hourOfDay, minuteOfHour, 0);
            completeAdjustTime(event, newTime);
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

    private void completeAdjustTime(@NonNull TimelineEvent event, @NonNull LocalTime newTime) {
        LoadingDialogFragment dialogFragment = LoadingDialogFragment.show(getFragmentManager(),
                getString(R.string.dialog_loading_message),
                LoadingDialogFragment.OPAQUE_BACKGROUND);
        dialogFragment.setDismissMessage(R.string.title_thank_you);
        bindAndSubscribe(timelinePresenter.amendEventTime(event, newTime),
                ignored -> {
                    LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), null);
                },
                e -> {
                    LoadingDialogFragment.close(getFragmentManager());
                    ErrorDialogFragment.presentError(getFragmentManager(), e);
                });
    }

    private void doEventAction(@NonNull SenseBottomSheet bottomSheet, @NonNull Observable<Void> action) {
        if (homeActivity != null) {
            Dialogs.disableOrientationChangesUntilDismissed(bottomSheet, homeActivity);
        }

        LoadingView loadingView = new LoadingView(getActivity());
        bottomSheet.replaceContent(loadingView, null);
        bottomSheet.setCancelable(false);
        bindAndSubscribe(action,
                ignored -> {
                    loadingView.playDoneTransition(R.string.title_thank_you, bottomSheet::dismiss);
                },
                e -> {
                    bottomSheet.dismiss();
                    ErrorDialogFragment.presentError(getFragmentManager(), e);
                });
    }

    //endregion


    private class ScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState != RecyclerView.SCROLL_STATE_IDLE && infoOverlay != null) {
                infoOverlay.dismiss(false);
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
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

    static class BottomInsetDecoration extends RecyclerView.ItemDecoration {
        private final int bottomPadding;
        private final int headerCount;

        public BottomInsetDecoration(@NonNull Resources resources, int headerCount) {
            this.bottomPadding = resources.getDimensionPixelSize(R.dimen.timeline_gap_bottom);
            this.headerCount = headerCount;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position >= headerCount && position == parent.getAdapter().getItemCount() - 1) {
                outRect.bottom = bottomPadding;
            }
        }
    }
}
