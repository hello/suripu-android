package is.hello.sense.rating;

import android.database.sqlite.SQLiteException;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.junit.After;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.rating.LocalUsageTracker.Identifier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class LocalUsageTrackerTests extends InjectionTestCase {
    @Inject LocalUsageTracker localUsageTracker;

    @After
    public void tearDown() {
        localUsageTracker.reset();
    }

    @Test
    public void resetClearsAllData() throws SQLiteException{
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
    public void deleteOldUsageStats() throws SQLiteException{
        LocalUsageTracker timeTravelingTracker = spy(this.localUsageTracker);
        DateTime distantPast = DateTime.now().withTimeAtStartOfDay().minusDays(90);
        doReturn(distantPast)
                .when(timeTravelingTracker)
                .today();
        timeTravelingTracker.increment(Identifier.APP_LAUNCHED);

        Interval lotsOfDays = new Interval(distantPast, localUsageTracker.today());
        assertThat(localUsageTracker.usageWithin(Identifier.APP_LAUNCHED, lotsOfDays),
                   is(equalTo(1)));

        localUsageTracker.deleteOldUsageStats();

        assertThat(localUsageTracker.usageWithin(Identifier.APP_LAUNCHED, lotsOfDays),
                   is(equalTo(0)));
    }

    @Test
    public void incrementMultipleTimesInOneDay() throws SQLiteException{
        localUsageTracker.increment(Identifier.APP_LAUNCHED);
        localUsageTracker.increment(Identifier.APP_LAUNCHED);
        localUsageTracker.increment(Identifier.APP_LAUNCHED);
        Interval last7Days = new Interval(Days.SEVEN, localUsageTracker.today());
        assertThat(localUsageTracker.usageWithin(Identifier.APP_LAUNCHED, last7Days),
                   is(equalTo(3)));
    }

    @Test
    public void incrementAcrossDaysWithinInterval() throws SQLiteException{
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
    public void incrementAcrossDaysOutsideInterval() throws SQLiteException{
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

    @Test
    public void isUsageAcceptableForRatingPrompt() throws SQLiteException{
        LocalUsageTracker tracker = spy(this.localUsageTracker);

        DateTime today = localUsageTracker.today();
        Interval lastWeek = new Interval(Weeks.ONE, today);
        Interval lastMonth = new Interval(Months.ONE, today);
        Interval last60Days = new Interval(Days.days(60), today);


        // Everything is great
        doReturn(5)
                .when(tracker)
                .usageWithin(Identifier.APP_LAUNCHED, lastWeek);
        doReturn(0)
                .when(tracker)
                .usageWithin(Identifier.SYSTEM_ALERT_SHOWN, lastMonth);
        doReturn(11)
                .when(tracker)
                .usageWithin(Identifier.TIMELINE_SHOWN_WITH_DATA, lastMonth);
        doReturn(0)
                .when(tracker)
                .usageWithin(Identifier.SKIP_REVIEW_PROMPT, last60Days);

        assertThat(tracker.isUsageAcceptableForRatingPrompt(),
                   is(equalTo(true)));


        // Not enough launches
        doReturn(3)
                .when(tracker)
                .usageWithin(Identifier.APP_LAUNCHED, lastWeek);
        assertThat(tracker.isUsageAcceptableForRatingPrompt(),
                   is(equalTo(false)));


        // Not enough timelines shown
        doReturn(0)
                .when(tracker)
                .usageWithin(Identifier.APP_LAUNCHED, lastMonth);
        doReturn(9)
                .when(tracker)
                .usageWithin(Identifier.TIMELINE_SHOWN_WITH_DATA, lastMonth);
        assertThat(tracker.isUsageAcceptableForRatingPrompt(),
                   is(equalTo(false)));


        // Too many system alerts
        doReturn(0)
                .when(tracker)
                .usageWithin(Identifier.APP_LAUNCHED, lastMonth);
        doReturn(3)
                .when(tracker)
                .usageWithin(Identifier.SYSTEM_ALERT_SHOWN, lastWeek);
        assertThat(tracker.isUsageAcceptableForRatingPrompt(),
                   is(equalTo(false)));


        // Skipped prompt in the last 60 days
        doReturn(1)
                .when(tracker)
                .usageWithin(Identifier.SKIP_REVIEW_PROMPT, last60Days);
        doReturn(0)
                .when(tracker)
                .usageWithin(Identifier.SYSTEM_ALERT_SHOWN, lastWeek);
        assertThat(tracker.isUsageAcceptableForRatingPrompt(),
                   is(equalTo(false)));
    }
}
