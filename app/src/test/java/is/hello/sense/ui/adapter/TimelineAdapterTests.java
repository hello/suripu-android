package is.hello.sense.ui.adapter;

import android.content.res.Resources;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.joda.time.DateTime;
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
        View[] headers = { headerView };
        this.adapter = new TimelineAdapter(getContext(), dateFormatter, headers);
    }

    //endregion


    //region Binding

    @Test
    public void differentialBindInsertion() throws Exception {
        RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();
        adapter.registerAdapterDataObserver(observer);

        List<TimelineEvent> firstBatch = Lists.newArrayList(
                TimelineEventBuilder.randomEvent(random),
                TimelineEventBuilder.randomEvent(random)
        );
        adapter.bindEvents(firstBatch);

        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.INSERTED,
                adapter.getHeaderCount(), 2);

        List<TimelineEvent> secondBatch = Lists.newArrayList(
                TimelineEventBuilder.randomEvent(random),
                TimelineEventBuilder.randomEvent(random),
                TimelineEventBuilder.randomEvent(random)
        );
        adapter.bindEvents(secondBatch);
        assertTrue(observer.hasObservedChange(RecyclerAdapterTesting.Observer.Change.Type.CHANGED,
                adapter.getHeaderCount(), 2));
        assertTrue(observer.hasObservedChange(RecyclerAdapterTesting.Observer.Change.Type.INSERTED,
                adapter.getHeaderCount() + 2, 1));
    }

    @Test
    public void differentialBindChange() throws Exception {
        List<TimelineEvent> events = Lists.newArrayList(
                TimelineEventBuilder.randomEvent(random),
                TimelineEventBuilder.randomEvent(random)
        );
        adapter.bindEvents(events);

        RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();
        adapter.registerAdapterDataObserver(observer);

        adapter.bindEvents(events);

        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.CHANGED,
                adapter.getHeaderCount(), 2);
    }

    @Test
    public void differentialBindRemove() throws Exception {
        List<TimelineEvent> firstBatch = Lists.newArrayList(
                TimelineEventBuilder.randomEvent(random),
                TimelineEventBuilder.randomEvent(random)
        );
        adapter.bindEvents(firstBatch);

        RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();
        adapter.registerAdapterDataObserver(observer);

        List<TimelineEvent> secondBatch = Lists.newArrayList(
                TimelineEventBuilder.randomEvent(random)
        );
        adapter.bindEvents(secondBatch);

        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.CHANGED,
                adapter.getHeaderCount(), 1);
        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.REMOVED,
                adapter.getHeaderCount() + 1, 1);
    }

    //endregion


    //region Rendering

    private List<TimelineEvent> generateSimpleTimeline() {
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEventBuilder()
                .setType(TimelineEvent.Type.IN_BED)
                .build());
        events.add(new TimelineEventBuilder()
                .setType(TimelineEvent.Type.LIGHTS_OUT)
                .setMessage(new MarkupString("Blah blah blah"))
                .build());
        events.add(new TimelineEventBuilder()
                .setType(TimelineEvent.Type.FELL_ASLEEP)
                .setMessage(new MarkupString("Blah blah blah"))
                .build());
        events.add(new TimelineEventBuilder()
                .setType(TimelineEvent.Type.IN_BED)
                .setDuration(30, TimeUnit.MINUTES)
                .build());
        events.add(new TimelineEventBuilder()
                .setType(TimelineEvent.Type.GENERIC_MOTION)
                .setMessage(new MarkupString("Y u mov so much???"))
                .build());
        return events;
    }

    @Test
    public void heights() throws Exception {
        adapter.bindEvents(generateSimpleTimeline());

        int headerCount = adapter.getHeaderCount();
        assertEquals(16, adapter.getSegmentHeight(headerCount));
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, adapter.getSegmentHeight(headerCount + 1));
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, adapter.getSegmentHeight(headerCount + 2));
        assertEquals(25, adapter.getSegmentHeight(headerCount + 3));
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, adapter.getSegmentHeight(headerCount + 4));
    }

    @Test
    public void viewHolderTypes() throws Exception {
        FrameLayout fakeParent = new FrameLayout(getContext());
        adapter.bindEvents(generateSimpleTimeline());

        TimelineBaseViewHolder headerHolder = adapter.createViewHolder(fakeParent, 0 /* header view type = position */);
        assertTrue(headerHolder instanceof TimelineAdapter.StaticViewHolder);

        TimelineBaseViewHolder segmentHolder = adapter.createViewHolder(fakeParent, TimelineAdapter.VIEW_TYPE_SEGMENT);
        assertTrue(segmentHolder instanceof TimelineAdapter.SegmentViewHolder);

        TimelineBaseViewHolder eventHolder = adapter.createViewHolder(fakeParent, TimelineAdapter.VIEW_TYPE_EVENT);
        assertTrue(eventHolder instanceof TimelineAdapter.EventViewHolder);
    }

    @Test
    public void segmentRendering() throws Exception {
        TimelineEvent segment = new TimelineEventBuilder()
                .setShiftedTimestamp(new DateTime(2015, 7, 13, 1, 30))
                .setType(TimelineEvent.Type.IN_BED)
                .setSleepDepth(50, TimelineEvent.SleepState.MEDIUM)
                .build();
        adapter.bindEvents(Lists.newArrayList(segment));

        TimelineAdapter.SegmentViewHolder holder = createAndBindView(adapter, fakeParent,
                TimelineAdapter.VIEW_TYPE_SEGMENT, adapter.getHeaderCount());
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
        TimelineEvent event = new TimelineEventBuilder()
                .setShiftedTimestamp(new DateTime(2015, 7, 13, 1, 30))
                .setType(TimelineEvent.Type.GENERIC_MOTION)
                .setSleepDepth(50, TimelineEvent.SleepState.MEDIUM)
                .setMessage(new MarkupString("Y u mov so much???"))
                .build();
        adapter.bindEvents(Lists.newArrayList(event));

        TimelineAdapter.EventViewHolder holder = createAndBindView(adapter, fakeParent,
                TimelineAdapter.VIEW_TYPE_EVENT, adapter.getHeaderCount());
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

        Resources resources = getContext().getResources();
        assertEquals(ResourcesCompat.getDrawable(resources, R.drawable.timeline_generic_motion, null),
                holder.iconImage.getDrawable());
    }

    @Test
    public void alarmRangRendering() throws Exception {
        TimelineEvent event = new TimelineEventBuilder()
                .setShiftedTimestamp(new DateTime(2015, 7, 13, 1, 30))
                .setType(TimelineEvent.Type.ALARM_RANG)
                .setSleepDepth(50, TimelineEvent.SleepState.MEDIUM)
                .setMessage(new MarkupString("Alarm rang at 8:00"))
                .build();
        adapter.bindEvents(Lists.newArrayList(event));

        TimelineAdapter.EventViewHolder holder = createAndBindView(adapter, fakeParent,
                TimelineAdapter.VIEW_TYPE_EVENT, adapter.getHeaderCount());
        holder.setExcludedFromParallax(true);

        assertNotEquals(View.VISIBLE, holder.dateText.getVisibility());
    }

    @Test
    public void eventScoreStealing() throws Exception {
        List<TimelineEvent> events = new ArrayList<>();
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

        int headerCount = adapter.getHeaderCount();

        TimelineAdapter.EventViewHolder holder1 = createAndBindView(adapter, fakeParent,
                TimelineAdapter.VIEW_TYPE_EVENT, headerCount);
        assertEquals(0f, holder1.drawable.getStolenTopSleepDepthFraction(), 0f);
        assertEquals(0.1f, holder1.drawable.getSleepDepthFraction(), 0f);
        assertEquals(0.8f, holder1.drawable.getStolenBottomSleepDepthFraction(), 0f);

        TimelineAdapter.EventViewHolder holder2 = createAndBindView(adapter, fakeParent,
                TimelineAdapter.VIEW_TYPE_EVENT, headerCount + 1);
        assertEquals(0.1f, holder2.drawable.getStolenTopSleepDepthFraction(), 0f);
        assertEquals(0.8f, holder2.drawable.getSleepDepthFraction(), 0f);
        assertEquals(0.4f, holder2.drawable.getStolenBottomSleepDepthFraction(), 0f);

        TimelineAdapter.EventViewHolder holder3 = createAndBindView(adapter, fakeParent,
                TimelineAdapter.VIEW_TYPE_EVENT, headerCount + 2);
        assertEquals(0.8f, holder3.drawable.getStolenTopSleepDepthFraction(), 0f);
        assertEquals(0.4f, holder3.drawable.getSleepDepthFraction(), 0f);
        assertEquals(0f, holder3.drawable.getStolenBottomSleepDepthFraction(), 0f);
    }

    //endregion
}
