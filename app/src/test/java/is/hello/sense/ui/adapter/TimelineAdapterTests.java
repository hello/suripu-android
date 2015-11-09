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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

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
        assertThat(observer.hasObservedChange(RecyclerAdapterTesting.Observer.Change.Type.CHANGED,
                                              adapter.getHeaderCount(), 2),
                   is(true));
        assertThat(observer.hasObservedChange(RecyclerAdapterTesting.Observer.Change.Type.INSERTED,
                                              adapter.getHeaderCount() + 2, 1),
                   is(true));
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

        assertThat(adapter.getSegmentHeight(0), is(equalTo(16)));
        assertThat(adapter.getSegmentHeight(1), is(equalTo(ViewGroup.LayoutParams.WRAP_CONTENT)));
        assertThat(adapter.getSegmentHeight(2), is(equalTo(ViewGroup.LayoutParams.WRAP_CONTENT)));
        assertThat(adapter.getSegmentHeight(3), is(equalTo(25)));
        assertThat(adapter.getSegmentHeight(4), is(equalTo(ViewGroup.LayoutParams.WRAP_CONTENT)));
    }

    @Test
    public void viewHolderTypes() throws Exception {
        final FrameLayout fakeParent = new FrameLayout(getContext());
        adapter.bindEvents(generateSimpleTimeline());

        final TimelineBaseViewHolder segmentHolder =
                (TimelineBaseViewHolder) adapter.createViewHolder(fakeParent,
                                                                  TimelineAdapter.VIEW_TYPE_SEGMENT);
        assertThat(segmentHolder, is(instanceOf(TimelineAdapter.SegmentViewHolder.class)));

        final TimelineBaseViewHolder eventHolder =
                (TimelineBaseViewHolder) adapter.createViewHolder(fakeParent,
                                                                  TimelineAdapter.VIEW_TYPE_EVENT);
        assertThat(eventHolder, is(instanceOf(TimelineAdapter.EventViewHolder.class)));
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

        assertThat(holder.drawable.getSleepDepthFraction(), is(equalTo(0.5f)));
        assertThat(holder.drawable.getSleepDepthColor(), is(equalTo(0x80009cff)));

        assertThat(holder.drawable.getStolenTopSleepDepthFraction(), is(equalTo(0f)));
        assertThat(holder.drawable.getStolenBottomSleepDepthFraction(), is(equalTo(0f)));

        //noinspection ConstantConditions
        assertThat(holder.drawable.getTimestamp().toString(), is(equalTo("1 AM")));
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

        assertThat(holder.drawable.getSleepDepthFraction(), is(equalTo(0.5f)));
        assertThat(holder.drawable.getSleepDepthColor(), is(equalTo(0x80009cff)));

        assertThat(holder.drawable.getStolenTopSleepDepthFraction(), is(equalTo(0f)));
        assertThat(holder.drawable.getStolenBottomSleepDepthFraction(), is(equalTo(0f)));

        //noinspection ConstantConditions
        assertThat(holder.drawable.getTimestamp().toString(), is(equalTo("1 AM")));

        assertThat(holder.messageText.getText().toString(), is(equalTo("Y u mov so much???")));
        assertThat(holder.dateText.getText().toString(), is(equalTo("1:30 AM")));
        assertThat(holder.dateText.getVisibility(), is(equalTo(View.VISIBLE)));

        assertThat(holder.containerLayoutParams.topMargin, is(equalTo(0)));
        assertThat(holder.containerLayoutParams.bottomMargin, is(equalTo(0)));

        assertThat(holder.iconImage.getDrawable(), is(equalTo(ResourcesCompat.getDrawable(getResources(), R.drawable.timeline_generic_motion, null))));
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

        assertThat(holder.dateText.getVisibility(), is(not(equalTo(View.VISIBLE))));
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
        assertThat(holder1.drawable.getStolenTopSleepDepthFraction(), is(equalTo(0f)));
        assertThat(holder1.drawable.getSleepDepthFraction(), is(equalTo(0.1f)));
        assertThat(holder1.drawable.getStolenBottomSleepDepthFraction(), is(equalTo(0.8f)));

        final TimelineAdapter.EventViewHolder holder2 =
                createAndBindView(adapter, fakeParent,
                                  headerCount + 1);
        assertThat(holder2.drawable.getStolenTopSleepDepthFraction(), is(equalTo(0.1f)));
        assertThat(holder2.drawable.getSleepDepthFraction(), is(equalTo(0.8f)));
        assertThat(holder2.drawable.getStolenBottomSleepDepthFraction(), is(equalTo(0.4f)));

        final TimelineAdapter.EventViewHolder holder3 =
                createAndBindView(adapter, fakeParent,
                                  headerCount + 2);
        assertThat(holder3.drawable.getStolenTopSleepDepthFraction(), is(equalTo(0.8f)));
        assertThat(holder3.drawable.getSleepDepthFraction(), is(equalTo(0.4f)));
        assertThat(holder3.drawable.getStolenBottomSleepDepthFraction(), is(equalTo(0f)));
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

        assertThat(holder12Hour.drawable.getTimestamp().toString(), is(equalTo("12 AM")));
        assertThat(holder12Hour.dateText.getText().toString(), is(equalTo("12:30 AM")));

        adapter.setUse24Time(true);

        final TimelineAdapter.EventViewHolder holder24Hour =
                createAndBindView(adapter, fakeParent,
                                  headerCount);

        assertThat(holder24Hour.drawable.getTimestamp().toString(), is(equalTo("00:00")));
        assertThat(holder24Hour.dateText.getText().toString(), is(equalTo("00:30")));
    }

    //endregion
}
