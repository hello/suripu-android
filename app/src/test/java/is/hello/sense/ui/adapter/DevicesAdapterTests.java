package is.hello.sense.ui.adapter;

import android.widget.FrameLayout;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.Collections;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.util.LambdaVar;
import is.hello.sense.util.RecyclerAdapterTesting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        final Device sense1 = new Device.Builder(Device.Type.SENSE)
                .setLastUpdated(DateTime.now())
                .setColor(Device.Color.WHITE)
                .setDeviceId("1234")
                .build();

        final Device sense2 = new Device.Builder(Device.Type.SENSE)
                .setLastUpdated(DateTime.now())
                .setColor(Device.Color.WHITE)
                .setDeviceId("3833")
                .build();

        final Device pill1 = new Device.Builder(Device.Type.PILL)
                .setLastUpdated(DateTime.now())
                .setColor(Device.Color.RED)
                .setDeviceId("asdf")
                .build();

        final Device pill2 = new Device.Builder(Device.Type.PILL)
                .setLastUpdated(DateTime.now())
                .setColor(Device.Color.BLUE)
                .setDeviceId("qwer")
                .build();

        adapter.bindDevices(Lists.newArrayList(sense1, sense2, pill1, pill2));

        assertEquals(2, adapter.getItemCount());
        assertEquals(sense1, adapter.getItem(0));
        assertEquals(pill1, adapter.getItem(1));
    }

    @Test
    public void senseNormalDisplay() throws Exception {
        final Device sense = new Device.Builder(Device.Type.SENSE)
                .setState(Device.State.NORMAL)
                .setDeviceId("1234")
                .setFirmwareVersion("ffffff")
                .setLastUpdated(DateTime.now().minusHours(2))
                .setColor(Device.Color.WHITE)
                .build();

        adapter.bindDevices(Lists.newArrayList(sense));

        final DevicesAdapter.DeviceViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                                                                                                fakeParent, adapter.getItemViewType(0), 0);

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
        final Device sense = new Device.Builder(Device.Type.SENSE)
                .setState(Device.State.UNKNOWN)
                .setDeviceId("1234")
                .setFirmwareVersion("ffffff")
                .setLastUpdated(DateTime.now().minusDays(5))
                .setColor(Device.Color.WHITE)
                .build();

        adapter.bindDevices(Lists.newArrayList(sense));

        final DevicesAdapter.DeviceViewHolder senseHolder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, adapter.getItemViewType(0), 0);

        assertEquals("Sense", senseHolder.title.getText().toString());
        assertEquals("5 days ago", senseHolder.lastSeen.getText().toString());
        assertEquals(getResources().getColor(R.color.destructive_accent), senseHolder.lastSeen.getCurrentTextColor());
        assertEquals("Wi-Fi", senseHolder.status1Label.getText().toString());
        assertEquals("--", senseHolder.status1.getText().toString());
        assertEquals("Firmware", senseHolder.status2Label.getText().toString());
        assertEquals("ffffff", senseHolder.status2.getText().toString());
    }

    @Test
    public void sleepPillNormalDisplay() throws Exception {
        final Device sleepPill = new Device.Builder(Device.Type.PILL)
                .setState(Device.State.NORMAL)
                .setDeviceId("1234")
                .setFirmwareVersion("ffffff")
                .setLastUpdated(DateTime.now().minusHours(2))
                .setColor(Device.Color.BLUE)
                .build();

        adapter.bindDevices(Lists.newArrayList(sleepPill));

        final DevicesAdapter.DeviceViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, adapter.getItemViewType(1), 1);

        assertEquals("Sleep Pill", holder.title.getText().toString());
        assertEquals("2 hours ago", holder.lastSeen.getText().toString());
        assertEquals("Battery Level", holder.status1Label.getText().toString());
        assertEquals("Good", holder.status1.getText().toString());
        assertEquals("Color", holder.status2Label.getText().toString());
        assertEquals("Blue", holder.status2.getText().toString());
    }

    @Test
    public void sleepPillLowBatteryDisplay() throws Exception {
        final Device sleepPill = new Device.Builder(Device.Type.PILL)
                .setState(Device.State.LOW_BATTERY)
                .setDeviceId("1234")
                .setFirmwareVersion("ffffff")
                .setLastUpdated(DateTime.now().minusHours(2))
                .setColor(Device.Color.BLUE)
                .build();

        adapter.bindDevices(Lists.newArrayList(sleepPill));

        final DevicesAdapter.DeviceViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, adapter.getItemViewType(1), 1);

        assertEquals("Sleep Pill", holder.title.getText().toString());
        assertEquals("2 hours ago", holder.lastSeen.getText().toString());
        assertEquals("Battery Level", holder.status1Label.getText().toString());
        assertEquals("Low", holder.status1.getText().toString());
        assertEquals("Color", holder.status2Label.getText().toString());
        assertEquals("Blue", holder.status2.getText().toString());
    }

    @Test
    public void sleepPillMissingDisplay() throws Exception {
        final Device sleepPill = new Device.Builder(Device.Type.PILL)
                .setState(Device.State.UNKNOWN)
                .setDeviceId("1234")
                .setFirmwareVersion("ffffff")
                .setLastUpdated(DateTime.now().minusDays(5))
                .setColor(Device.Color.BLUE)
                .build();

        adapter.bindDevices(Lists.newArrayList(sleepPill));

        final DevicesAdapter.DeviceViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, adapter.getItemViewType(1), 1);

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

        final LambdaVar<Device.Type> clickedType = LambdaVar.empty();
        adapter.setOnPairNewDeviceListener(clickedType::set);

        final DevicesAdapter.PlaceholderViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, adapter.getItemViewType(0), 0);

        assertEquals("Sense", holder.title.getText().toString());
        assertEquals("Pair New Sense", holder.actionButton.getText().toString());
        assertTrue(holder.actionButton.isEnabled());

        // For SafeOnClickListener to function properly
        ShadowSystemClock.setCurrentTimeMillis(1000);
        holder.actionButton.performClick();

        assertEquals(Device.Type.SENSE, clickedType.get());
    }

    @Test
    public void pairPillDisplayNoSense() throws Exception {
        adapter.bindDevices(Collections.emptyList());

        final LambdaVar<Device.Type> clickedType = LambdaVar.empty();
        adapter.setOnPairNewDeviceListener(clickedType::set);

        final DevicesAdapter.PlaceholderViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, adapter.getItemViewType(1), 1);

        assertEquals("Sleep Pill", holder.title.getText().toString());
        assertEquals("Pair New Pill", holder.actionButton.getText().toString());
        assertFalse(holder.actionButton.isEnabled());
    }

    @Test
    public void pairPillDisplayWithSense() throws Exception {
        final Device sense = new Device.Builder(Device.Type.SENSE)
                .setState(Device.State.UNKNOWN)
                .setDeviceId("1234")
                .setFirmwareVersion("ffffff")
                .setLastUpdated(DateTime.now().minusDays(5))
                .setColor(Device.Color.WHITE)
                .build();

        adapter.bindDevices(Lists.newArrayList(sense));

        final LambdaVar<Device.Type> clickedType = LambdaVar.empty();
        adapter.setOnPairNewDeviceListener(clickedType::set);

        final DevicesAdapter.PlaceholderViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                 fakeParent, adapter.getItemViewType(1), 1);

        assertEquals("Sleep Pill", holder.title.getText().toString());
        assertEquals("Pair New Pill", holder.actionButton.getText().toString());
        assertTrue(holder.actionButton.isEnabled());

        // For SafeOnClickListener to function properly
        ShadowSystemClock.setCurrentTimeMillis(1000);
        holder.actionButton.performClick();

        assertEquals(Device.Type.PILL, clickedType.get());
    }

    //endregion
}
