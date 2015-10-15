package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;

import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DeviceIssuesPresenterTests extends SenseTestCase {
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
        final DeviceIssuesPresenter.Issue issue = DeviceIssuesPresenter.getTopIssue(devices);
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
        final Devices devices = new Devices(Lists.newArrayList(missingSense), new ArrayList<>());
        final DeviceIssuesPresenter.Issue issue = DeviceIssuesPresenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.SENSE_MISSING)));
    }

    @Test
    public void topIssueNoPill() throws Exception {
        final Devices devices = new Devices(Lists.newArrayList(createOkSense()), new ArrayList<>());
        final DeviceIssuesPresenter.Issue issue = DeviceIssuesPresenter.getTopIssue(devices);
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
        final Devices devices = new Devices(Lists.newArrayList(createOkSense()), Lists.newArrayList(lowBatteryPill));
        final DeviceIssuesPresenter.Issue issue = DeviceIssuesPresenter.getTopIssue(devices);
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
        final Devices devices = new Devices(Lists.newArrayList(createOkSense()), Lists.newArrayList(missingPill));
        final DeviceIssuesPresenter.Issue issue = DeviceIssuesPresenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.SLEEP_PILL_MISSING)));
    }
}
