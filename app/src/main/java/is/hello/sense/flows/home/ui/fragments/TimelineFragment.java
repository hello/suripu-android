package is.hello.sense.flows.home.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.segment.analytics.Properties;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.flows.home.interactors.LastNightInteractor;
import is.hello.sense.flows.home.ui.views.TimelineView;
import is.hello.sense.flows.timeline.ui.activities.TimelineActivity;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.TimelineInteractor;
import is.hello.sense.mvp.fragments.PresenterFragment;
import is.hello.sense.permissions.ExternalStoragePermission;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.adapter.TimelineAdapter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayView;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.StaggeredFadeItemAnimator;
import is.hello.sense.ui.widget.LoadingView;
import is.hello.sense.ui.widget.RotaryTimePickerDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.ui.widget.timeline.TimelineImageGenerator;
import is.hello.sense.ui.widget.timeline.TimelineInfoOverlay;
import is.hello.sense.ui.widget.util.Dialogs;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Share;
import rx.Observable;

public class TimelineFragment extends PresenterFragment<TimelineView>
        implements TimelineAdapter.OnItemClickListener {
    // !! Important: Do not use setTargetFragment on TimelineFragment.
    // It is not guaranteed to exist at the time of state restoration.

    //region static
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";
    private static final String ARG_CACHED_TIMELINE = TimelineFragment.class.getName() + ".ARG_CACHED_TIMELINE";

    private static final int REQUEST_CODE_CHANGE_SRC = 0x50;

    public static final int ZOOMED_OUT_TIMELINE_REQUEST = 101;
    private static final int ID_EVENT_CORRECT = 0;
    private static final int ID_EVENT_ADJUST_TIME = 1;
    private static final int ID_EVENT_REMOVE = 2;
    private static final int ID_EVENT_INCORRECT = 3;
    private static final double TOOL_TIP_HEIGHT_MULTIPLIER = 3.25; // 3 for top+bottom+text height. .25 for a little white space.

    public static TimelineFragment newInstance(@NonNull final LocalDate date,
                                               @Nullable final Timeline cachedTimeline) {
        final TimelineFragment fragment = new TimelineFragment();

        final Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DATE, date);
        arguments.putSerializable(ARG_CACHED_TIMELINE, cachedTimeline);
        fragment.setArguments(arguments);

        return fragment;
    }

    //endregion

    @Inject
    TimelineInteractor timelineInteractor;
    @Inject
    DateFormatter dateFormatter;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    LocalUsageTracker localUsageTracker;
    @Inject
    LastNightInteractor lastNightInteractor;

    @Nullable
    private WeakReference<Dialog> activeDialog;

    private final ExternalStoragePermission externalStoragePermission = new ExternalStoragePermission(this);

    @VisibleForTesting
    TimelineInfoOverlay infoOverlay;

    @VisibleForTesting
    Parent parent;

    @VisibleForTesting
    int toolTipHeight;

    //region PresenterFragment
    @Override
    public void initializeSenseView() {
        if (this.senseView == null) {
            this.senseView = new TimelineView(getActivity(),
                                              getAnimatorContext(),
                                              createAdapter(),
                                              new ScrollListener(),
                                              this::showBreakdown);
        }
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (this.senseView == null) {
            return;
        }
        if (isVisibleToUser) {
            // For all subsequent fragments running below Nougat
            // setUserVisibleHint called before attached to activity
            // not a reliable way to determine current view pager fragment
            bindIfNeeded();
            this.senseView.setAnimationEnabled(true);
        } else {
            this.senseView.setAnimationEnabled(false);
            dismissVisibleOverlaysAndDialogs();
            this.senseView.clearHeader();
        }
    }

    @Override
    public void onAttach(@NonNull final Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Parent && this.parent == null) {
            setParent((Parent) activity);
        } else if (activity instanceof ParentProvider && this.parent == null) {
            setParent(((ParentProvider) activity).getTimelineParent());
        } else if (this.parent == null) {
            throw new IllegalStateException("A parent is required to control TimelineFragment");
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final LocalDate date = getDate();
        final Properties properties = Analytics.createProperties(Analytics.Timeline.PROP_DATE,
                                                                 date.toString());
        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE, properties);


        this.timelineInteractor.setDateWithTimeline(date, getCachedTimeline());
        addInteractor(this.timelineInteractor);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.toolTipHeight = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX,
                                                              getResources().getDimensionPixelSize(R.dimen.x2),
                                                              getResources().getDisplayMetrics()) * TOOL_TIP_HEIGHT_MULTIPLIER);
        // For the first fragment
        bindIfNeeded();
    }

    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            this.senseView.destroySoundPlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (this.infoOverlay != null) {
            this.infoOverlay.dismiss(false);
        }

        this.senseView.stopSoundPlayer();
        if (this.timelineInteractor.hasValidTimeline()) {
            final Timeline timeline = this.timelineInteractor.timeline.getValue();
            if (timeline == null) {
                return;
            }
            this.senseView.renderTimeline(timeline);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissVisibleOverlaysAndDialogs();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.parent = null;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_CHANGE_SRC) {
            this.timelineInteractor.update();
        } else if (requestCode == ZOOMED_OUT_TIMELINE_REQUEST && data != null) {
            final LocalDate date = (LocalDate) data.getSerializableExtra(TimelineActivity.EXTRA_LOCAL_DATE);
            final Timeline timeline = (Timeline) data.getSerializableExtra(TimelineActivity.EXTRA_TIMELINE);
            this.parent.jumpTo(date, timeline);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (externalStoragePermission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            share();
        } else {
            externalStoragePermission.showEnableInstructionsDialog();
        }
    }

    //endregion

    //region Actions
    public void showTimelineNavigator(@NonNull final LocalDate date,
                                      @Nullable final Timeline timeline) {
        startActivityForResult(TimelineActivity.getZoomedOutIntent(getActivity(),
                                                                   date,
                                                                   timeline),
                               ZOOMED_OUT_TIMELINE_REQUEST);
    }

    private void onShareIconClicked(final View ignored) {
        share();
    }

    private void onHistoryIconClicked(final View ignored) {
        dismissVisibleOverlaysAndDialogs();
        showTimelineNavigator(getDate(), getCachedTimeline());
    }

    public void share() {
        Analytics.trackEvent(Analytics.Timeline.EVENT_SHARE, null);

        if (this.infoOverlay != null) {
            this.infoOverlay.dismiss(false);
        }

        bindAndSubscribe(this.timelineInteractor.latest(),
                         timeline -> {
                             final Bitmap screenShot = TimelineImageGenerator.createShareableTimeline(getActivity(), timeline);
                             if (screenShot == null) {
                                 return;
                             }
                             Share.image(screenShot)
                                  .send(this);
                         },
                         e -> Logger.error(getClass().getSimpleName(), "Cannot bind for sharing", e));
    }

    public void showBreakdown(@NonNull final View sender) {
        if (this.infoOverlay != null) {
            this.infoOverlay.dismiss(false);
        }
        bindAndSubscribe(this.timelineInteractor.latest(),
                         this::showBreakDown,
                         Functions.LOG_ERROR);
    }

    //endregion

    @VisibleForTesting
    void showBreakDown(@NonNull final Timeline timeline) {
        startActivity(TimelineActivity.getInfoIntent(getActivity(), timeline));
    }


    @VisibleForTesting
    TimelineAdapter createAdapter() {
        final TimelineAdapter timelineAdapter = new TimelineAdapter(getActivity(),
                                                                    this.dateFormatter,
                                                                    this.dateFormatter.formatAsTimelineDate(getDate()),
                                                                    this::onHistoryIconClicked,
                                                                    this::onShareIconClicked);
        timelineAdapter.setOnItemClickListener(this.stateSafeExecutor, this);
        return timelineAdapter;
    }

    @VisibleForTesting
    void bindIfNeeded() {
        if (getView() != null && getUserVisibleHint()) {
            if (!hasSubscriptions()) {
                this.timelineInteractor.updateIfEmpty();

                this.stateSafeExecutor.execute(this.senseView::pulseHeaderView);

                bindAndSubscribe(this.timelineInteractor.timeline,
                                 this::bindTimeline,
                                 this::timelineUnavailable);

                bindAndSubscribe(this.preferences.observableUse24Time(),
                                 this.senseView::setUse24Time,
                                 Functions.LOG_ERROR);
            } else if (this.senseView.inNoDataState()) {
                update();
            }
        }
    }

    public void dismissVisibleOverlaysAndDialogs() {
        if (this.infoOverlay != null) {
            this.infoOverlay.dismiss(false);
        }

        final Dialog activeDialog = Functions.extract(this.activeDialog);
        if (activeDialog != null) {
            activeDialog.dismiss();
        }
    }

    //region Hooks

    public void setParent(@Nullable final Parent parent) {
        this.parent = parent;
    }

    @SuppressWarnings("ConstantConditions")
    public
    @NonNull
    LocalDate getDate() {
        return (LocalDate) getArguments().getSerializable(ARG_DATE);
    }

    public
    @Nullable
    Timeline getCachedTimeline() {
        return (Timeline) getArguments().getSerializable(ARG_CACHED_TIMELINE);
    }

    public void onSwipeBetweenDatesStarted() {
        dismissVisibleOverlaysAndDialogs();
    }

    public void scrollToTop() {
        this.senseView.scrollToTop();
    }

    public void update() {
        this.timelineInteractor.update();
    }

    //endregion


    //region Handholding
    public boolean isAtLeastThreeDaysOld() {
        return DateFormatter.isMoreThanThreeDays(preferences.getAccountCreationDate());
    }

    private void showHandholdingIfAppropriate() {
        if (this.parent == null ||
                WelcomeDialogFragment.isAnyVisible(getActivity())) {
            return;
        }

        if (Tutorial.SWIPE_TIMELINE.shouldShow(getActivity()) && !this.senseView.hasTutorial() && isAtLeastThreeDaysOld()) {
            final TutorialOverlayView overlayView = new TutorialOverlayView(getActivity(), Tutorial.SWIPE_TIMELINE);
            overlayView.setOnDismiss(() -> this.senseView.clearTutorial());
            overlayView.setAnchorContainer(getActivity().findViewById(this.parent.getTutorialContainerIdRes()));
            this.senseView.showTutorial(overlayView,
                                        this.parent.getTutorialContainerIdRes());
        }
    }

    private class HandholdingOneShotListener implements StaggeredFadeItemAnimator.Listener {
        @Override
        public void onItemAnimatorWillStart(@NonNull final AnimatorContext.Transaction transaction) {
        }

        @Override
        public void onItemAnimatorDidStop(final boolean finished) {
            if (finished) {
                showHandholdingIfAppropriate();
            }
            TimelineFragment.this.senseView.removeItemAnimatorListener(this);
        }
    }

    //endregion


    //region Binding

    public void bindTimeline(@NonNull final Timeline timeline) {
        final boolean hasEvents = !Lists.isEmpty(timeline.getEvents());
        if (!DateFormatter.isLastNight(timeline.getDate())) {
            Tutorial.SWIPE_TIMELINE.markShown(getActivity());
        }
        if (hasEvents) {
            this.senseView.transitionOutOfNoDataState();
            final Runnable backgroundAnimations = this.stateSafeExecutor.bind(() -> {
                final int targetColor = ContextCompat.getColor(getActivity(), R.color.timeline_background_fill);
                this.senseView.startBackgroundFade(targetColor);
            });
            final Runnable adapterAnimations = this.stateSafeExecutor.bind(() -> {
                this.senseView.addItemAnimatorListener(new HandholdingOneShotListener());
                this.senseView.bindEventsToTimeline(timeline.getEvents());
            });
            this.senseView.bindTimelineToHeader(timeline, backgroundAnimations, adapterAnimations);

            this.localUsageTracker.incrementAsync(LocalUsageTracker.Identifier.TIMELINE_SHOWN_WITH_DATA);
        } else {
            this.senseView.transitionIntoNoDataState((header -> {
                // Indicates on-boarding just ended
                final LocalDate creationDate =
                        this.preferences.getLocalDate(PreferencesInteractor.ACCOUNT_CREATION_DATE);
                final boolean isAccountNew = (creationDate == null ||
                        creationDate.equals(LocalDate.now()));
                if (isAccountNew) {
                    header.setDiagramResource(R.drawable.timeline_state_first_night);
                    header.setTitle(R.string.title_timeline_first_night);
                    header.setMessage(R.string.message_timeline_first_night);
                    header.setAction(R.string.action_timeline_bad_data_support, null);
                } else if (timeline.getScoreCondition() == ScoreCondition.INCOMPLETE) {
                    header.setDiagramResource(R.drawable.timeline_state_not_enough_data);
                    header.setTitle(R.string.title_timeline_not_enough_data);
                    header.setMessage(getString(R.string.title_timeline_not_enough_data_message));
                    header.setAction(R.string.action_timeline_bad_data_support, ignored -> UserSupport.showForDeviceIssue(getActivity(), UserSupport.DeviceIssue.TIMELINE_NOT_ENOUGH_SLEEP_DATA));
                } else {
                    header.setDiagramResource(R.drawable.timeline_state_no_data);
                    header.setTitle(R.string.title_timeline_no_data);
                    header.setMessage(getString(R.string.title_timeline_no_data_message));
                    header.setAction(R.string.action_timeline_bad_data_support, ignored -> UserSupport.showForDeviceIssue(getActivity(), UserSupport.DeviceIssue.TIMELINE_NO_SLEEP_DATA));
                }
            }));
        }
        this.senseView.setHeaderScoreEnabled(!Lists.isEmpty(timeline.getMetrics()) && hasEvents);
    }

    public void timelineUnavailable(final Throwable e) {
        Analytics.trackError(e, "Loading Timeline");
        final CharSequence message = getString(R.string.timeline_error_message);
        if (this.senseView.adapterHasEvents()) {
            final Toast toast = new Toast(getActivity().getApplicationContext());
            @SuppressLint("InflateParams")
            final TextView text = (TextView) getActivity().getLayoutInflater()
                                                          .inflate(R.layout.toast_text, null);
            text.setText(message);
            toast.setView(text);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();
        } else {
            this.senseView.transitionIntoNoDataState(header -> {
                header.setDiagramResource(R.drawable.timeline_state_error);
                header.setTitle(R.string.timeline_error_title);
                header.setMessage(message);
            });
        }
    }

    //endregion


    //region Acting on Items


    @Override
    public void onSegmentItemClicked(final int position, final View view, @NonNull final TimelineEvent event) {
        boolean animateShow = true;
        if (this.infoOverlay != null) {
            animateShow = false;
            this.infoOverlay.dismiss(false);
        }
        if (view.getY() < this.toolTipHeight) {
            final int dy = this.toolTipHeight * 2 - (int) view.getY();
            this.senseView.scrollForSpace(this, event, view, position, -dy);
            return;
        }
        this.infoOverlay = new TimelineInfoOverlay(getActivity(), getAnimatorContext());
        this.infoOverlay.setOnDismiss(sender -> {
            if (sender == this.infoOverlay) {
                this.infoOverlay = null;
            }
        });
        this.infoOverlay.bindEvent(event);
        this.infoOverlay.show(view,
                              this.senseView,
                              Views.getActivityScreenSize(getActivity(), false),
                              animateShow);

        Analytics.trackEvent(Analytics.Timeline.EVENT_TAP, null);
        Analytics.trackEvent(Analytics.Timeline.EVENT_LONG_PRESS_EVENT, null);
    }

    @Override
    public void onEventItemClicked(final int eventPosition, @NonNull final TimelineEvent event) {
        if (this.infoOverlay != null) {
            this.infoOverlay.dismiss(true);
        }

        if (event.hasActions()) {
            showAvailableActions(event);
        } else {
            showNoActionsAvailable();
        }

        Analytics.trackEvent(Analytics.Timeline.EVENT_TIMELINE_EVENT_TAPPED, null);
    }

    @VisibleForTesting
    void showNoActionsAvailable() {
        final SenseBottomSheet noActions = new SenseBottomSheet(getActivity());
        noActions.setTitle(R.string.message_timeline_no_actions_title);
        noActions.setMessage(R.string.message_timeline_no_actions_body);
        noActions.setWantsBigTitle(true);
        noActions.show();

        this.activeDialog = new WeakReference<>(noActions);
    }

    @VisibleForTesting
    void showAvailableActions(@NonNull final TimelineEvent event) {
        final SharedPreferences preferences = getActivity().getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);

        final SenseBottomSheet actions = new SenseBottomSheet(getActivity());
        if (!preferences.getBoolean(Constants.HANDHOLDING_HAS_SHOWN_TIMELINE_ADJUST_INTRO, false)) {
            actions.setTitle(R.string.timeline_actions_intro_title);
            actions.setMessage(R.string.timeline_actions_intro_message);
            actions.setWantsBigTitle(true);
        }
        actions.setWantsDividers(true);

        if (event.supportsAction(TimelineEvent.Action.VERIFY)) {
            actions.addOption(new SenseBottomSheet.Option(ID_EVENT_CORRECT)
                                      .setTitle(R.string.action_timeline_mark_event_correct)
                                      .setIcon(R.drawable.timeline_action_correct));
        }
        if (event.supportsAction(TimelineEvent.Action.ADJUST_TIME)) {
            actions.addOption(new SenseBottomSheet.Option(ID_EVENT_ADJUST_TIME)
                                      .setTitle(R.string.action_timeline_event_adjust_time)
                                      .setIcon(R.drawable.timeline_action_adjust));
        }
        if (event.supportsAction(TimelineEvent.Action.REMOVE)) {
            actions.addOption(new SenseBottomSheet.Option(ID_EVENT_REMOVE)
                                      .setTitle(R.string.action_timeline_event_remove)
                                      .setIcon(R.drawable.timeline_action_remove));
        }
        if (event.supportsAction(TimelineEvent.Action.INCORRECT)) {
            actions.addOption(new SenseBottomSheet.Option(ID_EVENT_INCORRECT)
                                      .setTitle(R.string.action_timeline_event_incorrect)
                                      .setIcon(R.drawable.timeline_action_remove));
        }

        actions.setOnOptionSelectedListener((option) -> {
            preferences.edit()
                       .putBoolean(Constants.HANDHOLDING_HAS_SHOWN_TIMELINE_ADJUST_INTRO, true)
                       .apply();

            final Properties properties = Analytics.createProperties(Analytics.Timeline.PROP_TYPE,
                                                                     event.getType().toString());
            switch (option.getOptionId()) {
                case ID_EVENT_CORRECT: {
                    doEventAction(actions, timelineInteractor.verifyEvent(event));
                    Analytics.trackEvent(Analytics.Timeline.EVENT_CORRECT, properties);
                    return false;
                }
                case ID_EVENT_ADJUST_TIME: {
                    adjustTime(event);
                    Analytics.trackEvent(Analytics.Timeline.EVENT_ADJUST_TIME, properties);
                    return true;
                }
                case ID_EVENT_INCORRECT: {
                    doEventAction(actions, timelineInteractor.deleteEvent(event));
                    Analytics.trackEvent(Analytics.Timeline.EVENT_INCORRECT, properties);
                    return false;
                }
                case ID_EVENT_REMOVE: {
                    doEventAction(actions, timelineInteractor.deleteEvent(event));
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

    private void adjustTime(@NonNull final TimelineEvent event) {
        final DateTime initialTime = event.getShiftedTimestamp();
        final RotaryTimePickerDialog.OnTimeSetListener listener = (hourOfDay, minuteOfHour) -> {
            final LocalTime newTime = new LocalTime(hourOfDay, minuteOfHour, 0);
            this.stateSafeExecutor.execute(() -> completeAdjustTime(event, newTime));
        };
        final RotaryTimePickerDialog timePicker = new RotaryTimePickerDialog(
                getActivity(),
                listener,
                initialTime.getHourOfDay(),
                initialTime.getMinuteOfHour(),
                this.preferences.getUse24Time()
        );
        timePicker.show();

        this.activeDialog = new WeakReference<>(timePicker);
    }

    private void completeAdjustTime(@NonNull final TimelineEvent event, @NonNull final LocalTime newTime) {
        final LoadingDialogFragment dialogFragment = LoadingDialogFragment.show(getFragmentManager(),
                                                                                getString(R.string.dialog_loading_message),
                                                                                LoadingDialogFragment.OPAQUE_BACKGROUND);
        dialogFragment.setDismissMessage(R.string.title_thank_you);
        bindAndSubscribe(this.timelineInteractor.amendEventTime(event, newTime),
                         ignored -> {
                             this.lastNightInteractor.update();
                             LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), null);
                         },
                         e -> {
                             LoadingDialogFragment.close(getFragmentManager());
                             ErrorDialogFragment.presentError(getActivity(), e);
                         });
    }

    private void doEventAction(@NonNull final SenseBottomSheet bottomSheet, @NonNull final Observable<Void> action) {
        if (getActivity() != null) {
            Dialogs.disableOrientationChangesUntilDismissed(bottomSheet, getActivity());
        }

        final LoadingView loadingView = new LoadingView(getActivity());
        bottomSheet.replaceContent(loadingView, null);
        bottomSheet.setCancelable(false);
        bindAndSubscribe(action,
                         ignored -> loadingView.playDoneTransition(R.string.title_thank_you, bottomSheet::dismiss),
                         e -> {
                             bottomSheet.dismiss();
                             ErrorDialogFragment.presentError(getActivity(), e);
                         });
    }

    //endregion

    private class ScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(final RecyclerView recyclerView, final int newState) {
            if (newState != RecyclerView.SCROLL_STATE_IDLE && TimelineFragment.this.infoOverlay != null) {
                TimelineFragment.this.infoOverlay.dismiss(false);
            }
        }

        @Override
        public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy) {
            if (!TimelineFragment.this.senseView.isAnimating()) {
                final int recyclerHeight = recyclerView.getMeasuredHeight(),
                        recyclerCenter = recyclerHeight / 2;
                for (int i = recyclerView.getChildCount() - 1; i >= 0; i--) {
                    final View view = recyclerView.getChildAt(i);
                    final RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
                    if (viewHolder instanceof TimelineAdapter.EventViewHolder) {
                        final TimelineAdapter.EventViewHolder eventViewHolder = (TimelineAdapter.EventViewHolder) viewHolder;
                        final int viewTop = view.getTop(),
                                viewBottom = view.getBottom(),
                                viewHeight = viewBottom - viewTop,
                                viewCenter = (viewTop + viewBottom) / 2;

                        final float centerDistanceAmount = (viewCenter - recyclerCenter) / (float) recyclerCenter;
                        final float bottomDistanceAmount;
                        if (viewBottom < recyclerHeight) {
                            bottomDistanceAmount = 1f;
                        } else {
                            final float offScreen = viewBottom - recyclerHeight;
                            bottomDistanceAmount = 1f - (offScreen / viewHeight);
                        }
                        eventViewHolder.setDistanceAmounts(bottomDistanceAmount, centerDistanceAmount);
                    }
                }
            }
        }
    }


    public interface Parent {

        @IdRes
        int getTutorialContainerIdRes();

        /**
         * Used to return to Last Night timeline when needed
         */
        void jumpToLastNight();

        void jumpTo(@NonNull final LocalDate date, @Nullable final Timeline timeline);

    }

    public interface ParentProvider {

        Parent getTimelineParent();
    }

}
