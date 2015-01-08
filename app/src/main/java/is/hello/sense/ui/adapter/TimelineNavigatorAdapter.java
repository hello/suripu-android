package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.DateTime;

import is.hello.sense.R;
import is.hello.sense.api.model.Timeline;
import is.hello.sense.graph.presenters.TimelineNavigatorPresenter;
import is.hello.sense.ui.widget.MiniTimelineView;
import is.hello.sense.ui.widget.SleepScoreDrawable;
import is.hello.sense.ui.widget.util.Styles;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class TimelineNavigatorAdapter extends RecyclerView.Adapter<TimelineNavigatorAdapter.ItemViewHolder> implements View.OnClickListener {
    public static final int TOTAL_DAYS = 366;

    private static final int ERROR_MARKER = -1;

    private final Context context;
    private final LayoutInflater inflater;
    private final TimelineNavigatorPresenter presenter;

    private @Nullable OnItemClickedListener onItemClickedListener;

    public TimelineNavigatorAdapter(@NonNull Context context,
                                    @NonNull TimelineNavigatorPresenter presenter) {
        this.context = context;
        this.presenter = presenter;
        this.inflater = LayoutInflater.from(context);
    }


    //region Population

    @Override
    public int getItemCount() {
        return TOTAL_DAYS;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View itemView = inflater.inflate(R.layout.item_timeline_navigator, viewGroup, false);
        itemView.setOnClickListener(this);
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        DateTime date = presenter.getDateTimeAt(position);

        holder.itemView.setTag(position);

        holder.date = date;
        holder.dayNumber.setText(date.toString("d"));
        holder.dayName.setText(date.toString("EE"));
        holder.pieDrawable.setValue(0);
        holder.score.setText(R.string.missing_data_placeholder);
        presenter.post(holder, holder::populate);
    }

    @Override
    public void onViewDetachedFromWindow(ItemViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        presenter.cancel(holder);
    }

    @Override
    public void onViewRecycled(ItemViewHolder holder) {
        super.onViewRecycled(holder);

        presenter.cancel(holder);
        holder.reset();
    }

    //endregion


    //region Click Listener

    public void setOnItemClickedListener(@Nullable OnItemClickedListener onItemClickedListener) {
        this.onItemClickedListener = onItemClickedListener;
    }

    @Override
    public void onClick(View view) {
        if (onItemClickedListener != null) {
            int position = (int) view.getTag();
            onItemClickedListener.onItemClicked(view, position);
        }
    }

    public interface OnItemClickedListener {
        void onItemClicked(@NonNull View itemView, int position);
    }

    //endregion


    public class ItemViewHolder extends RecyclerView.ViewHolder {
        public final TextView dayNumber;
        public final TextView dayName;
        public final TextView score;
        public final MiniTimelineView timeline;

        public final SleepScoreDrawable pieDrawable;

        public @Nullable DateTime date;

        private @Nullable Subscription loading;

        private ItemViewHolder(View itemView) {
            super(itemView);

            this.dayNumber = (TextView) itemView.findViewById(R.id.item_timeline_navigator_day_number);
            this.dayName = (TextView) itemView.findViewById(R.id.item_timeline_navigator_day_name);
            this.score = (TextView) itemView.findViewById(R.id.item_timeline_navigator_score);
            this.timeline = (MiniTimelineView) itemView.findViewById(R.id.item_timeline_navigator_timeline);

            this.pieDrawable = new SleepScoreDrawable(context.getResources());

            View pieView = itemView.findViewById(R.id.item_timeline_navigator_pie);
            pieView.setBackground(pieDrawable);
        }


        void setTimeline(@Nullable Timeline timeline) {
            if (timeline == null) {
                int sleepScoreColor = context.getResources().getColor(R.color.sensor_warning);
                score.setText(R.string.missing_data_placeholder);
                score.setTextColor(sleepScoreColor);
                pieDrawable.setFillColor(sleepScoreColor);
                pieDrawable.setValue(100);

                this.timeline.setTimelineSegments(null);
            } else {
                int sleepScore = timeline.getScore();
                int sleepScoreColor = Styles.getSleepScoreColor(context, sleepScore);
                score.setText(Integer.toString(sleepScore));
                score.setTextColor(sleepScoreColor);
                pieDrawable.setFillColor(sleepScoreColor);
                pieDrawable.setValue(sleepScore);

                this.timeline.setTimelineSegments(timeline.getSegments());
            }
        }

        void reset() {
            if (loading != null) {
                loading.unsubscribe();
                this.loading = null;
            }

            this.date = null;

            pieDrawable.setValue(0);
            pieDrawable.setTrackColor(context.getResources().getColor(R.color.border));
            score.setText(R.string.missing_data_placeholder);
            score.setTextColor(context.getResources().getColor(R.color.text_dark));

            timeline.setTimelineSegments(null);
        }

        void populate() {
            if (loading == null && date != null) {
                this.loading = presenter.timelineForDate(date)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(this::setTimeline, ignored -> setTimeline(null));
            }
        }
    }
}
