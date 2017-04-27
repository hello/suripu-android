package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.Locale;

import is.hello.go99.Anime;
import is.hello.go99.animators.MultiAnimator;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.interactors.ZoomedOutTimelineInteractor;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.TimelinePreviewView;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import rx.functions.Action1;

public class ZoomedOutTimelineAdapter extends RecyclerView.Adapter<ZoomedOutTimelineAdapter.ViewHolder> {
    private final ZoomedOutTimelineInteractor interactor;
    private final int count;

    @Nullable
    private OnItemClickedListener onItemClickedListener;

    public ZoomedOutTimelineAdapter(@NonNull final ZoomedOutTimelineInteractor interactor,
                                    @NonNull final LocalDate oldestDate) {
        this.interactor = interactor;

        LocalDate today = DateFormatter.todayForTimeline();
        if (today.equals(oldestDate)) {
            today = today.plusDays(1);
        }
        this.count = Days.daysBetween(oldestDate, today).getDays();
    }


    //region Population

    @Override
    public int getItemCount() {
        return this.count;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup viewGroup,
                                         final int position) {
        final View itemView = LayoutInflater.from(viewGroup.getContext())
                                            .inflate(R.layout.item_zoomed_out_timeline, viewGroup, false);
        return new ViewHolder(itemView,
                              ZoomedOutTimelineAdapter.this::dispatchClick);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder,
                                 final int position) {
        final LocalDate date = this.interactor.getDateAt(position);
        final Timeline timeline = this.interactor.getCachedTimeline(date);
        holder.bind(date, timeline);

        this.interactor.addDataView(holder);
    }

    @Override
    public void onViewDetachedFromWindow(final ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        this.interactor.removeDataView(holder);
    }

    @Override
    public void onViewRecycled(final ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.unbind();
        this.interactor.removeDataView(holder);
    }

    @Override
    public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        this.interactor.clearDataViews();
    }

    //endregion


    //region Click Listener

    public void setOnItemClickedListener(@Nullable final OnItemClickedListener onItemClickedListener) {
        this.onItemClickedListener = onItemClickedListener;
    }

    private void dispatchClick(@NonNull final ViewHolder viewHolder) {
        // View dispatches OnClickListener#onClick(View) calls on
        // the next looper cycle. It's possible for the adapter's
        // containing recycler view to update and invalidate a
        // view holder before the callback fires.
        final int adapterPosition = viewHolder.getAdapterPosition();
        if (onItemClickedListener != null && adapterPosition != RecyclerView.NO_POSITION) {
            onItemClickedListener.onItemClicked(viewHolder.itemView, viewHolder.getAdapterPosition());
        }
    }

    public interface OnItemClickedListener {
        void onItemClicked(@NonNull View itemView, int position);
    }

    //endregion


    public static class ViewHolder extends RecyclerView.ViewHolder
            implements ZoomedOutTimelineInteractor.DataView {
        private final TextView dayNumber;
        private final TextView dayName;
        public final TextView score;
        private final TimelinePreviewView preview;
        private final SleepScoreDrawable scoreDrawable;
        public final ProgressBar progressBar;
        private final View scoreContainer;
        private boolean hasTimeline = false;
        private boolean showScore = false;
        private LocalDate date;


        //region Lifecycle

        private ViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.dayNumber = (TextView) itemView.findViewById(R.id.item_zoomed_out_timeline_day_number);
            this.dayName = (TextView) itemView.findViewById(R.id.item_zoomed_out_timeline_day_name);
            this.score = (TextView) itemView.findViewById(R.id.item_zoomed_out_timeline_score);
            this.preview = (TimelinePreviewView) itemView.findViewById(R.id.item_zoomed_out_timeline_preview);
            this.progressBar = (ProgressBar) itemView.findViewById(R.id.item_zoomed_out_timeline_progress);

            this.scoreDrawable = new SleepScoreDrawable(itemView.getResources(), false);

            scoreContainer = itemView.findViewById(R.id.item_zoomed_out_timeline_score_container);
            scoreContainer.setBackground(scoreDrawable);
        }

        protected ViewHolder(@NonNull final View itemView,
                             @NonNull final Action1<ViewHolder> dispatchClick) {
            this(itemView);
            itemView.setOnClickListener(ignored -> dispatchClick.call(this));
        }

        private boolean isTimelineEmpty(@NonNull final Timeline timeline) { //todo use TimelineInteractor static isValid methods
            return (timeline.getScore() == null ||
                    timeline.getScoreCondition() == ScoreCondition.UNAVAILABLE ||
                    timeline.getScoreCondition() == ScoreCondition.INCOMPLETE);
        }

        private void bind(@NonNull final LocalDate date,
                          @Nullable final Timeline timeline) {
            this.date = date;
            dayNumber.setText(date.toString("d"));
            dayName.setText(date.toString("EE"));
            if (timeline == null) {
                cancelAnimation(true);
                this.hasTimeline = false;
            } else {
                showLoading(false);
                showScore = true;
                if (isTimelineEmpty(timeline)) {
                    final int sleepScoreColor = ContextCompat.getColor(itemView.getContext(),
                                                                       ScoreCondition.UNAVAILABLE.colorRes);
                    score.setText(null);
                    score.setTextColor(sleepScoreColor);
                    scoreDrawable.setFillColor(sleepScoreColor);
                    scoreDrawable.setValue(0);
                    showScore = false;
                    preview.setTimelineEvents(null);

                } else {
                    final Integer sleepScore = timeline.getScore();
                    final int sleepScoreColor = ContextCompat.getColor(itemView.getContext(),
                                                                       timeline.getScoreCondition().colorRes);
                    score.setText(String.format(Locale.getDefault(), "%d", sleepScore));
                    score.setTextColor(sleepScoreColor);
                    scoreDrawable.setFillColor(sleepScoreColor);
                    if (sleepScore != null) {
                        scoreDrawable.setValue(sleepScore);
                    }

                    preview.setTimelineEvents(timeline.getEvents());

                }
                this.hasTimeline = true;
                if (progressBar.getVisibility() == View.VISIBLE) {
                    scoreContainer.setVisibility(View.INVISIBLE);
                    preview.setVisibility(View.INVISIBLE);
                    MultiAnimator.animatorFor(progressBar)
                                 .fadeOut(View.GONE)
                                 .setDuration(Anime.DURATION_NORMAL)
                                 .addOnAnimationCompleted(finished -> {
                                     if (!finished) {
                                         return;
                                     }

                                     if (showScore) {
                                         MultiAnimator.animatorFor(preview)
                                                      .fadeIn()
                                                      .setDuration(1250)
                                                      .start();

                                     }

                                     MultiAnimator.animatorFor(scoreContainer)
                                                  .fadeIn()
                                                  .setDuration(1250)
                                                  .start();
                                 })
                                 .start();
                }
            }
        }

        private void unbind() {
            cancelAnimation(false);
            this.hasTimeline = false;
            this.date = null;
        }

        //endregion


        //region Hooks

        @Override
        public LocalDate getDate() {
            return date;
        }

        @Override
        public boolean wantsUpdates() {
            return (getAdapterPosition() != RecyclerView.NO_POSITION && !hasTimeline);
        }

        @Override
        public void onUpdateAvailable(@NonNull final Timeline timeline) {
            if (wantsUpdates()) {
                bind(date, timeline);
            }
        }

        @Override
        public void onUpdateFailed(final Throwable e) {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                Logger.error(getClass().getSimpleName(), "Could not load item " + getAdapterPosition(), e);
                bind(date, null);
            }
        }

        @Override
        public void cancelAnimation(final boolean showLoading) {
            Anime.cancelAll(progressBar, scoreContainer, preview);
            showLoading(showLoading);
        }

        //endregion

        //region view support

        private void showLoading(final boolean show) {
            if (show) {
                scoreContainer.setVisibility(View.GONE);
                preview.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setAlpha(1f);
            } else {
                progressBar.setVisibility(View.GONE);
                scoreContainer.setVisibility(View.VISIBLE);
                preview.setVisibility(View.VISIBLE);
            }
        }

        //endregion
    }
}
