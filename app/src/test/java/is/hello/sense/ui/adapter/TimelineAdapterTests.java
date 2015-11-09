package is.hello.sense.ui.adapter;

import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.TimelineEvent;
import is.hello.sense.api.model.v2.TimelineEventBuilder;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.RecyclerAdapterTesting;
import is.hello.sense.util.markup.text.MarkupString;

import static is.hello.sense.util.RecyclerAdapterTesting.createAndBindView;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TimelineAdapterTests extends SenseTestCase {
    private final Random random = new Random();
    private final FrameLayout fakeParent = new FrameLayout(getContext());

    private final View headerView = new View(getContext());
    private final DateFormatter dateFormatter = new DateFormatter(getContext());
    private TimelineAdapter adapter;


    //region Lifecycle

    @Before
    public void setUp() throws Exception {
        this.adapter = new TimelineAdapter(getContext(), dateFormatter);
        adapter.addHeader(headerView);
    }

    //endregion


    //region Binding

    @Test
    public void differentialBindInsertion() throws Exception {
        final RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();
        adapter.registerAdapterDataObserver(observer);

        final List<TimelineEvent> firstBatch =
                Lists.newArrayList(TimelineEventBuilder.randomEvent(random),
                                   TimelineEventBuilder.randomEvent(random));
        adapter.bindEvents(firstBatch);

        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.INSERTED,
                                      adapter.getHeaderCount(), 2);

        final List<TimelineEvent> secondBatch =
                Lists.newArrayList(TimelineEventBuilder.randomEvent(random),
                                   TimelineEventBuilder.randomEvent(random),
                                   TimelineEventBuilder.randomEvent(random));
        adapter.bindEvents(secondBatch);
        assertTrue(observer.hasObservedChange(RecyclerAdapterTesting.Observer.Change.Type.CHANGED,
                                              adapter.getHeaderCount(), 2));
        assertTrue(observer.hasObservedChange(RecyclerAdapterTesting.Observer.Change.Type.INSERTED,
                                              adapter.getHeaderCount() + 2, 1));
    }

    @Test
    public void differentialBindChange() throws Exception {
        final List<TimelineEvent> events =
                Lists.newArrayList(TimelineEventBuilder.randomEvent(random),
                                   TimelineEventBuilder.randomEvent(random));
        adapter.bindEvents(events);

        final RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();
        adapter.registerAdapterDataObserver(observer);

        adapter.bindEvents(events);

        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.CHANGED,
                                      adapter.getHeaderCount(), 2);
    }

    @Test
    public void differentialBindRemove() throws Exception {
        final List<TimelineEvent> firstBatch =
                Lists.newArrayList(TimelineEventBuilder.randomEvent(random),
                                   TimelineEventBuilder.randomEvent(random));
        adapter.bindEvents(firstBatch);

        final RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();
        adapter.registerAdapterDataObserver(observer);

        final List<TimelineEvent> secondBatch =
                Lists.newArrayList(TimelineEventBuilder.randomEvent(random));
        adapter.bindEvents(secondBatch);

        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.CHANGED,
                                      adapter.getHeaderCount(), 1);
        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.REMOVED,
                                      adapter.getHeaderCount() + 1, 1);
    }

    //endregion


    //region Rendering

    private List<TimelineEvent> generateSimpleTimeline() {
        final List<TimelineEvent> events = new ArrayList<>();
        final DateTime base = DateTime.now(DateTimeZone.UTC);

        events.add(new TimelineEventBuilder()
                           .setType(TimelineEvent.Type.IN_BED)
                           .setShiftedTimestamp(base.withTime(1, 20, 0, 0))
                           .build());
        events.add(new TimelineEventBuilder()
                           .setType(TimelineEvent.Type.LIGHTS_OUT)
                           .setMessage(new MarkupString("Blah blah blah"))
                           .setShiftedTimestamp(base.withTime(1, 30, 0, 0))
                           .build());
        events.add(new TimelineEventBuilder()
                           .setType(TimelineEvent.Type.FELL_ASLEEP)
                           .setMessage(new MarkupString("Blah blah blah"))
                           .setShiftedTimestamp(base.withTime(2, 0, 0, 0))
                           .build());
        events.add(new TimelineEventBuilder()
                           .setType(TimelineEvent.Type.IN_BED)
                           .setDuration(30, TimeUnit.MINUTES)
                           .setShiftedTimestamp(base.withTime(3, 30, 0, 0))
                           .build());
        events.add(new TimelineEventBuilder()
                           .setType(TimelineEvent.Type.GENERIC_MOTION)
                           .setMessage(new MarkupString("Y u mov so much???"))
                           .setShiftedTimestamp(base.withTime(4, 45, 0, 0))
                           .build());
        return events;
    }

    @Test
    public void heights() throws Exception {
        adapter.bindEvents(generateSimpleTimeline());

        assertEquals(16, adapter.getSegmentHeight(0));
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, adapter.getSegmentHeight(1));
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, adapter.getSegmentHeight(2));
        assertEquals(25, adapter.getSegmentHeight(3));
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, adapter.getSegmentHeight(4));
    }

    @Test
    public void viewHolderTypes() throws Exception {
        final FrameLayout fakeParent = new FrameLayout(getContext());
        adapter.bindEvents(generateSimpleTimeline());

        final TimelineBaseViewHolder segmentHolder =
                (TimelineBaseViewHolder) adapter.createViewHolder(fakeParent,
                                                                  TimelineAdapter.VIEW_TYPE_SEGMENT);
        assertTrue(segmentHolder instanceof TimelineAdapter.SegmentViewHolder);

        final TimelineBaseViewHolder eventHolder =
                (TimelineBaseViewHolder) adapter.createViewHolder(fakeParent,
                                                                  TimelineAdapter.VIEW_TYPE_EVENT);
        assertTrue(eventHolder instanceof TimelineAdapter.EventViewHolder);
    }

    @Test
    public void segmentRendering() throws Exception {
        final TimelineEvent segment = new TimelineEventBuilder()
                .setShiftedTimestamp(new DateTime(2015, 7, 13, 1, 30, DateTimeZone.UTC))
                .setType(TimelineEvent.Type.IN_BED)
                .setSleepDepth(50, TimelineEvent.SleepState.MEDIUM)
                .build();
        adapter.bindEvents(Lists.newArrayList(segment));

        final TimelineAdapter.SegmentViewHolder holder =
                createAndBindView(adapter, fakeParent,
                                  adapter.getHeaderCount());
        adapter.bindViewHolder(holder, adapter.getHeaderCount());

        assertEquals(0.5f, holder.drawable.getSleepDepthFraction(), 0f);
        assertEquals(0x80009cff, holder.drawable.getSleepDepthColor());

        assertEquals(0f, holder.drawable.getStolenTopSleepDepthFraction(), 0f);
        assertEquals(0f, holder.drawable.getStolenBottomSleepDepthFraction(), 0f);

        //noinspection ConstantConditions
        assertEquals("1 AM", holder.drawable.getTimestamp().toString());
    }

    @Test
    public void eventRendering() throws Exception {
        final TimelineEvent event = new TimelineEventBuilder()
                .setShiftedTimestamp(new DateTime(2015, 7, 13, 1, 30, DateTimeZone.UTC))
                .setType(TimelineEvent.Type.GENERIC_MOTION)
                .setSleepDepth(50, TimelineEvent.SleepState.MEDIUM)
                .setMessage(new MarkupString("Y u mov so much???"))
                .build();
        adapter.bindEvents(Lists.newArrayList(event));

        final TimelineAdapter.EventViewHolder holder =
                createAndBindView(adapter, fakeParent,
                                  adapter.getHeaderCount());
        holder.setExcludedFromParallax(true);

        assertEquals(0.5f, holder.drawable.getSleepDepthFraction(), 0f);
        assertEquals(0x80009cff, holder.drawable.getSleepDepthColor());

        assertEquals(0f, holder.drawable.getStolenTopSleepDepthFraction(), 0f);
        assertEquals(0f, holder.drawable.getStolenBottomSleepDepthFraction(), 0f);

        //noinspection ConstantConditions
        assertEquals("1 AM", holder.drawable.getTimestamp().toString());

        assertEquals("Y u mov so much???", holder.messageText.getText().toString());
        assertEquals("1:30 AM", holder.dateText.getText().toString());
        assertEquals(View.VISIBLE, holder.dateText.getVisibility());

        assertEquals(0, holder.containerLayoutParams.topMargin);
        assertEquals(0, holder.containerLayoutParams.bottomMargin);

        assertEquals(ResourcesCompat.getDrawable(getResources(), R.drawable.timeline_generic_motion, null),
                     holder.iconImage.getDrawable());
    }

    @Test
    public void alarmRangRendering() throws Exception {
        final TimelineEvent event = new TimelineEventBuilder()
                .setShiftedTimestamp(new DateTime(2015, 7, 13, 1, 30, DateTimeZone.UTC))
                .setType(TimelineEvent.Type.ALARM_RANG)
                .setSleepDepth(50, TimelineEvent.SleepState.MEDIUM)
                .setMessage(new MarkupString("Alarm rang at 8:00"))
                .build();
        adapter.bindEvents(Lists.newArrayList(event));

        final TimelineAdapter.EventViewHolder holder =
                createAndBindView(adapter, fakeParent,
                                  adapter.getHeaderCount());
        holder.setExcludedFromParallax(true);

        assertNotEquals(View.VISIBLE, holder.dateText.getVisibility());
    }

    @Test
    public void eventScoreStealing() throws Exception {
        final List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEventBuilder()
                           .setType(TimelineEvent.Type.GENERIC_MOTION)
                           .setSleepDepth(10, TimelineEvent.SleepState.MEDIUM)
                           .build());
        events.add(new TimelineEventBuilder()
                           .setType(TimelineEvent.Type.GENERIC_MOTION)
                           .setSleepDepth(80, TimelineEvent.SleepState.MEDIUM)
                           .build());
        events.add(new TimelineEventBuilder()
                           .setType(TimelineEvent.Type.GENERIC_MOTION)
                           .setSleepDepth(40, TimelineEvent.SleepState.MEDIUM)
                           .build());
        adapter.bindEvents(events);

        final int headerCount = adapter.getHeaderCount();

        final TimelineAdapter.EventViewHolder holder1 =
                createAndBindView(adapter, fakeParent,
                                  headerCount);
        assertEquals(0f, holder1.drawable.getStolenTopSleepDepthFraction(), 0f);
        assertEquals(0.1f, holder1.drawable.getSleepDepthFraction(), 0f);
        assertEquals(0.8f, holder1.drawable.getStolenBottomSleepDepthFraction(), 0f);

        final TimelineAdapter.EventViewHolder holder2 =
                createAndBindView(adapter, fakeParent,
                                  headerCount + 1);
        assertEquals(0.1f, holder2.drawable.getStolenTopSleepDepthFraction(), 0f);
        assertEquals(0.8f, holder2.drawable.getSleepDepthFraction(), 0f);
        assertEquals(0.4f, holder2.drawable.getStolenBottomSleepDepthFraction(), 0f);

        final TimelineAdapter.EventViewHolder holder3 =
                createAndBindView(adapter, fakeParent,
                                  headerCount + 2);
        assertEquals(0.8f, holder3.drawable.getStolenTopSleepDepthFraction(), 0f);
        assertEquals(0.4f, holder3.drawable.getSleepDepthFraction(), 0f);
        assertEquals(0f, holder3.drawable.getStolenBottomSleepDepthFraction(), 0f);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void use24TimeUpdating() throws Exception {
        adapter.setUse24Time(false);

        final List<TimelineEvent> events = new ArrayList<>();
        final DateTime timestamp = DateTime.now(DateTimeZone.UTC).withTime(0, 30, 0, 0);
        events.add(new TimelineEventBuilder()
                           .setType(TimelineEvent.Type.GOT_IN_BED)
                           .setSleepDepth(0, TimelineEvent.SleepState.AWAKE)
                           .setMessage(new MarkupString("Whatevs"))
                           .setShiftedTimestamp(timestamp)
                           .build());
        adapter.bindEvents(events);

        final int headerCount = adapter.getHeaderCount();

        final TimelineAdapter.EventViewHolder holder12Hour =
                createAndBindView(adapter, fakeParent,
                                  headerCount);

        assertEquals("12 AM", holder12Hour.drawable.getTimestamp().toString());
        assertEquals("12:30 AM", holder12Hour.dateText.getText().toString());

        adapter.setUse24Time(true);

        final TimelineAdapter.EventViewHolder holder24Hour =
                createAndBindView(adapter, fakeParent,
                                  headerCount);

        assertEquals("00:00", holder24Hour.drawable.getTimestamp().toString());
        assertEquals("00:30", holder24Hour.dateText.getText().toString());
    }

    //endregion
}
