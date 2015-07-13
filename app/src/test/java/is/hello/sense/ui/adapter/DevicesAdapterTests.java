package is.hello.sense.ui.adapter;

import android.view.View;
import android.widget.FrameLayout;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.util.LambdaVar;
import is.hello.sense.util.ListAdapterTesting;

import static org.junit.Assert.assertEquals;

public class DevicesAdapterTests extends InjectionTestCase {
    private final FrameLayout fakeParent = new FrameLayout(getContext());
    @Inject PreferencesPresenter preferences;
    private DevicesAdapter adapter;

    //region Lifecycle

    @Before
    public void setUp() {
        this.adapter = new DevicesAdapter(getContext(), preferences);
    }

    //endregion


    //region Rendering

    @Test
    public void onlyOneDevicePerType() throws Exception {
        Device sense1 = new Device.Builder(Device.Type.SENSE)
                .setLastUpdated(DateTime.now())
                .setColor(Device.Color.WHITE)
                .setDeviceId("1234")
                .build();

        Device sense2 = new Device.Builder(Device.Type.SENSE)
                .setLastUpdated(DateTime.now())
                .setColor(Device.Color.WHITE)
                .setDeviceId("3833")
                .build();

        Device pill1 = new Device.Builder(Device.Type.PILL)
                .setLastUpdated(DateTime.now())
                .setColor(Device.Color.RED)
                .setDeviceId("asdf")
                .build();

        Device pill2 = new Device.Builder(Device.Type.PILL)
                .setLastUpdated(DateTime.now())
                .setColor(Device.Color.BLUE)
                .setDeviceId("qwer")
                .build();

        adapter.bindDevices(Lists.newArrayList(sense1, sense2, pill1, pill2));

        assertEquals(2, adapter.getCount());
        assertEquals(sense1, adapter.getItem(0));
        assertEquals(pill1, adapter.getItem(1));
    }

    @Test
    public void senseNormalDisplay() throws Exception {
        Device sense = new Device.Builder(Device.Type.SENSE)
                .setState(Device.State.NORMAL)
                .setDeviceId("1234")
                .setFirmwareVersion("ffffff")
                .setLastUpdated(DateTime.now().minusHours(2))
                .setColor(Device.Color.WHITE)
                .build();

        adapter.bindDevices(Lists.newArrayList(sense));

        View view = adapter.getView(0, null, fakeParent);
        DevicesAdapter.DeviceViewHolder holder = ListAdapterTesting.getViewHolder(view);

        assertEquals("Sense", holder.title.getText().toString());
        assertEquals("2 hours ago", holder.lastSeen.getText().toString());
        assertEquals(getResources().getColor(R.color.text_dark), holder.lastSeen.getCurrentTextColor());
        assertEquals("Wi-Fi", holder.status1Label.getText().toString());
        assertEquals("Connected", holder.status1.getText().toString());
        assertEquals("Firmware", holder.status2Label.getText().toString());
        assertEquals("ffffff", holder.status2.getText().toString());
    }

    @Test
    public void senseMissingDisplay() throws Exception {
        Device sense = new Device.Builder(Device.Type.SENSE)
                .setState(Device.State.UNKNOWN)
                .setDeviceId("1234")
                .setFirmwareVersion("ffffff")
                .setLastUpdated(DateTime.now().minusDays(5))
                .setColor(Device.Color.WHITE)
                .build();

        adapter.bindDevices(Lists.newArrayList(sense));

        View view = adapter.getView(0, null, fakeParent);
        DevicesAdapter.DeviceViewHolder holder = ListAdapterTesting.getViewHolder(view);

        assertEquals("Sense", holder.title.getText().toString());
        assertEquals("5 days ago", holder.lastSeen.getText().toString());
        assertEquals(getResources().getColor(R.color.destructive_accent), holder.lastSeen.getCurrentTextColor());
        assertEquals("Wi-Fi", holder.status1Label.getText().toString());
        assertEquals("--", holder.status1.getText().toString());
        assertEquals("Firmware", holder.status2Label.getText().toString());
        assertEquals("ffffff", holder.status2.getText().toString());
    }

    @Test
    public void sleepPillNormalDisplay() throws Exception {
        Device sleepPill = new Device.Builder(Device.Type.PILL)
                .setState(Device.State.NORMAL)
                .setDeviceId("1234")
                .setFirmwareVersion("ffffff")
                .setLastUpdated(DateTime.now().minusHours(2))
                .setColor(Device.Color.BLUE)
                .build();

        adapter.bindDevices(Lists.newArrayList(sleepPill));

        View view = adapter.getView(1, null, fakeParent);
        DevicesAdapter.DeviceViewHolder holder = ListAdapterTesting.getViewHolder(view);

        assertEquals("Sleep Pill", holder.title.getText().toString());
        assertEquals("2 hours ago", holder.lastSeen.getText().toString());
        assertEquals("Battery Level", holder.status1Label.getText().toString());
        assertEquals("Good", holder.status1.getText().toString());
        assertEquals("Color", holder.status2Label.getText().toString());
        assertEquals("Blue", holder.status2.getText().toString());
    }

    @Test
    public void sleepPillLowBatteryDisplay() throws Exception {
        Device sleepPill = new Device.Builder(Device.Type.PILL)
                .setState(Device.State.LOW_BATTERY)
                .setDeviceId("1234")
                .setFirmwareVersion("ffffff")
                .setLastUpdated(DateTime.now().minusHours(2))
                .setColor(Device.Color.BLUE)
                .build();

        adapter.bindDevices(Lists.newArrayList(sleepPill));

        View view = adapter.getView(1, null, fakeParent);
        DevicesAdapter.DeviceViewHolder holder = ListAdapterTesting.getViewHolder(view);

        assertEquals("Sleep Pill", holder.title.getText().toString());
        assertEquals("2 hours ago", holder.lastSeen.getText().toString());
        assertEquals("Battery Level", holder.status1Label.getText().toString());
        assertEquals("Low", holder.status1.getText().toString());
        assertEquals("Color", holder.status2Label.getText().toString());
        assertEquals("Blue", holder.status2.getText().toString());
    }

    @Test
    public void sleepPillMissingDisplay() throws Exception {
        Device sleepPill = new Device.Builder(Device.Type.PILL)
                .setState(Device.State.UNKNOWN)
                .setDeviceId("1234")
                .setFirmwareVersion("ffffff")
                .setLastUpdated(DateTime.now().minusDays(5))
                .setColor(Device.Color.BLUE)
                .build();

        adapter.bindDevices(Lists.newArrayList(sleepPill));

        View view = adapter.getView(1, null, fakeParent);
        DevicesAdapter.DeviceViewHolder holder = ListAdapterTesting.getViewHolder(view);

        assertEquals("Sleep Pill", holder.title.getText().toString());
        assertEquals("5 days ago", holder.lastSeen.getText().toString());
        assertEquals("Battery Level", holder.status1Label.getText().toString());
        assertEquals("Unknown", holder.status1.getText().toString());
        assertEquals("Color", holder.status2Label.getText().toString());
        assertEquals("Blue", holder.status2.getText().toString());
    }

    @Test
    public void pairSenseDisplay() throws Exception {
        adapter.bindDevices(Collections.emptyList());

        LambdaVar<Device.Type> clickedType = LambdaVar.empty();
        adapter.setOnPairNewDeviceListener(clickedType::set);

        View view = adapter.getView(0, null, fakeParent);
        DevicesAdapter.PlaceholderViewHolder holder = ListAdapterTesting.getViewHolder(view);

        assertEquals("Sense", holder.title.getText().toString());
        assertEquals("Pair New Sense", holder.actionButton.getText().toString());

        holder.actionButton.performClick();

        assertEquals(Device.Type.SENSE, clickedType.get());
    }

    @Test
    public void pairPillDisplay() throws Exception {
        adapter.bindDevices(Collections.emptyList());

        LambdaVar<Device.Type> clickedType = LambdaVar.empty();
        adapter.setOnPairNewDeviceListener(clickedType::set);

        View view = adapter.getView(1, null, fakeParent);
        DevicesAdapter.PlaceholderViewHolder holder = ListAdapterTesting.getViewHolder(view);

        assertEquals("Sleep Pill", holder.title.getText().toString());
        assertEquals("Pair New Pill", holder.actionButton.getText().toString());

        holder.actionButton.performClick();

        assertEquals(Device.Type.PILL, clickedType.get());
    }

    //endregion
}
