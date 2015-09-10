package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.graph.presenters.ZoomedOutTimelinePresenter;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.TimelinePreviewView;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class ZoomedOutTimelineAdapter extends RecyclerView.Adapter<ZoomedOutTimelineAdapter.ViewHolder> {
    private final Context context;
    private final LayoutInflater inflater;
    private final Resources resources;
    private final ZoomedOutTimelinePresenter presenter;
    private final int count;

    private @Nullable OnItemClickedListener onItemClickedListener;

    public ZoomedOutTimelineAdapter(@NonNull Context context, @NonNull ZoomedOutTimelinePresenter presenter) {
        this.context = context;
        this.resources = context.getResources();
        this.inflater = LayoutInflater.from(context);
        this.presenter = presenter;

        this.count = Days.daysBetween(Constants.TIMELINE_EPOCH, DateFormatter.todayForTimeline()).getDays();
    }


    //region Population

    @Override
    public int getItemCount() {
        return count;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View itemView = inflater.inflate(R.layout.item_zoomed_out_timeline, viewGroup, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LocalDate date = presenter.getDateAt(position);
        Timeline timeline = presenter.getCachedTimeline(date);
        holder.bind(date, timeline);

        presenter.addDataView(holder);
    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        presenter.removeDataView(holder);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);

        holder.unbind();
        presenter.removeDataView(holder);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        presenter.clearDataViews();
    }

    //endregion


    //region Click Listener

    public void setOnItemClickedListener(@Nullable OnItemClickedListener onItemClickedListener) {
        this.onItemClickedListener = onItemClickedListener;
    }

    private void dispatchClick(@NonNull ViewHolder viewHolder) {
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


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, ZoomedOutTimelinePresenter.DataView {
        public final TextView dayNumber;
        public final TextView dayName;
        public final TextView score;
        public final TimelinePreviewView preview;
        public final SleepScoreDrawable scoreDrawable;

        private boolean hasTimeline = false;
        private LocalDate date;


        //region Lifecycle

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.dayNumber = (TextView) itemView.findViewById(R.id.item_zoomed_out_timeline_day_number);
            this.dayName = (TextView) itemView.findViewById(R.id.item_zoomed_out_timeline_day_name);
            this.score = (TextView) itemView.findViewById(R.id.item_zoomed_out_timeline_score);
            this.preview = (TimelinePreviewView) itemView.findViewById(R.id.item_zoomed_out_timeline_preview);

            this.scoreDrawable = new SleepScoreDrawable(context.getResources(), false);

            View scoreContainer = itemView.findViewById(R.id.item_zoomed_out_timeline_score_container);
            scoreContainer.setBackground(scoreDrawable);

            itemView.setOnClickListener(this);
        }

        private void bind(@NonNull LocalDate date, @Nullable Timeline timeline) {
            this.date = date;
            dayNumber.setText(date.toString("d"));
            dayName.setText(date.toString("EE"));

            if (timeline == null || timeline.getScore() == null ||
                    timeline.getScoreCondition() == ScoreCondition.UNAVAILABLE) {
                int sleepScoreColor = resources.getColor(ScoreCondition.UNAVAILABLE.colorRes);
                score.setText(R.string.missing_data_placeholder);
                score.setTextColor(sleepScoreColor);
                scoreDrawable.setFillColor(sleepScoreColor);
                scoreDrawable.setValue(0);

                preview.setTimelineEvents(null);

                this.hasTimeline = false;
            } else {
                int sleepScore = timeline.getScore();
                int sleepScoreColor = resources.getColor(timeline.getScoreCondition().colorRes);
                score.setText(Integer.toString(sleepScore));
                score.setTextColor(sleepScoreColor);
                scoreDrawable.setFillColor(sleepScoreColor);
                scoreDrawable.setValue(sleepScore);

                preview.setTimelineEvents(timeline.getEvents());

                this.hasTimeline = true;
            }
        }

        private void unbind() {
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
        public void onUpdateAvailable(@NonNull Timeline timeline) {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                bind(date, timeline);
            }
        }

        @Override
        public void onUpdateFailed(Throwable e) {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                Logger.error(getClass().getSimpleName(), "Could not load item " + getAdapterPosition(), e);
                bind(date, null);
            }
        }

        //endregion


        //region Click Support

        @Override
        public void onClick(View ignored) {
            dispatchClick(this);
        }

        //endregion
    }
}
