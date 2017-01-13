package is.hello.sense.flows.home.ui.views;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.flows.home.ui.fragments.TimelineFragment;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.TimelineAdapter;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayView;
import is.hello.sense.ui.recycler.ExtendedItemAnimator;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.recycler.StaggeredFadeItemAnimator;
import is.hello.sense.ui.widget.graphing.ColorDrawableCompat;
import is.hello.sense.ui.widget.timeline.TimelineHeaderView;
import is.hello.sense.ui.widget.timeline.TimelineNoDataHeaderView;
import rx.functions.Action1;

@SuppressLint("ViewConstructor")
public class TimelineView extends PresenterView {


    private final RecyclerView recyclerView;
    private final TimelineHeaderView headerView;
    private final TimelineAdapter adapter;
    private final StaggeredFadeItemAnimator itemAnimator;
    private final ColorDrawableCompat backgroundFill;
    private final RecyclerView.OnScrollListener scrollListener;
    @Nullable
    private TutorialOverlayView tutorialOverlay;

    public TimelineView(@NonNull final Activity activity,
                        @NonNull final AnimatorContext animatorContext,
                        @NonNull final TimelineAdapter timelineAdapter,
                        @NonNull final RecyclerView.OnScrollListener scrollListener,
                        @NonNull final OnClickListener onScoreClickListener) {
        super(activity);
        final Resources resources = activity.getResources();
        this.recyclerView = (RecyclerView) findViewById(R.id.fragment_timeline_recycler);
        this.headerView = new TimelineHeaderView(activity);
        this.adapter = timelineAdapter;
        this.scrollListener = scrollListener;

        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.addOnScrollListener(this.scrollListener);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        this.recyclerView.setLayoutManager(layoutManager);
        this.recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources, FadingEdgesItemDecoration.Style.STRAIGHT));


        this.itemAnimator = new StaggeredFadeItemAnimator(animatorContext);
        this.recyclerView.setItemAnimator(this.itemAnimator);
        this.recyclerView.addItemDecoration(new BottomInsetDecoration(resources, 2));

        this.headerView.setAnimatorContext(animatorContext);
        this.headerView.setAnimationEnabled(true);
        this.headerView.setOnScoreClickListener(onScoreClickListener);


        final int backgroundFillColor = ContextCompat.getColor(activity, R.color.background_timeline);
        this.backgroundFill = new ColorDrawableCompat(backgroundFillColor);
        this.recyclerView.setBackground(this.backgroundFill);
        this.adapter.addHeader(this.headerView);
        this.recyclerView.setAdapter(this.adapter);
    }

    //region PresenterView

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_timeline;
    }

    @Override
    public void releaseViews() {

        this.headerView.clearAnimation();

        this.itemAnimator.endAnimations();
        this.itemAnimator.removeAllListeners();

        this.recyclerView.clearOnScrollListeners();
        this.recyclerView.setAdapter(null);
        this.adapter.release();

        if (this.tutorialOverlay != null) {
            this.tutorialOverlay.dismiss(false);
            this.tutorialOverlay = null;
        }
    }
    //endregion

    //region methods
    public boolean inNoDataState() {
        return this.adapter.getHeaderAt(TimelineAdapter.CONTENT_START_POSITION) != this.headerView;
    }

    public void showTutorial(@NonNull final Activity activity,
                             @NonNull final TimelineFragment.Parent parent,
                             @NonNull final Tutorial tutorial) {
        if (this.tutorialOverlay != null) {
            return;
        }

        this.tutorialOverlay = new TutorialOverlayView(activity, tutorial);
        this.tutorialOverlay.setOnDismiss(() -> this.tutorialOverlay = null);
        this.tutorialOverlay.setAnchorContainer(activity.findViewById(parent.getTutorialContainerIdRes()));
        this.tutorialOverlay.show(parent.getTutorialContainerIdRes());
    }


    public void setAnimationEnabled(final boolean enabled) {
        if (enabled) {
            this.itemAnimator.setEnabled(ExtendedItemAnimator.Action.ADD, true);
        } else {
            this.itemAnimator.setEnabled(ExtendedItemAnimator.Action.ADD, false);
            this.itemAnimator.endAnimations();

        }
    }

    public void clearHeader() {
        this.headerView.clearAnimation();
    }

    public void pulseHeaderView() {
        this.headerView.startPulsing();
    }

    public void setUse24Time(final boolean use24Time) {
        this.adapter.setUse24Time(use24Time);
    }

    public void destroySoundPlayer() {
        if (this.adapter.isSoundPlayerDisposable()) {
            this.adapter.destroySoundPlayer();
        }
    }

    public void stopSoundPlayer() {
        this.adapter.stopSoundPlayer();
    }

    public void bindTimelineToHeader(@NonNull final Timeline timeline,
                                     @NonNull final Runnable backgroundAnimations,
                                     @NonNull final Runnable adapterAnimations) {
        this.headerView.bindTimeline(timeline, backgroundAnimations, adapterAnimations);
    }

    public void bindEventsToTimeline(@NonNull final ArrayList<TimelineEvent> events) {
        this.adapter.bindEvents(events);
    }

    public void setHeaderScoreEnabled(final boolean enabled) {
        this.headerView.setScoreClickEnabled(enabled);
        this.adapter.showShareIcon(enabled);
    }

    public void renderTimeline(@NonNull final Timeline timeline) {
        this.backgroundFill.setColor(ContextCompat.getColor(getContext(), R.color.timeline_background_fill));
        this.headerView.bindTimeline(timeline);
        bindEventsToTimeline(timeline.getEvents());
    }

    public void scrollToTop() {
        this.recyclerView.smoothScrollToPosition(0);
    }

    public void addItemAnimatorListener(@NonNull final ExtendedItemAnimator.Listener listener) {
        this.itemAnimator.addListener(listener);
    }

    public void removeItemAnimatorListener(@NonNull final ExtendedItemAnimator.Listener listener) {
        this.itemAnimator.removeListener(listener);
    }

    public void startBackgroundFade(final int targetColor) {
        this.backgroundFill.colorAnimator(targetColor).start();
    }

    public boolean adapterHasEvents() {
        return this.adapter.hasEvents();
    }

    public void scrollForSpace(@NonNull final TimelineAdapter.OnItemClickListener itemClickListener,
                               @NonNull final TimelineEvent event,
                               final View view,
                               final int position,
                               final int dy) {
        this.recyclerView.removeOnScrollListener(this.scrollListener);
        this.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(final RecyclerView recyclerView, final int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    recyclerView.removeOnScrollListener(this);
                    itemClickListener.onSegmentItemClicked(position, view, event);
                    recyclerView.addOnScrollListener(TimelineView.this.scrollListener);
                }
            }
        });
        this.recyclerView.smoothScrollBy(0, dy);

    }

    public void transitionIntoNoDataState(@NonNull final Action1<TimelineNoDataHeaderView> configurer) {
        final TimelineNoDataHeaderView newHeader = new TimelineNoDataHeaderView(getContext());
        configurer.call(newHeader);
        if (ViewCompat.isLaidOut(this.recyclerView)) {
            this.itemAnimator.setDelayEnabled(false);
            this.itemAnimator.setEnabled(ExtendedItemAnimator.Action.REMOVE, true);
            this.itemAnimator.setTemplate(this.itemAnimator.getTemplate()
                                                           .withDuration(Anime.DURATION_SLOW));
            this.itemAnimator.addListener(createListener());
        } else {
            this.backgroundFill.setColor(ContextCompat.getColor(getContext(), R.color.background_timeline));
        }
        this.headerView.stopPulsing();
        this.adapter.replaceHeader(TimelineAdapter.CONTENT_START_POSITION, newHeader);
    }

    public void transitionOutOfNoDataState() {
        if (!inNoDataState()) {
            return;
        }
        this.itemAnimator.setEnabled(ExtendedItemAnimator.Action.ADD, false);
        this.itemAnimator.setEnabled(ExtendedItemAnimator.Action.REMOVE, false);
        this.itemAnimator.setTemplate(this.itemAnimator.getTemplate()
                                                       .withDuration(Anime.DURATION_NORMAL));


        this.adapter.replaceHeader(TimelineAdapter.CONTENT_START_POSITION, this.headerView);

        this.headerView.setBackgroundSolid(false, 0);
        this.headerView.startPulsing();

        // Run after change listener
        this.recyclerView.post(() -> {
            this.itemAnimator.setDelayEnabled(true);
            this.itemAnimator.setEnabled(ExtendedItemAnimator.Action.ADD, true);
        });
    }

    public boolean isAnimating() {
        return itemAnimator.isRunning();
    }

    private ExtendedItemAnimator.Listener createListener() {
        return new ExtendedItemAnimator.Listener() {
            final Animator crossFade = TimelineView.this.backgroundFill.colorAnimator(ContextCompat.getColor(getContext(), R.color.background_timeline));

            @Override
            public void onItemAnimatorWillStart(@NonNull final AnimatorContext.Transaction transaction) {
                transaction.takeOwnership(crossFade, "TimelineFragment#backgroundFill#crossFade");
            }

            @Override
            public void onItemAnimatorDidStop(final boolean finished) {
                if (!finished) {
                    crossFade.cancel();
                }
                TimelineView.this.itemAnimator.removeListener(this);
            }

        };
    }
    //endregion

    static class BottomInsetDecoration extends RecyclerView.ItemDecoration {
        private final int bottomPadding;
        private final int headerCount;

        BottomInsetDecoration(@NonNull final Resources resources, final int headerCount) {
            this.bottomPadding = resources.getDimensionPixelSize(R.dimen.timeline_gap_bottom);
            this.headerCount = headerCount;
        }

        @Override
        public void getItemOffsets(final Rect outRect, final View view, final RecyclerView parent, final RecyclerView.State state) {
            final int position = parent.getChildAdapterPosition(view);
            if (position >= this.headerCount && position == parent.getAdapter().getItemCount() - 1) {
                outRect.bottom = this.bottomPadding;
            }
        }
    }


}
