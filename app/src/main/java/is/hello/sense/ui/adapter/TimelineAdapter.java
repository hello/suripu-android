package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.widget.timeline.TimelineSegmentDrawable;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.SoundPlayer;
import is.hello.sense.util.StateSafeExecutor;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineBaseViewHolder> implements SoundPlayer.OnEventListener {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_SEGMENT = 1;
    private static final int VIEW_TYPE_EVENT = 2;

    public static final int STATIC_ITEM_COUNT = 1;

    private static final float EVENT_SCALE_MIN = 0.9f;
    private static final float EVENT_SCALE_MAX = 1.0f;


    private final Context context;
    private final LayoutInflater inflater;
    private final View headerView;
    private final DateFormatter dateFormatter;

    private final int segmentMinHeight;
    private final int segmentHeightPerHour;
    private final int segmentEventOffsetMax;
    private final int segmentEventStolenHeight;

    private final List<TimelineSegment> segments = new ArrayList<>();
    private final SparseArray<LocalTime> itemTimes = new SparseArray<>();
    private final SparseArray<Pair<Integer, Integer>> stolenSleepDepths = new SparseArray<>();
    private int[] segmentHeights;

    private boolean use24Time = false;
    private @Nullable StateSafeExecutor onItemClickExecutor;
    private @Nullable OnItemClickListener onItemClickListener;

    private @Nullable SoundPlayer soundPlayer;
    private int playingPosition = RecyclerView.NO_POSITION;


    public TimelineAdapter(@NonNull Context context,
                           @NonNull View headerView,
                           @NonNull DateFormatter dateFormatter) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.headerView = headerView;
        this.dateFormatter = dateFormatter;

        Resources resources = context.getResources();
        this.segmentMinHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_min_height);
        this.segmentHeightPerHour = resources.getDimensionPixelSize(R.dimen.timeline_segment_height_per_hour);
        this.segmentEventOffsetMax = resources.getDimensionPixelSize(R.dimen.timeline_segment_event_offset_max);
        this.segmentEventStolenHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_stolen_height);
    }


    //region Rendering Cache

    private int calculateSegmentHeight(@NonNull TimelineSegment segment, boolean previousSegmentWasEvent) {
        float hours = segment.getDuration() / 3600f;
        int rawHeight = Math.round(segmentHeightPerHour * hours);
        if (previousSegmentWasEvent) {
            rawHeight -= segmentEventOffsetMax + segmentEventStolenHeight;
        }
        return Math.max(segmentMinHeight, rawHeight);
    }

    private void buildCache() {
        int segmentCount = segments.size();
        this.segmentHeights = new int[segmentCount];
        stolenSleepDepths.clear();
        itemTimes.clear();

        Set<Integer> hours = new HashSet<>();
        boolean previousSegmentWasEvent = false;
        for (int i = 0; i < segmentCount; i++) {
            TimelineSegment segment = segments.get(i);

            if (segment.hasEventInfo()) {
                int previousDepth = i > 0 ? segments.get(i - 1).getDisplaySleepDepth() : 0;
                int nextDepth = i < (segmentCount - 1) ? segments.get(i + 1).getDisplaySleepDepth() : 0;
                stolenSleepDepths.put(i + STATIC_ITEM_COUNT, Pair.create(previousDepth, nextDepth));

                this.segmentHeights[i] = ViewGroup.LayoutParams.WRAP_CONTENT;
                previousSegmentWasEvent = true;
            } else {
                int segmentHeight = calculateSegmentHeight(segment, previousSegmentWasEvent);
                this.segmentHeights[i] = segmentHeight;
                previousSegmentWasEvent = false;

            }

            int hour = segment.getShiftedTimestamp().getHourOfDay();
            if (!hours.contains(hour)) {
                if (segment.hasEventInfo()) {
                    int previous = (i - 1) + STATIC_ITEM_COUNT;
                    if (i > 0 && itemTimes.get(previous) == null) {
                        itemTimes.put(previous, segment.getShiftedTimestamp().toLocalTime());
                    } else {
                        continue;
                    }
                } else {
                    itemTimes.put(i + STATIC_ITEM_COUNT, segment.getShiftedTimestamp().toLocalTime());
                }

                hours.add(hour);
            }
        }
    }

    private int getSegmentHeight(int adapterPosition) {
        return segmentHeights[adapterPosition - STATIC_ITEM_COUNT];
    }

    private void clearCache() {
        this.segmentHeights = null;
        stolenSleepDepths.clear();
        itemTimes.clear();
    }

    //endregion


    //region Data

    @Override
    public int getItemCount() {
        return STATIC_ITEM_COUNT + segments.size();
    }

    public TimelineSegment getSegment(int adapterPosition) {
        return segments.get(adapterPosition - STATIC_ITEM_COUNT);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else if (getSegment(position).hasEventInfo()) {
            return VIEW_TYPE_EVENT;
        } else {
            return VIEW_TYPE_SEGMENT;
        }
    }

    public void setUse24Time(boolean use24Time) {
        this.use24Time = use24Time;

        for (int i = 0, size = itemTimes.size(); i < size; i++) {
            int positionWithTime = itemTimes.keyAt(i);
            notifyItemChanged(positionWithTime);
        }
    }

    public void bindSegments(@Nullable List<TimelineSegment> newSegments) {
        stopSoundPlayer();

        int oldSize = segments.size();
        int newSize = newSegments != null ? newSegments.size() : 0;

        segments.clear();
        if (!Lists.isEmpty(newSegments)) {
            segments.addAll(newSegments);
            buildCache();
        }

        if (oldSize > newSize) {
            notifyItemRangeRemoved(STATIC_ITEM_COUNT + newSize, oldSize - newSize);
            notifyItemRangeChanged(STATIC_ITEM_COUNT, newSize);
        } else if (newSize > oldSize) {
            notifyItemRangeInserted(STATIC_ITEM_COUNT + oldSize, newSize - oldSize);
            notifyItemRangeChanged(STATIC_ITEM_COUNT, oldSize);
        } else {
            notifyItemRangeChanged(STATIC_ITEM_COUNT, newSize);
        }
    }

    public void clear() {
        stopSoundPlayer();

        int oldSize = segments.size();
        segments.clear();
        clearCache();
        notifyItemRangeRemoved(STATIC_ITEM_COUNT, oldSize);
    }

    //endregion


    //region Click Support

    public void setOnItemClickListener(@Nullable StateSafeExecutor onItemClickExecutor,
                                       @Nullable OnItemClickListener onItemClickListener) {
        this.onItemClickExecutor = onItemClickExecutor;
        this.onItemClickListener = onItemClickListener;
    }

    private void dispatchItemClick(@NonNull SegmentViewHolder holder) {
        if (onItemClickExecutor != null && onItemClickListener != null) {
            OnItemClickListener onItemClickListener = this.onItemClickListener;
            onItemClickExecutor.execute(() -> {
                int position = holder.getAdapterPosition();
                TimelineSegment segment = getSegment(position);
                if (segment.hasEventInfo()) {
                    onItemClickListener.onEventItemClicked(position, segment);
                } else {
                    onItemClickListener.onSegmentItemClicked(position, holder.itemView, segment);
                }
            });
        }
    }

    //endregion


    //region Playback

    private void setPlayingPosition(int newPosition) {
        int oldPosition = this.playingPosition;
        if (oldPosition != newPosition) {
            this.playingPosition = newPosition;

            if (oldPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition);
            }

            if (newPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(newPosition);
            }
        }
    }

    private void playSegmentSound(int position) {
        if (position == this.playingPosition) {
            stopSoundPlayer();
            return;
        }

        TimelineSegment segment = getSegment(position);
        if (segment.hasSound()) {
            Logger.debug(getClass().getSimpleName(), "playSegmentSound(" + position + ")");

            if (soundPlayer == null) {
                this.soundPlayer = new SoundPlayer(context, this, false);
            }

            String url = segment.getSound().getUrl();
            soundPlayer.play(Uri.parse(url));

            setPlayingPosition(position);
        } else {
            stopSoundPlayer();
        }
    }

    private boolean isSegmentPlaybackActive(int position) {
        return (this.playingPosition == position && soundPlayer != null);
    }

    public void stopSoundPlayer() {
        if (soundPlayer != null) {
            Logger.debug(getClass().getSimpleName(), "stopSoundPlayer()");

            soundPlayer.stopPlayback();
        }
    }

    public boolean isSoundPlayerDisposable() {
        return (soundPlayer != null && !soundPlayer.isPlaying() && !soundPlayer.isLoading());
    }

    public void destroySoundPlayer() {
        if (soundPlayer != null) {
            Logger.debug(getClass().getSimpleName(), "destroySoundPlayer()");

            soundPlayer.stopPlayback();
            soundPlayer.recycle();

            this.soundPlayer = null;
        }
    }


    @Override
    public void onPlaybackStarted(@NonNull SoundPlayer player) {
        Logger.debug(getClass().getSimpleName(), "onPlaybackStarted(" + player + ")");
    }

    @Override
    public void onPlaybackStopped(@NonNull SoundPlayer player, boolean finished) {
        Logger.debug(getClass().getSimpleName(), "onPlaybackStopped(" + player + ", " + finished + ")");
        setPlayingPosition(RecyclerView.NO_POSITION);
    }

    @Override
    public void onPlaybackError(@NonNull SoundPlayer player, @NonNull Throwable error) {
        Logger.debug(getClass().getSimpleName(), "onPlaybackError(" + player + ", " + error + ")");

        Toast.makeText(context.getApplicationContext(), R.string.error_timeline_sound_playback_failed, Toast.LENGTH_SHORT).show();
        setPlayingPosition(RecyclerView.NO_POSITION);

        Analytics.trackError(error, "Timeline event playback");
    }

    @Override
    public void onPlaybackPulse(@NonNull SoundPlayer player, int position) {
    }


    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        destroySoundPlayer();
    }

    //endregion


    //region Vending Views

    @Override
    public TimelineBaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER: {
                return new StaticViewHolder(headerView);
            }

            case VIEW_TYPE_SEGMENT: {
                View segmentView = new View(context);
                segmentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new SegmentViewHolder(segmentView);
            }

            case VIEW_TYPE_EVENT: {
                View segmentView = inflater.inflate(R.layout.item_timeline_segment, parent, false);
                return new EventViewHolder(segmentView);
            }

            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onBindViewHolder(TimelineBaseViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public void onViewRecycled(TimelineBaseViewHolder holder) {
        holder.unbind();
    }

    static class StaticViewHolder extends TimelineBaseViewHolder {
        StaticViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void bind(int position) {
            // Do nothing.
        }
    }

    class SegmentViewHolder extends TimelineBaseViewHolder implements View.OnClickListener {
        final TimelineSegmentDrawable drawable;

        SegmentViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            this.drawable = new TimelineSegmentDrawable(context);
            itemView.setBackground(drawable);
        }

        //region Binding

        @Override
        public final void bind(int position) {
            itemView.getLayoutParams().height = TimelineAdapter.this.getSegmentHeight(position);

            TimelineSegment segment = getSegment(position);
            bindSegment(position, segment);
        }

        void bindSegment(int position, @NonNull TimelineSegment segment) {
            drawable.setSleepDepth(segment.getDisplaySleepDepth());

            LocalTime itemTime = itemTimes.get(position);
            if (itemTime != null) {
                drawable.setTimestamp(dateFormatter.formatForTimelineSegment(itemTime, use24Time));
            } else {
                drawable.setTimestamp(null);
            }
        }

        //endregion


        //region Click Support

        @Override
        public void onClick(View ignored) {
            dispatchItemClick(this);
        }

        //endregion
    }

    public class EventViewHolder extends SegmentViewHolder {
        final ViewGroup container;
        final MarginLayoutParams containerLayoutParams;
        final ImageView iconImage;
        final TextView messageText;
        final TextView dateText;

        private boolean excludedFromParallax = false;
        private float centerDistanceAmount = 0f;
        private float bottomDistanceAmount = 1f;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);

            this.container = (ViewGroup) itemView.findViewById(R.id.item_timeline_segment_container);
            this.containerLayoutParams = (MarginLayoutParams) container.getLayoutParams();
            this.iconImage = (ImageView) itemView.findViewById(R.id.item_timeline_segment_icon);
            this.messageText = (TextView) itemView.findViewById(R.id.item_timeline_segment_message);
            this.dateText = (TextView) itemView.findViewById(R.id.item_timeline_segment_date);

            container.setPivotX(0f);
        }

        //region Binding

        @Override
        void bindSegment(int position, @NonNull TimelineSegment segment) {
            super.bindSegment(position, segment);

            setExcludedFromParallax(segment.isBeforeSleep());

            Pair<Integer, Integer> stolenScores = stolenSleepDepths.get(position);
            if (stolenScores != null) {
                drawable.setStolenTopSleepDepth(stolenScores.first);
                drawable.setStolenBottomSleepDepth(stolenScores.second);
            } else {
                drawable.setStolenBottomSleepDepth(0);
                drawable.setStolenTopSleepDepth(0);
            }

            messageText.setText(segment.getMessage());
            dateText.setText(dateFormatter.formatForTimelineEvent(segment.getShiftedTimestamp(), use24Time));

            if (segment.hasSound()) {
                if (isSegmentPlaybackActive(position)) {
                    iconImage.setImageResource(R.drawable.timeline_event_stop);
                    iconImage.setContentDescription(context.getString(R.string.accessibility_event_stop));
                } else {
                    iconImage.setImageResource(R.drawable.timeline_event_play);
                    iconImage.setContentDescription(context.getString(R.string.accessibility_event_play));
                }

                Views.setSafeOnClickListener(iconImage, ignored -> playSegmentSound(position));
                iconImage.setBackgroundResource(R.drawable.selectable_dark);
            } else {
                int iconRes = Styles.getTimelineSegmentIconRes(segment);
                iconImage.setImageResource(iconRes);
                iconImage.setContentDescription(context.getString(segment.getEventType().accessibilityStringRes));

                iconImage.setOnClickListener(null);
                iconImage.setBackground(null);
            }
        }

        //endregion


        //region Scroll Effect

        public void setExcludedFromParallax(boolean excludedFromParallax) {
            if (excludedFromParallax != this.excludedFromParallax) {
                this.excludedFromParallax = excludedFromParallax;

                if (excludedFromParallax) {
                    container.setTranslationY(0f);

                    containerLayoutParams.topMargin = 0;
                    containerLayoutParams.bottomMargin = 0;
                } else {
                    float translation = Drawing.interpolateFloats(centerDistanceAmount, 0f, segmentEventOffsetMax);
                    container.setTranslationY(translation);

                    containerLayoutParams.topMargin = segmentEventOffsetMax;
                    containerLayoutParams.bottomMargin = segmentEventOffsetMax;
                }
            }
        }

        public void setDistanceAmounts(float bottomDistanceAmount, float centerDistanceAmount) {
            if (this.bottomDistanceAmount != bottomDistanceAmount) {
                float scale = Drawing.interpolateFloats(bottomDistanceAmount, EVENT_SCALE_MIN, EVENT_SCALE_MAX);
                container.setScaleX(scale);
                container.setScaleY(scale);
                container.setAlpha(bottomDistanceAmount);

                this.bottomDistanceAmount = bottomDistanceAmount;
            }

            if (this.centerDistanceAmount != centerDistanceAmount) {
                if (!excludedFromParallax) {
                    float translation = Drawing.interpolateFloats(centerDistanceAmount, 0f, segmentEventOffsetMax);
                    container.setTranslationY(translation);
                }

                this.centerDistanceAmount = centerDistanceAmount;
            }
        }

        //endregion
    }

    //endregion


    public interface OnItemClickListener {
        void onSegmentItemClicked(int position, View view, @NonNull TimelineSegment segment);
        void onEventItemClicked(int position, @NonNull TimelineSegment segment);
    }
}
