package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.LocalDate;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.ZoomedOutTimelineAdapter;
import is.hello.sense.ui.common.ZoomedOutTimelineLayoutManager;
import is.hello.sense.ui.widget.timeline.ZoomedOutTimelineDecoration;

@SuppressLint("ViewConstructor")
public class ZoomedOutTimelineView extends PresenterView
        implements ZoomedOutTimelineAdapter.OnItemClickedListener {
    private final TextView monthTextView;
    private final RecyclerView recyclerView;
    private final Button todayButton;
    private final ZoomedOutTimelineLayoutManager layoutManager;
    private Listener listener = null;

    public ZoomedOutTimelineView(@NonNull final Activity activity,
                                 @NonNull final ZoomedOutTimelineAdapter zoomedOutTimelineAdapter) {
        super(activity);
        this.monthTextView = (TextView) findViewById(R.id.fragment_zoomed_out_timeline_month);
        this.recyclerView = (RecyclerView) findViewById(R.id.fragment_zoomed_out_timeline_recycler_view);
        this.todayButton = (Button) findViewById(R.id.fragment_zoomed_out_timeline_today);
        this.layoutManager = new ZoomedOutTimelineLayoutManager(activity);

        this.recyclerView.addItemDecoration(new ZoomedOutTimelineDecoration(getResources()));
        this.recyclerView.addOnScrollListener(new SnappingScrollListener());
        this.recyclerView.setLayoutManager(this.layoutManager);
        zoomedOutTimelineAdapter.setOnItemClickedListener(this);
        this.recyclerView.setAdapter(zoomedOutTimelineAdapter);

        this.todayButton.setOnClickListener(this::jumpToToday);
        // The second item from the right at the beginning of the
        // navigator is a special case. Because the item is already
        // visible without scrolling, the layout manager will try to
        // take a shortcut and not scroll at all. So we have to give
        // a specific offset if we want it to be centered. Of course,
        // we can only get the offset after layout.
        this.layoutManager.postLayout(() -> this.recyclerView.post(() -> {
            jumpToToday(null);
            retrieveTimelines();
        }));
    }

    //region PresenterView
    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_zoomed_out_timeline;
    }

    @Override
    public void releaseViews() {
        ((ZoomedOutTimelineAdapter) this.recyclerView.getAdapter()).setOnItemClickedListener(null);
        this.recyclerView.setAdapter(null);
        this.recyclerView.clearOnScrollListeners();
        this.todayButton.setOnClickListener(null);
        this.layoutManager.postLayout(null);
        this.listener = null;
    }
    //endregion

    //region ZoomedOutTimelineAdapter.OnItemClickedListener
    @Override
    public void onItemClicked(@NonNull final View itemView,
                              final int position) {
        // Tried implementing this using the first/last visible items
        // from the linear layout manager, the children of the recycler
        // view, and neither worked 100% of the time. This does.

        // Guard against rapid taps
        if (this.recyclerView == null || this.layoutManager == null) {
            return;
        }

        if (itemView.getAlpha() < 0.95f) { // Can't use `< 1f` because of uneven view metrics
            final int recyclerViewCenter = this.recyclerView.getMeasuredWidth() / 2;
            if (itemView.getRight() < recyclerViewCenter) {
                this.recyclerView.smoothScrollBy(-this.layoutManager.getItemWidth(), 0);
            } else {
                this.recyclerView.smoothScrollBy(this.layoutManager.getItemWidth(), 0);
            }
        } else {
            onTimelineClicked(position);
        }

    }
    //endregion

    //region methods
    public void setMonthText(@Nullable final String month) {
        this.monthTextView.setText(month);
    }

    public void setListener(@Nullable final Listener listener) {
        this.listener = listener;
    }

    private void jumpToToday(@Nullable final View ignored) {
        final LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
            @Override
            protected int calculateTimeForScrolling(final int dx) {
                return super.calculateTimeForScrolling(dx) / 4;
            }

            @Override
            protected int calculateTimeForDeceleration(final int dx) {
                return super.calculateTimeForDeceleration(dx) / 4;
            }
        };
        smoothScroller.setTargetPosition(0);
        if (this.layoutManager != null) {
            this.layoutManager.startSmoothScroll(smoothScroller);
        }
    }

    private String getFormattedDate(@NonNull final LocalDate localDate) {
        if (this.listener == null) {
            return null;
        }
        return this.listener.getFormattedDate(localDate);
    }

    private void retrieveTimelines() {
        if (this.listener == null) {
            return;
        }
        this.listener.retrieveTimelines();
    }

    private void onTimelineClicked(final int position) {
        if (this.listener == null) {
            return;
        }
        this.listener.onTimelineClicked(position);
    }
    // endregion

    private class SnappingScrollListener extends RecyclerView.OnScrollListener {
        int previousState = RecyclerView.SCROLL_STATE_IDLE;

        @Override
        public void onScrolled(final RecyclerView recyclerView,
                               final int dx,
                               final int dy) {
            final View centerChild = recyclerView.findChildViewUnder(recyclerView.getWidth() / 2, 0);
            final ZoomedOutTimelineAdapter.ViewHolder holder = (ZoomedOutTimelineAdapter.ViewHolder) recyclerView.getChildViewHolder(centerChild);
            if (ZoomedOutTimelineView.this.monthTextView != null) {
                ZoomedOutTimelineView.this.monthTextView.setText(getFormattedDate(holder.getDate()));
            }
        }

        @Override
        public void onScrollStateChanged(final RecyclerView recyclerView,
                                         final int newState) {
            if (this.previousState != RecyclerView.SCROLL_STATE_IDLE && newState == RecyclerView.SCROLL_STATE_IDLE) {
                snapToNearestItem(recyclerView);
                retrieveTimelines();
            }

            this.previousState = newState;
        }

        private void snapToNearestItem(@NonNull final RecyclerView recyclerView) {
            final int containerMidX = recyclerView.getWidth() / 2;
            final View centerView = recyclerView.findChildViewUnder(containerMidX, 0);
            final int centerViewMidX = (centerView.getLeft() + centerView.getRight()) / 2;
            final int distanceToNotch = centerViewMidX - containerMidX;
            if (distanceToNotch != 0) {
                recyclerView.smoothScrollBy(distanceToNotch, 0);
            }
        }
    }

    public interface Listener {
        String getFormattedDate(@NonNull LocalDate localDate);

        void retrieveTimelines();

        void onTimelineClicked(final int position);
    }
}
