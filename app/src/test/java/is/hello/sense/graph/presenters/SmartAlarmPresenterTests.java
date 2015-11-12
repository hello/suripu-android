package is.hello.sense.graph.presenters;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.Alarm;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ConstantConditions")
public class SmartAlarmPresenterTests extends InjectionTestCase {
    @Inject SmartAlarmPresenter presenter;

    @Test
    public void update() throws Exception {
        final ArrayList<Alarm> smartAlarms = Sync.wrapAfter(presenter::update, presenter.alarms)
                                                 .last();
        assertThat(smartAlarms.size(), is(equalTo(1)));
    }

    @Test
    public void validateAlarms() throws Exception {
        final Alarm sunday = new Alarm();
        sunday.addDayOfWeek(DateTimeConstants.SUNDAY);
        sunday.setSmart(true);

        final Alarm monday = new Alarm();
        monday.addDayOfWeek(DateTimeConstants.MONDAY);
        monday.setSmart(true);

        assertThat(presenter.validateAlarms(Lists.newArrayList(sunday, monday)), is(true));
        assertThat(presenter.validateAlarms(Lists.newArrayList(sunday, sunday)), is(false));
        assertThat(presenter.validateAlarms(Lists.newArrayList(monday, monday)), is(false));

        final Alarm stupidSunday = new Alarm();
        stupidSunday.setSmart(false);
        stupidSunday.addDayOfWeek(DateTimeConstants.SUNDAY);
        assertThat(presenter.validateAlarms(Lists.newArrayList(sunday, stupidSunday)), is(true));

        final Alarm disabledSunday = new Alarm();
        disabledSunday.addDayOfWeek(DateTimeConstants.SUNDAY);
        disabledSunday.setSmart(true);
        disabledSunday.setEnabled(false);
        assertThat(presenter.validateAlarms(Lists.newArrayList(sunday, disabledSunday)), is(true));
    }

    @Test
    public void save() throws Exception {
        final ArrayList<Alarm> goodAlarms = new ArrayList<>();
        Sync.wrap(presenter.save(goodAlarms))
            .assertThat(is(notNullValue()));

        final Alarm sunday = new Alarm();
        sunday.addDayOfWeek(DateTimeConstants.SUNDAY);
        sunday.setSmart(true);
        final ArrayList<Alarm> badAlarms = Lists.newArrayList(sunday, sunday);

        Sync.wrap(presenter.save(badAlarms))
            .assertThrows(SmartAlarmPresenter.DayOverlapError.class);
    }

    @Test
    public void isTooSoon() throws Exception {
        final Alarm alarm = new Alarm();

        final LocalTime midHour = new LocalTime(9, 30, 0);
        final LocalTime tooSoonMidHour = midHour.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES / 2);
        final LocalTime pastCutOffMidHour = midHour.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES * 2);
        final LocalTime beforeCutOffMidHour = midHour.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES * 2);

        alarm.setTime(midHour);
        assertThat(presenter.isAlarmTooSoon(midHour, alarm), is(true));

        alarm.setTime(tooSoonMidHour);
        assertThat(presenter.isAlarmTooSoon(midHour, alarm), is(true));

        alarm.setTime(pastCutOffMidHour);
        assertThat(presenter.isAlarmTooSoon(midHour, alarm), is(false));

        alarm.setTime(beforeCutOffMidHour);
        assertThat(presenter.isAlarmTooSoon(midHour, alarm), is(false));


        final LocalTime hourBoundary = new LocalTime(9, 59, 0);
        final LocalTime tooSoonBoundary = hourBoundary.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES / 2);
        final LocalTime pastCutOffBoundary = hourBoundary.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES * 2);
        final LocalTime beforeCutOffBoundary = hourBoundary.plusMinutes(Alarm.FUTURE_CUT_OFF_MINUTES * 2);

        alarm.setTime(hourBoundary);
        assertThat(presenter.isAlarmTooSoon(hourBoundary, alarm), is(true));

        alarm.setTime(tooSoonBoundary);
        assertThat(presenter.isAlarmTooSoon(hourBoundary, alarm), is(true));

        alarm.setTime(pastCutOffBoundary);
        assertThat(presenter.isAlarmTooSoon(hourBoundary, alarm), is(false));

        alarm.setTime(beforeCutOffBoundary);
        assertThat(presenter.isAlarmTooSoon(hourBoundary, alarm), is(false));
    }
}
