package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;

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

public class DeviceIssuesPresenterTests extends InjectionTestCase {
    @Inject DeviceIssuesPresenter presenter;
    @Inject PreferencesPresenter preferences;

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
                               null);
    }

    @Test
    public void topIssueNoSense() throws Exception {
        final Devices devices = new Devices(new ArrayList<>(), new ArrayList<>());
        final DeviceIssuesPresenter.Issue issue = presenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.NO_SENSE_PAIRED)));
    }

    @Test
    public void topIssueMissingSense() throws Exception {
        final SenseDevice missingSense = new SenseDevice(BaseDevice.State.UNKNOWN,
                                                         SenseDevice.Color.BLACK,
                                                         null,
                                                         null,
                                                         new DateTime(0),
                                                         null);
        final Devices devices = new Devices(Lists.newArrayList(missingSense),
                                            new ArrayList<>());
        final DeviceIssuesPresenter.Issue issue = presenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.SENSE_MISSING)));
    }

    @Test
    public void topIssueNoPill() throws Exception {
        final Devices devices = new Devices(Lists.newArrayList(createOkSense()),
                                            new ArrayList<>());
        final DeviceIssuesPresenter.Issue issue = presenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.NO_SLEEP_PILL_PAIRED)));
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
        final DeviceIssuesPresenter.Issue issue = presenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.SLEEP_PILL_LOW_BATTERY)));
    }

    @Test
    public void topIssueMissingPill() throws Exception {
        final SleepPillDevice missingPill = new SleepPillDevice(BaseDevice.State.UNKNOWN,
                                                                SleepPillDevice.Color.BLUE,
                                                                null,
                                                                null,
                                                                new DateTime(0),
                                                                0);
        final Devices devices = new Devices(Lists.newArrayList(createOkSense()),
                                            Lists.newArrayList(missingPill));
        final DeviceIssuesPresenter.Issue issue = presenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.SLEEP_PILL_MISSING)));
    }

    @Test
    public void shouldReportLowBattery() {
        final DateTime fixedTime = new DateTime(2015, 12, 9, 12, 29);
        DateTimeUtils.setCurrentMillisFixed(fixedTime.getMillis());

        assertThat(presenter.shouldReportLowBattery(), is(true));
        presenter.updateSystemAlertLastShown();
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
        final DeviceIssuesPresenter.Issue issue1 = presenter.getTopIssue(devices);
        assertThat(issue1, is(equalTo(DeviceIssuesPresenter.Issue.SLEEP_PILL_LOW_BATTERY)));

        presenter.updateSystemAlertLastShown();
        final DeviceIssuesPresenter.Issue issue2 = presenter.getTopIssue(devices);
        assertThat(issue2, is(not(equalTo(DeviceIssuesPresenter.Issue.SLEEP_PILL_LOW_BATTERY))));

        DateTimeUtils.setCurrentMillisFixed(fixedTime.plusDays(1).getMillis());
        devices.senses.set(0, createOkSense());
        final DeviceIssuesPresenter.Issue issue3 = presenter.getTopIssue(devices);
        assertThat(issue3, is(equalTo(DeviceIssuesPresenter.Issue.SLEEP_PILL_LOW_BATTERY)));
    }
}
