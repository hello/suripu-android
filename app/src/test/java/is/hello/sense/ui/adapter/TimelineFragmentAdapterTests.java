package is.hello.sense.ui.adapter;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Parcelable;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.api.model.v2.TimelineBuilder;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.util.Constants;
import is.hello.sense.util.PagerAdapterTesting;
import is.hello.sense.util.markup.text.MarkupString;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TimelineFragmentAdapterTests extends SenseTestCase {
    private TimelineFragmentAdapter adapter;

    @Before
    public void setUp() {
        FragmentTransaction transaction = PagerAdapterTesting.createMockTransaction();
        FragmentManager fragmentManager = PagerAdapterTesting.createMockFragmentManager(transaction);
        this.adapter = spy(new TimelineFragmentAdapter(fragmentManager));
    }

    @Test
    public void saveState() {
        adapter.firstTimeline = false;

        Parcelable savedState = adapter.saveState();
        adapter.firstTimeline = true;
        adapter.restoreState(savedState, Bundle.class.getClassLoader());

        assertThat(adapter.firstTimeline, is(false));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void cachedTimeline() {
        LocalDate lastNight = LocalDate.now().minusDays(1);
        Timeline timeline = new TimelineBuilder()
                .setDate(lastNight)
                .setScore(0, ScoreCondition.UNAVAILABLE)
                .setMessage(new MarkupString("y so srs?!"))
                .build();

        adapter.setCachedTimeline(timeline);


        TimelineFragment fragment1 = (TimelineFragment) adapter.createFragment(adapter.getLastNight() - 1);
        assertThat(fragment1.getDate(), is(not(equalTo(lastNight))));
        assertThat(fragment1.getCachedTimeline(), is(nullValue()));

        assertThat(adapter.cachedTimeline, is(notNullValue()));


        TimelineFragment fragment2 = (TimelineFragment) adapter.createFragment(adapter.getLastNight());
        assertThat(fragment2.getDate(), is(equalTo(lastNight)));
        assertThat(fragment2.getCachedTimeline().getDate(), is(equalTo(lastNight)));

        assertThat(adapter.cachedTimeline, is(nullValue()));
    }

    @Test
    public void ensureLatestDateIsToday() {
        adapter.ensureLatestDateIsLastNight();
        verify(adapter, never()).setLatestDate(any(LocalDate.class));

        LocalDate thePast = LocalDate.now().minusDays(5);
        adapter.setLatestDate(thePast);
        adapter.ensureLatestDateIsLastNight();
        verify(adapter).setLatestDate(LocalDate.now());
    }

    @Test
    public void setLatestDate() {
        DataSetObserver observer = mock(DataSetObserver.class);
        doNothing()
                .when(observer)
                .onChanged();

        LocalDate today = LocalDate.now();
        int initialCount = Days.daysBetween(Constants.TIMELINE_EPOCH, today).getDays();
        assertThat(adapter.latestDate, is(equalTo(today)));
        assertThat(adapter.getCount(), is(equalTo(initialCount)));

        adapter.setLatestDate(today.plusDays(1));
        assertThat(adapter.getCount(), is(equalTo(initialCount + 1)));

        adapter.setLatestDate(Constants.TIMELINE_EPOCH.plusDays(5));
        assertThat(adapter.getCount(), is(equalTo(5)));
    }

    @Test
    public void getDatePosition() {
        int last = adapter.getCount() - 1;
        LocalDate lastNight = LocalDate.now().minusDays(1);
        assertThat(adapter.getDatePosition(lastNight), is(equalTo(last)));
        assertThat(adapter.getDatePosition(lastNight.minusDays(1)), is(equalTo(last - 1)));
        assertThat(adapter.getDatePosition(lastNight.minusDays(5)), is(equalTo(last - 5)));
        assertThat(adapter.getDatePosition(lastNight.minusDays(10)), is(equalTo(last - 10)));
    }

    @Test
    public void getLastNight() {
        assertThat(adapter.getLastNight(), is(equalTo(adapter.getCount() - 1)));
        verify(adapter).ensureLatestDateIsLastNight();
    }

    @Test
    public void getItemDate() {
        int last = adapter.getCount() - 1;
        LocalDate lastNight = LocalDate.now().minusDays(1);
        assertThat(adapter.getItemDate(last), is(equalTo(lastNight)));
        assertThat(adapter.getItemDate(last - 1), is(equalTo(lastNight.minusDays(1))));
        assertThat(adapter.getItemDate(last - 5), is(equalTo(lastNight.minusDays(5))));
        assertThat(adapter.getItemDate(last - 10), is(equalTo(lastNight.minusDays(10))));
    }
}
