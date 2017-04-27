package is.hello.sense.interactors;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class DeviceIssuesInteractorTests extends InjectionTestCase {
    @Inject
    DeviceIssuesInteractor presenter;
    @Inject
    PreferencesInteractor preferences;

    @After
    public void tearDown() {
        preferences.clear();
        DateTimeUtils.setCurrentMillisSystem();
    }

    public static SenseDevice createOkSense() {
        return new SenseDevice(BaseDevice.State.NORMAL,
                               SenseDevice.Color.BLACK,
                               "Not real",
                               "0.0.0",
                               DateTime.now(),
                               null,
                               SenseDevice.HardwareVersion.SENSE);
    }

    @Test
    public void topIssueLowBatteryPill() throws Exception {
        final SleepPillDevice lowBatteryPill = new SleepPillDevice(BaseDevice.State.LOW_BATTERY,
                                                                   SleepPillDevice.Color.BLUE,
                                                                   null,
                                                                   null,
                                                                   DateTime.now(),
                                                                   20);
        final Devices devices = new Devices(Lists.newArrayList(createOkSense()),
                                            Lists.newArrayList(lowBatteryPill));
        final DeviceIssuesInteractor.Issue issue = presenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesInteractor.Issue.SLEEP_PILL_LOW_BATTERY)));
    }

    @Test
    public void shouldReportLowBattery() {
        final DateTime fixedTime = new DateTime(2015, 12, 9, 12, 29);
        DateTimeUtils.setCurrentMillisFixed(fixedTime.getMillis());

        assertThat(presenter.shouldReportLowBattery(), is(true));
        presenter.updateLastShown(DeviceIssuesInteractor.Issue.SLEEP_PILL_LOW_BATTERY);

        assertThat(presenter.shouldReportLowBattery(), is(false));

        DateTimeUtils.setCurrentMillisFixed(fixedTime.plusDays(1).getMillis());
        assertThat(presenter.shouldReportLowBattery(), is(true));
    }

    @Test
    public void lowBatteryTimeRestrictions() throws Exception {
        final DateTime fixedTime = new DateTime(2015, 12, 9, 12, 29);
        DateTimeUtils.setCurrentMillisFixed(fixedTime.getMillis());

        final SleepPillDevice lowBatteryPill = new SleepPillDevice(BaseDevice.State.LOW_BATTERY,
                                                                   SleepPillDevice.Color.BLUE,
                                                                   null,
                                                                   null,
                                                                   DateTime.now(),
                                                                   20);
        final Devices devices = new Devices(Lists.newArrayList(createOkSense()),
                                            Lists.newArrayList(lowBatteryPill));
        final DeviceIssuesInteractor.Issue issue1 = presenter.getTopIssue(devices);
        assertThat(issue1, is(equalTo(DeviceIssuesInteractor.Issue.SLEEP_PILL_LOW_BATTERY)));

        presenter.updateLastShown(DeviceIssuesInteractor.Issue.SLEEP_PILL_LOW_BATTERY);
        final DeviceIssuesInteractor.Issue issue2 = presenter.getTopIssue(devices);
        assertThat(issue2, is(not(equalTo(DeviceIssuesInteractor.Issue.SLEEP_PILL_LOW_BATTERY))));

        DateTimeUtils.setCurrentMillisFixed(fixedTime.plusDays(1).getMillis());
        devices.senses.set(0, createOkSense());
        final DeviceIssuesInteractor.Issue issue3 = presenter.getTopIssue(devices);
        assertThat(issue3, is(equalTo(DeviceIssuesInteractor.Issue.SLEEP_PILL_LOW_BATTERY)));
    }
}
