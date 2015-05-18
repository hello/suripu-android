package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONObject;

import java.util.ArrayList;

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
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.BottomSheetDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.TimePickerDialogFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayView;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.ui.widget.SlidingLayersView;
import is.hello.sense.ui.widget.timeline.TimelineFadeItemAnimator;
import is.hello.sense.ui.widget.timeline.TimelineHeaderView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Share;
import rx.Observable;

public class TimelineFragment extends InjectionFragment implements SlidingLayersView.OnInteractionListener, TimelineAdapter.OnItemClickListener {
    private static final String ARG_DATE = TimelineFragment.class.getName() + ".ARG_DATE";
    private static final String ARG_CACHED_TIMELINE = TimelineFragment.class.getName() + ".ARG_CACHED_TIMELINE";
    private static final String ARG_IS_FIRST_TIMELINE = TimelineFragment.class.getName() + ".ARG_IS_FIRST_TIMELINE";

    private static final int REQUEST_CODE_ALTER_EVENT = 0xAE3;
    private static final int REQUEST_CODE_ADJUST_TIME = 0xA1E;

    private static final int ID_EVENT_CORRECT = 0;
    private static final int ID_EVENT_ADJUST_TIME = 1;
    private static final int ID_EVENT_REMOVE = 2;


    @Inject TimelinePresenter presenter;
    @Inject DateFormatter dateFormatter;
    @Inject PreferencesPresenter preferences;

    private HomeActivity homeActivity;

    private boolean firstTimeline;

    private ImageButton menuButton;
    private ImageButton shareButton;
    private TextView dateText;
    private View contentShadow;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private TimelineHeaderView headerView;
    private TimelineAdapter adapter;
    private TimelineFadeItemAnimator itemAnimator;

    private boolean controlsAlarmShortcut = false;

    private @Nullable TutorialOverlayView tutorialOverlay;


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

        presenter.setDateWithTimeline(date, getCachedTimeline());
        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeline, container, false);


        this.dateText = (TextView) view.findViewById(R.id.fragment_timeline_date);
        dateText.setText(dateFormatter.formatAsTimelineDate(presenter.getDate()));
        Views.setSafeOnClickListener(dateText, this::showNavigator);

        this.menuButton = (ImageButton) view.findViewById(R.id.fragment_timeline_header_menu);
        Views.setSafeOnClickListener(menuButton, this::showUnderside);

        this.shareButton = (ImageButton) view.findViewById(R.id.fragment_timeline_header_share);
        shareButton.setVisibility(View.INVISIBLE);
        Views.setSafeOnClickListener(shareButton, this::share);


        this.contentShadow = view.findViewById(R.id.fragment_timeline_content_shadow);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_timeline_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setOnScrollListener(new ScrollListener());

        this.layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        this.headerView = new TimelineHeaderView(getActivity());
        headerView.setAnimatorContext(getAnimatorContext());
        headerView.setFirstTimeline(firstTimeline);
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

        this.itemAnimator = new TimelineFadeItemAnimator(getAnimatorContext(), headerView);
        recyclerView.setItemAnimator(itemAnimator);
        recyclerView.addItemDecoration(new BackgroundDecoration(getResources()));

        this.adapter = new TimelineAdapter(getActivity(), headerView, dateFormatter);
        adapter.setOnItemClickListener(stateSafeExecutor, this);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.timeline,
                this::bindTimeline,
                this::timelineUnavailable);

        bindAndSubscribe(presenter.message,
                         headerView::bindMessage,
                         Functions.IGNORE_ERROR);

        bindAndSubscribe(preferences.observableUse24Time(),
                         adapter::setUse24Time,
                         Functions.LOG_ERROR);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.menuButton = null;
        this.shareButton = null;
        this.dateText = null;

        this.headerView = null;
        this.recyclerView = null;
        this.layoutManager = null;
        this.adapter = null;
        this.itemAnimator = null;

        if (tutorialOverlay != null) {
            tutorialOverlay.dismiss(false);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        this.homeActivity = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        dateText.setText(dateFormatter.formatAsTimelineDate(presenter.getDate()));
    }

    //endregion


    //region Actions

    public void showUnderside(@NonNull View sender) {
        homeActivity.getSlidingLayersView().toggle();
        scrollToTop();
    }

    public void share(@NonNull View sender) {
        Analytics.trackEvent(Analytics.Timeline.EVENT_SHARE, null);

        Observable<Timeline> currentTimeline = presenter.timeline.take(1);
        bindAndSubscribe(currentTimeline,
                timeline -> {
                    DateTime date = presenter.getDate();
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

    public void showNavigator(@NonNull View sender) {
        Timeline timeline = (Timeline) dateText.getTag();
        homeActivity.showTimelineNavigator(getDate(), timeline);
    }

    //endregion


    //region Hooks

    public @NonNull DateTime getDate() {
        return (DateTime) getArguments().getSerializable(ARG_DATE);
    }

    public @Nullable Timeline getCachedTimeline() {
        return (Timeline) getArguments().getSerializable(ARG_CACHED_TIMELINE);
    }

    public void setControlsAlarmShortcut(boolean controlsAlarmShortcut) {
        this.controlsAlarmShortcut = controlsAlarmShortcut;
    }

    public void scrollToTop() {
        recyclerView.smoothScrollToPosition(0);
    }

    public void update() {
        presenter.update();
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
        if (adapter.getItemCount() > 0) {
            shareButton.setVisibility(View.VISIBLE);
        }
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
        if (homeActivity.getWillShowUnderside()) {
            WelcomeDialogFragment.markShown(homeActivity, R.xml.welcome_dialog_timeline);
        } else {
            getAnimatorContext().runWhenIdle(stateSafeExecutor.bind(() -> {
                if (WelcomeDialogFragment.shouldShow(homeActivity, R.xml.welcome_dialog_timeline)) {
                    WelcomeDialogFragment.show(homeActivity, R.xml.welcome_dialog_timeline);
                } else if (Tutorial.SLEEP_SCORE_BREAKDOWN.shouldShow(getActivity())) {
                    showTutorial(Tutorial.SLEEP_SCORE_BREAKDOWN);
                } else if (Tutorial.SWIPE_TIMELINE.shouldShow(getActivity())) {
                    showTutorial(Tutorial.SWIPE_TIMELINE);
                }
            }));
        }
    }

    //endregion


    //region Binding

    public void bindTimeline(@NonNull Timeline timeline) {
        Runnable continuation = stateSafeExecutor.bind(() -> adapter.bindSegments(timeline.getSegments()));
        if (Lists.isEmpty(timeline.getSegments())) {
            headerView.bindScore(TimelineHeaderView.NULL_SCORE, continuation);
            shareButton.setVisibility(View.GONE);
        } else {
            headerView.bindScore(timeline.getScore(), continuation);
            if (!homeActivity.getSlidingLayersView().isOpen()) {
                shareButton.setVisibility(View.VISIBLE);
            }
        }
    }

    public void timelineUnavailable(Throwable e) {
        adapter.clear();
        headerView.bindError(e);
    }

    //endregion


    //region Acting on Items

    @Override
    public void onSegmentItemClicked(int position, @NonNull TimelineSegment segment) {

    }

    @Override
    public void onEventItemClicked(int position, @NonNull TimelineSegment segment) {
        ArrayList<SenseBottomSheet.Option> options = new ArrayList<>();
        options.add(
                new SenseBottomSheet.Option(ID_EVENT_CORRECT)
                        .setTitle(R.string.action_timeline_mark_event_correct)
                        .setIcon(R.drawable.timeline_action_correct)
                        .setEnabled(false)
        );
        if (segment.isTimeAdjustable()) {
            options.add(
                    new SenseBottomSheet.Option(ID_EVENT_ADJUST_TIME)
                            .setTitle(R.string.action_timeline_event_adjust_time)
                            .setIcon(R.drawable.timeline_action_adjust)
            );
        }
        options.add(
                new SenseBottomSheet.Option(ID_EVENT_REMOVE)
                        .setTitle(R.string.action_timeline_event_remove)
                        .setIcon(R.drawable.timeline_action_remove)
                        .setEnabled(false)
        );
        BottomSheetDialogFragment actions = BottomSheetDialogFragment.newInstance(options);
        actions.setWantsDividers(true);
        actions.setAffectedPosition(position);
        actions.setTargetFragment(this, REQUEST_CODE_ALTER_EVENT);
        actions.show(getFragmentManager(), BottomSheetDialogFragment.TAG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ALTER_EVENT && resultCode == Activity.RESULT_OK) {
            int optionId = data.getIntExtra(BottomSheetDialogFragment.RESULT_OPTION_ID, 0);
            int segmentPosition = data.getIntExtra(BottomSheetDialogFragment.RESULT_AFFECTED_POSITION, 0);
            switch (optionId) {
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
                    Logger.warn(getClass().getSimpleName(), "Unknown option #" + optionId);
                    break;
                }
            }
        } else if (requestCode == REQUEST_CODE_ADJUST_TIME && resultCode == Activity.RESULT_OK) {
            int segmentPosition = data.getIntExtra(TimePickerDialogFragment.RESULT_AFFECTED_POSITION, 0);
            int hour = data.getIntExtra(TimePickerDialogFragment.RESULT_HOUR, 11);
            int minute = data.getIntExtra(TimePickerDialogFragment.RESULT_MINUTE, 30);
            completeAdjustTime(segmentPosition, new LocalTime(hour, minute, 0));
        }
    }

    private void adjustTime(int position) {
        TimelineSegment segment = adapter.getSegment(position);
        LocalTime initialTime = segment.getShiftedTimestamp().toLocalTime();
        int flags = TimePickerDialogFragment.FLAG_USE_ROTARY_PICKER;
        if (preferences.getUse24Time()) {
            flags |= TimePickerDialogFragment.FLAG_USE_24_TIME;
        }
        TimePickerDialogFragment adjustTimeDialog = TimePickerDialogFragment.newInstance(initialTime, flags);
        adjustTimeDialog.setAffectedPosition(position);
        adjustTimeDialog.setTargetFragment(this, REQUEST_CODE_ADJUST_TIME);
        adjustTimeDialog.show(getFragmentManager(), TimePickerDialogFragment.TAG);
    }

    private void completeAdjustTime(int position, @NonNull LocalTime newTime) {
        TimelineSegment segment = adapter.getSegment(position);
        DateTime originalTime = segment.getShiftedTimestamp();

        Feedback feedback = new Feedback();
        feedback.setEventType(segment.getEventType());
        feedback.setNight(originalTime.toLocalDate());
        feedback.setOldTime(originalTime.toLocalTime());
        feedback.setNewTime(newTime);

        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(presenter.submitCorrection(feedback),
                         ignored -> {
                             LoadingDialogFragment.close(getFragmentManager());
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
                float bottom = headerView.getBottom();
                float height = headerView.getMeasuredHeight();
                float alpha = bottom / height;
                headerView.setChildFadeAmount(alpha);
            }

            if (!itemAnimator.isRunning()) {
                int recyclerBottom = recyclerView.getMeasuredHeight();
                for (int i = recyclerView.getChildCount() - 1; i >= 0; i--) {
                    View view = recyclerView.getChildAt(i);
                    RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
                    if (viewHolder instanceof TimelineAdapter.EventViewHolder) {
                        TimelineAdapter.EventViewHolder eventViewHolder = (TimelineAdapter.EventViewHolder) viewHolder;
                        int bottom = view.getBottom();
                        if (bottom < recyclerBottom) {
                            eventViewHolder.setOnScreenAmount(1f);
                        } else {
                            float offScreen = bottom - recyclerBottom;
                            float amount = 1f - (offScreen / view.getMeasuredHeight());
                            eventViewHolder.setOnScreenAmount(amount);
                        }
                    }
                }
            }

            if (controlsAlarmShortcut) {
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
            this.background = resources.getDrawable(R.drawable.background_timeline_segment);
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
