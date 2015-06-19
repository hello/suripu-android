package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Collections;

import is.hello.sense.api.model.Device;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;

import static junit.framework.Assert.assertEquals;

public class DevicesPresenterTests extends SenseTestCase {
    public static Device createOkSense() {
        return new Device.Builder(Device.Type.SENSE)
                .setState(Device.State.NORMAL)
                .setLastUpdated(DateTime.now())
                .build();
    }

    @Test
    public void topIssueNoSense() throws Exception {
        DevicesPresenter.Issue issue = DevicesPresenter.topIssue(Collections.emptyList());
        assertEquals(DevicesPresenter.Issue.NO_SENSE_PAIRED, issue);
    }

    @Test
    public void topIssueMissingSense() throws Exception {
        Device missingSense = new Device.Builder(Device.Type.SENSE)
                .setLastUpdated(new DateTime(0))
                .build();
        DevicesPresenter.Issue issue = DevicesPresenter.topIssue(Lists.newArrayList(missingSense));
        assertEquals(DevicesPresenter.Issue.SENSE_MISSING, issue);
    }

    @Test
    public void topIssueNoPill() throws Exception {
        DevicesPresenter.Issue issue = DevicesPresenter.topIssue(Lists.newArrayList(createOkSense()));
        assertEquals(DevicesPresenter.Issue.NO_SLEEP_PILL_PAIRED, issue);
    }

    @Test
    public void topIssueLowBatteryPill() throws Exception {
        Device lowBatteryPill = new Device.Builder(Device.Type.PILL)
                .setLastUpdated(DateTime.now())
                .setState(Device.State.LOW_BATTERY)
                .build();
        DevicesPresenter.Issue issue = DevicesPresenter.topIssue(Lists.newArrayList(createOkSense(), lowBatteryPill));
        assertEquals(DevicesPresenter.Issue.SLEEP_PILL_LOW_BATTERY, issue);
    }

    @Test
    public void topIssueMissingPill() throws Exception {
        Device lowBatteryPill = new Device.Builder(Device.Type.PILL)
                .setLastUpdated(new DateTime(0))
                .build();
        DevicesPresenter.Issue issue = DevicesPresenter.topIssue(Lists.newArrayList(createOkSense(), lowBatteryPill));
        assertEquals(DevicesPresenter.Issue.SLEEP_PILL_MISSING, issue);
    }
}
