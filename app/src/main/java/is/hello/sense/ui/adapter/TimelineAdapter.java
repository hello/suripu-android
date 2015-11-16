package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
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
import java.util.concurrent.TimeUnit;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.widget.timeline.TimelineSegmentDrawable;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Player;
import is.hello.sense.util.StateSafeExecutor;

public class TimelineAdapter extends HeadersRecyclerAdapter<TimelineBaseViewHolder>
        implements Player.OnEventListener {
    @VisibleForTesting static final int VIEW_TYPE_SEGMENT = 1;
    @VisibleForTesting static final int VIEW_TYPE_EVENT = 2;

    private static final float EVENT_SCALE_MIN = 0.9f;
    private static final float EVENT_SCALE_MAX = 1.0f;


    private final Context context;
    private final LayoutInflater inflater;
    private final DateFormatter dateFormatter;

    private final int segmentMinHeight;
    private final int segmentHeightPerHour;
    private final int segmentEventOffsetMax;
    private final int segmentEventStolenHeight;

    private final List<TimelineEvent> events = new ArrayList<>();
    private final SparseArray<LocalTime> itemTimes = new SparseArray<>();
    private final SparseArray<Pair<TimelineEvent, TimelineEvent>> stolenSleepDepths = new SparseArray<>();
    private int[] segmentHeights;

    private boolean use24Time = false;
    private @Nullable StateSafeExecutor onItemClickExecutor;
    private @Nullable OnItemClickListener onItemClickListener;

    private @Nullable Player player;
    private int playingPosition = RecyclerView.NO_POSITION;


    public TimelineAdapter(@NonNull Context context,
                           @NonNull DateFormatter dateFormatter) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.dateFormatter = dateFormatter;

        final Resources resources = context.getResources();
        this.segmentMinHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_min_height);
        this.segmentHeightPerHour = resources.getDimensionPixelSize(R.dimen.timeline_segment_height_per_hour);
        this.segmentEventOffsetMax = resources.getDimensionPixelSize(R.dimen.timeline_segment_event_offset_max);
        this.segmentEventStolenHeight = resources.getDimensionPixelSize(R.dimen.timeline_segment_stolen_height);
    }


    //region Rendering Cache

    private int calculateSegmentHeight(@NonNull TimelineEvent segment, boolean previousSegmentWasEvent) {
        final float hours = segment.getDuration(TimeUnit.SECONDS) / 3600f;
        int rawHeight = Math.round(segmentHeightPerHour * hours);
        if (previousSegmentWasEvent) {
            rawHeight -= segmentEventOffsetMax + segmentEventStolenHeight;
        }
        return Math.max(segmentMinHeight, rawHeight);
    }

    private void buildCache() {
        final int eventCount = events.size();
        this.segmentHeights = new int[eventCount];
        stolenSleepDepths.clear();
        itemTimes.clear();

        final Set<Integer> hours = new HashSet<>();
        boolean previousEventHadInfo = false;
        for (int i = 0; i < eventCount; i++) {
            final TimelineEvent event = events.get(i);

            if (event.hasInfo()) {
                final TimelineEvent previousEvent = i > 0 ? events.get(i - 1) : null;
                final TimelineEvent nextEvent = i < (eventCount - 1) ? events.get(i + 1) : null;
                stolenSleepDepths.put(i, Pair.create(previousEvent, nextEvent));

                this.segmentHeights[i] = ViewGroup.LayoutParams.WRAP_CONTENT;
                previousEventHadInfo = true;
            } else {
                int segmentHeight = calculateSegmentHeight(event, previousEventHadInfo);
                this.segmentHeights[i] = segmentHeight;
                previousEventHadInfo = false;

            }

            final int hour = event.getShiftedTimestamp().getHourOfDay();
            if (!hours.contains(hour)) {
                itemTimes.put(i, event.getShiftedTimestamp().toLocalTime());
                hours.add(hour);
            }
        }
    }

    @VisibleForTesting
    int getSegmentHeight(int contentPosition) {
        return segmentHeights[contentPosition];
    }

    private void clearCache() {
        this.segmentHeights = null;
        stolenSleepDepths.clear();
        itemTimes.clear();
    }

    //endregion


    //region Data

    @Override
    public int getContentItemCount() {
        return events.size();
    }

    public boolean hasEvents() {
        return !events.isEmpty();
    }

    public TimelineEvent getEvent(int contentPosition) {
        return events.get(contentPosition);
    }

    @Override
    public int getContentItemViewType(int contentPosition) {
        if (getEvent(contentPosition).hasInfo()) {
            return VIEW_TYPE_EVENT;
        } else {
            return VIEW_TYPE_SEGMENT;
        }
    }

    public void setUse24Time(boolean use24Time) {
        if (this.use24Time != use24Time) {
            this.use24Time = use24Time;
            notifyContentItemRangeChanged(getHeaderCount(), events.size());
        }
    }

    public void bindEvents(@Nullable List<TimelineEvent> newEvents) {
        stopSoundPlayer();

        final int oldSize = events.size();
        final int newSize = newEvents != null ? newEvents.size() : 0;

        events.clear();
        if (!Lists.isEmpty(newEvents)) {
            events.addAll(newEvents);
            buildCache();
        }

        if (oldSize > newSize) {
            notifyContentItemRangeRemoved(newSize, oldSize - newSize);
            notifyContentItemRangeChanged(0, newSize);
        } else if (newSize > oldSize) {
            notifyContentItemRangeInserted(oldSize, newSize - oldSize);
            notifyContentItemRangeChanged(0, oldSize);
        } else {
            notifyContentItemRangeChanged(0, newSize);
        }
    }

    public void clear() {
        stopSoundPlayer();

        final int oldSize = events.size();
        events.clear();
        clearCache();
        notifyContentItemRangeRemoved(0, oldSize);
    }

    //endregion


    //region Click Support

    public void setOnItemClickListener(@Nullable StateSafeExecutor onItemClickExecutor,
                                       @Nullable OnItemClickListener onItemClickListener) {
        this.onItemClickExecutor = onItemClickExecutor;
        this.onItemClickListener = onItemClickListener;
    }

    private void dispatchItemClick(@NonNull SegmentViewHolder holder) {
        // View dispatches OnClickListener#onClick(View) calls on
        // the next looper cycle. It's possible for the adapter's
        // containing recycler view to update and invalidate a
        // view holder before the callback fires.
        final int adapterPosition = holder.getContentPosition();
        if (onItemClickExecutor != null && onItemClickListener != null &&
                adapterPosition != RecyclerView.NO_POSITION) {
            final OnItemClickListener onItemClickListener = this.onItemClickListener;
            onItemClickExecutor.execute(() -> {
                final TimelineEvent event = getEvent(adapterPosition);
                if (event.hasInfo()) {
                    onItemClickListener.onEventItemClicked(adapterPosition, event);
                } else {
                    onItemClickListener.onSegmentItemClicked(adapterPosition, holder.itemView, event);
                }
            });
        }
    }

    //endregion


    //region Playback

    private void setPlayingPosition(int newPosition) {
        final int oldPosition = this.playingPosition;
        if (oldPosition != newPosition) {
            this.playingPosition = newPosition;

            if (oldPosition != RecyclerView.NO_POSITION) {
                notifyContentItemChanged(oldPosition);
            }

            if (newPosition != RecyclerView.NO_POSITION) {
                notifyContentItemChanged(newPosition);
            }
        }
    }

    private void playSegmentSound(int position) {
        if (position == this.playingPosition) {
            stopSoundPlayer();
            return;
        }

        final TimelineEvent event = getEvent(position);
        if (event.hasSound()) {
            Logger.debug(getClass().getSimpleName(), "playSegmentSound(" + position + ")");

            if (player == null) {
                this.player = new Player(context, this, null);
            }

            final String url = event.getSoundUrl();
            player.setDataSource(Uri.parse(url), true);

            setPlayingPosition(position);
        } else {
            stopSoundPlayer();
        }
    }

    private boolean isSegmentPlaybackActive(int position) {
        return (this.playingPosition == position && player != null);
    }

    public void stopSoundPlayer() {
        if (player != null) {
            Logger.debug(getClass().getSimpleName(), "stopSoundPlayer()");

            player.stopPlayback();
        }
    }

    public boolean isSoundPlayerDisposable() {
        return (player != null && player.getState() < Player.STATE_LOADING);
    }

    public void destroySoundPlayer() {
        if (player != null) {
            Logger.debug(getClass().getSimpleName(), "destroySoundPlayer()");

            player.stopPlayback();
            player.recycle();

            this.player = null;
        }
    }


    @Override
    public void onPlaybackReady(@NonNull Player player) {
        Logger.debug(getClass().getSimpleName(), "onPlaybackReady(" + player + ")");
    }

    @Override
    public void onPlaybackStarted(@NonNull Player player) {
        Logger.debug(getClass().getSimpleName(), "onPlaybackStarted(" + player + ")");
    }

    @Override
    public void onPlaybackStopped(@NonNull Player player, boolean finished) {
        Logger.debug(getClass().getSimpleName(), "onPlaybackStopped(" + player + ", " + finished + ")");
        setPlayingPosition(RecyclerView.NO_POSITION);
    }

    @Override
    public void onPlaybackError(@NonNull Player player, @NonNull Throwable error) {
        Logger.debug(getClass().getSimpleName(), "onPlaybackError(" + player + ", " + error + ")");

        Toast.makeText(context.getApplicationContext(), R.string.error_timeline_sound_playback_failed, Toast.LENGTH_SHORT).show();
        setPlayingPosition(RecyclerView.NO_POSITION);

        Analytics.trackError(error, "Timeline event playback");
    }


    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        destroySoundPlayer();
    }

    //endregion


    //region Vending Views


    @Override
    public TimelineBaseViewHolder onCreateContentViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SEGMENT) {
            final View segmentView = new View(context);
            segmentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                   ViewGroup.LayoutParams.WRAP_CONTENT));
            return new SegmentViewHolder(segmentView);
        } else if (viewType == VIEW_TYPE_EVENT) {
            final View segmentView = inflater.inflate(R.layout.item_timeline_segment, parent, false);
            return new EventViewHolder(segmentView);
        } else {
            throw new IllegalArgumentException("Unknown view type " + viewType);
        }
    }

    @Override
    public void onBindContentViewHolder(TimelineBaseViewHolder holder, int position) {
        holder.bind(position);
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

            final TimelineEvent event = getEvent(position);
            bindEvent(position, event);
        }

        void bindEvent(int position, @NonNull TimelineEvent event) {
            drawable.setSleepDepth(event.getSleepDepth(), event.getSleepState());

            final LocalTime itemTime = itemTimes.get(position);
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
        void bindEvent(int position, @NonNull TimelineEvent event) {
            super.bindEvent(position, event);

            setExcludedFromParallax(event.getSleepState() == TimelineEvent.SleepState.AWAKE);

            final Pair<TimelineEvent, TimelineEvent> stolenScores = stolenSleepDepths.get(position);
            if (stolenScores != null) {
                final TimelineEvent top = stolenScores.first;
                if (top != null) {
                    drawable.setStolenTopSleepDepth(top.getSleepDepth(), top.getSleepState());
                } else {
                    drawable.setStolenTopSleepDepth(0, TimelineEvent.SleepState.AWAKE);
                }

                final TimelineEvent bottom = stolenScores.second;
                if (bottom != null) {
                    drawable.setStolenBottomSleepDepth(bottom.getSleepDepth(), bottom.getSleepState());
                } else {
                    drawable.setStolenBottomSleepDepth(0, TimelineEvent.SleepState.AWAKE);
                }
            } else {
                drawable.setStolenBottomSleepDepth(0, TimelineEvent.SleepState.AWAKE);
                drawable.setStolenTopSleepDepth(0, TimelineEvent.SleepState.AWAKE);
            }

            messageText.setText(event.getMessage());
            if (event.getType() == TimelineEvent.Type.ALARM_RANG) {
                dateText.setVisibility(View.GONE);
            } else {
                dateText.setText(dateFormatter.formatForTimelineEvent(event.getShiftedTimestamp(), use24Time));
                dateText.setVisibility(View.VISIBLE);
            }

            if (event.hasSound()) {
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
                iconImage.setImageResource(event.getType().iconDrawableRes);
                iconImage.setContentDescription(context.getString(event.getType().accessibilityStringRes));

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
                    final float translation = Anime.interpolateFloats(centerDistanceAmount,
                                                                      0f, segmentEventOffsetMax);
                    container.setTranslationY(translation);

                    containerLayoutParams.topMargin = segmentEventOffsetMax;
                    containerLayoutParams.bottomMargin = segmentEventOffsetMax;
                }
            }
        }

        public void setDistanceAmounts(float bottomDistanceAmount, float centerDistanceAmount) {
            if (this.bottomDistanceAmount != bottomDistanceAmount) {
                final float scale = Anime.interpolateFloats(bottomDistanceAmount,
                                                            EVENT_SCALE_MIN, EVENT_SCALE_MAX);
                container.setScaleX(scale);
                container.setScaleY(scale);
                container.setAlpha(bottomDistanceAmount);

                this.bottomDistanceAmount = bottomDistanceAmount;
            }

            if (this.centerDistanceAmount != centerDistanceAmount) {
                if (!excludedFromParallax) {
                    final float translation = Anime.interpolateFloats(centerDistanceAmount,
                                                                      0f, segmentEventOffsetMax);
                    container.setTranslationY(translation);
                }

                this.centerDistanceAmount = centerDistanceAmount;
            }
        }

        //endregion
    }

    //endregion


    public interface OnItemClickListener {
        void onSegmentItemClicked(int position, View view, @NonNull TimelineEvent event);
        void onEventItemClicked(int position, @NonNull TimelineEvent event);
    }
}
