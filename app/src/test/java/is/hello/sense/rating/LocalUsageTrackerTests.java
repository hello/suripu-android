package is.hello.sense.rating;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.rating.LocalUsageTracker.Identifier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class LocalUsageTrackerTests extends SenseTestCase {
    private LocalUsageTracker localUsageTracker = new LocalUsageTracker();

    @After
    public void tearDown() {
        localUsageTracker.reset();
    }

    @Test
    public void resetClearsAllData() {
        LocalUsageTracker timeTravelingTracker = spy(this.localUsageTracker);
        doReturn(DateTime.now().withTimeAtStartOfDay().minusDays(3))
                .when(timeTravelingTracker)
                .today();
        timeTravelingTracker.increment(Identifier.APP_LAUNCHED);

        localUsageTracker.increment(Identifier.APP_LAUNCHED);
        Interval last7Days = new Interval(Days.SEVEN, localUsageTracker.today());
        assertThat(localUsageTracker.usageWithin(Identifier.APP_LAUNCHED, last7Days),
                   is(equalTo(2)));

        localUsageTracker.reset();

        assertThat(localUsageTracker.usageWithin(Identifier.APP_LAUNCHED, last7Days),
                   is(equalTo(0)));
    }

    @Test
    public void collectClearsOldData() {
        LocalUsageTracker timeTravelingTracker = spy(this.localUsageTracker);
        Days distantPast = LocalUsageTracker.OLDEST_DAY.minus(10);
        doReturn(DateTime.now().withTimeAtStartOfDay().minus(distantPast))
                .when(timeTravelingTracker)
                .today();
        timeTravelingTracker.increment(Identifier.APP_LAUNCHED);

        Interval lotsOfDays = new Interval(distantPast, localUsageTracker.today());
        assertThat(localUsageTracker.usageWithin(Identifier.APP_LAUNCHED, lotsOfDays),
                   is(equalTo(1)));

        localUsageTracker.collect();

        assertThat(localUsageTracker.usageWithin(Identifier.APP_LAUNCHED, lotsOfDays),
                   is(equalTo(0)));
    }

    @Test
    public void incrementMultipleTimesInOneDay() {
        localUsageTracker.increment(Identifier.APP_LAUNCHED);
        localUsageTracker.increment(Identifier.APP_LAUNCHED);
        localUsageTracker.increment(Identifier.APP_LAUNCHED);
        Interval last7Days = new Interval(Days.SEVEN, localUsageTracker.today());
        assertThat(localUsageTracker.usageWithin(Identifier.APP_LAUNCHED, last7Days),
                   is(equalTo(3)));
    }

    @Test
    public void incrementAcrossDaysWithinInterval() {
        LocalUsageTracker timeTravelingTracker = spy(this.localUsageTracker);
        doReturn(DateTime.now().withTimeAtStartOfDay().minusDays(3))
                .when(timeTravelingTracker)
                .today();
        timeTravelingTracker.increment(Identifier.APP_LAUNCHED);

        localUsageTracker.increment(Identifier.APP_LAUNCHED);
        Interval last7Days = new Interval(Days.SEVEN, localUsageTracker.today());
        assertThat(localUsageTracker.usageWithin(Identifier.APP_LAUNCHED, last7Days),
                   is(equalTo(2)));
    }

    @Test
    public void incrementAcrossDaysOutsideInterval() {
        LocalUsageTracker timeTravelingTracker = spy(this.localUsageTracker);
        doReturn(DateTime.now().withTimeAtStartOfDay().minusDays(10))
                .when(timeTravelingTracker)
                .today();
        timeTravelingTracker.increment(Identifier.APP_LAUNCHED);

        localUsageTracker.increment(Identifier.APP_LAUNCHED);
        Interval last7Days = new Interval(Days.SEVEN, localUsageTracker.today());
        assertThat(localUsageTracker.usageWithin(Identifier.APP_LAUNCHED, last7Days),
                   is(equalTo(1)));
    }
}
