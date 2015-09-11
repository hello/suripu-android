package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.sense.api.model.Device;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DeviceIssuesPresenterTests extends SenseTestCase {
    public static Device createOkSense() {
        return new Device.Builder(Device.Type.SENSE)
                .setState(Device.State.NORMAL)
                .setLastUpdated(DateTime.now())
                .build();
    }

    @Test
    public void topIssueNoSense() throws Exception {
        final List<Device> devices = Collections.emptyList();
        final DeviceIssuesPresenter.Issue issue = DeviceIssuesPresenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.NO_SENSE_PAIRED)));
    }

    @Test
    public void topIssueMissingSense() throws Exception {
        final Device missingSense = new Device.Builder(Device.Type.SENSE)
                .setLastUpdated(new DateTime(0))
                .build();
        final ArrayList<Device> devices = Lists.newArrayList(missingSense);
        final DeviceIssuesPresenter.Issue issue = DeviceIssuesPresenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.SENSE_MISSING)));
    }

    @Test
    public void topIssueNoPill() throws Exception {
        final ArrayList<Device> devices = Lists.newArrayList(createOkSense());
        final DeviceIssuesPresenter.Issue issue = DeviceIssuesPresenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.NO_SLEEP_PILL_PAIRED)));
    }

    @Test
    public void topIssueLowBatteryPill() throws Exception {
        final Device lowBatteryPill = new Device.Builder(Device.Type.PILL)
                .setLastUpdated(DateTime.now())
                .setState(Device.State.LOW_BATTERY)
                .build();
        final ArrayList<Device> devices = Lists.newArrayList(createOkSense(), lowBatteryPill);
        final DeviceIssuesPresenter.Issue issue = DeviceIssuesPresenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.SLEEP_PILL_LOW_BATTERY)));
    }

    @Test
    public void topIssueMissingPill() throws Exception {
        final Device lowBatteryPill = new Device.Builder(Device.Type.PILL)
                .setLastUpdated(new DateTime(0))
                .build();
        final ArrayList<Device> devices = Lists.newArrayList(createOkSense(), lowBatteryPill);
        final DeviceIssuesPresenter.Issue issue = DeviceIssuesPresenter.getTopIssue(devices);
        assertThat(issue, is(equalTo(DeviceIssuesPresenter.Issue.SLEEP_PILL_MISSING)));
    }
}
