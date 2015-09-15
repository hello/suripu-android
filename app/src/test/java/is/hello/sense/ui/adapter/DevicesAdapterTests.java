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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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

        assertThat(adapter.getItemCount(), is(equalTo(2)));
        assertThat(adapter.getItem(0), is(equalTo(sense1)));
        assertThat(adapter.getItem(1), is(equalTo(pill1)));
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

        assertThat(holder.title.getText().toString(), is(equalTo("Sense")));
        assertThat(holder.lastSeen.getText().toString(), is(equalTo("2 hours ago")));
        assertThat(holder.lastSeen.getCurrentTextColor(),
                   is(equalTo(getResources().getColor(R.color.text_dark))));
        assertThat(holder.status1Label.getText().toString(), is(equalTo("Wi-Fi")));
        assertThat(holder.status1.getText().toString(), is(equalTo("Connected")));
        assertThat(holder.status2Label.getText().toString(), is(equalTo("Firmware")));
        assertThat(holder.status2.getText().toString(), is(equalTo("ffffff")));
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

        assertThat(senseHolder.title.getText().toString(), is(equalTo("Sense")));
        assertThat(senseHolder.lastSeen.getText().toString(), is(equalTo("5 days ago")));
        assertThat(senseHolder.lastSeen.getCurrentTextColor(),
                   is(equalTo(getResources().getColor(R.color.destructive_accent))));
        assertThat(senseHolder.status1Label.getText().toString(), is(equalTo("Wi-Fi")));
        assertThat(senseHolder.status1.getText().toString(), is(equalTo("--")));
        assertThat(senseHolder.status2Label.getText().toString(), is(equalTo("Firmware")));
        assertThat(senseHolder.status2.getText().toString(), is(equalTo("ffffff")));
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

        assertThat(holder.title.getText().toString(), is(equalTo("Sleep Pill")));
        assertThat(holder.lastSeen.getText().toString(), is(equalTo("2 hours ago")));
        assertThat(holder.status1Label.getText().toString(), is(equalTo("Battery Level")));
        assertThat(holder.status1.getText().toString(), is(equalTo("Good")));
        assertThat(holder.status2Label.getText().toString(), is(equalTo("Color")));
        assertThat(holder.status2.getText().toString(), is(equalTo("Blue")));
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

        assertThat(holder.title.getText().toString(), is(equalTo("Sleep Pill")));
        assertThat(holder.lastSeen.getText().toString(), is(equalTo("2 hours ago")));
        assertThat(holder.status1Label.getText().toString(), is(equalTo("Battery Level")));
        assertThat(holder.status1.getText().toString(), is(equalTo("Low")));
        assertThat(holder.status2Label.getText().toString(), is(equalTo("Color")));
        assertThat(holder.status2.getText().toString(), is(equalTo("Blue")));
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

        assertThat(holder.title.getText().toString(), is(equalTo("Sleep Pill")));
        assertThat(holder.lastSeen.getText().toString(), is(equalTo("5 days ago")));
        assertThat(holder.status1Label.getText().toString(), is(equalTo("Battery Level")));
        assertThat(holder.status1.getText().toString(), is(equalTo("Unknown")));
        assertThat(holder.status2Label.getText().toString(), is(equalTo("Color")));
        assertThat(holder.status2.getText().toString(), is(equalTo("Blue")));
    }

    @Test
    public void pairSenseDisplay() throws Exception {
        adapter.bindDevices(Collections.emptyList());

        final LambdaVar<Device.Type> clickedType = LambdaVar.empty();
        adapter.setOnPairNewDeviceListener(clickedType::set);

        final DevicesAdapter.PlaceholderViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, adapter.getItemViewType(0), 0);

        assertThat(holder.title.getText().toString(), is(equalTo("Sense")));
        assertThat(holder.actionButton.getText().toString(), is(equalTo("Pair New Sense")));
        assertThat(holder.actionButton.isEnabled(), is(true));

        // For SafeOnClickListener to function properly
        ShadowSystemClock.setCurrentTimeMillis(1000);
        holder.actionButton.performClick();

        assertThat(clickedType.get(), is(equalTo(Device.Type.SENSE)));
    }

    @Test
    public void pairPillDisplayNoSense() throws Exception {
        adapter.bindDevices(Collections.emptyList());

        final LambdaVar<Device.Type> clickedType = LambdaVar.empty();
        adapter.setOnPairNewDeviceListener(clickedType::set);

        final DevicesAdapter.PlaceholderViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, adapter.getItemViewType(1), 1);

        assertThat("Sleep Pill", is(equalTo(holder.title.getText().toString())));
        assertThat("Pair New Pill", is(equalTo(holder.actionButton.getText().toString())));
        assertThat(holder.actionButton.isEnabled(), is(false));
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

        assertThat("Sleep Pill", is(equalTo(holder.title.getText().toString())));
        assertThat("Pair New Pill", is(equalTo(holder.actionButton.getText().toString())));
        assertThat(holder.actionButton.isEnabled(), is(true));

        // For SafeOnClickListener to function properly
        ShadowSystemClock.setCurrentTimeMillis(1000);
        holder.actionButton.performClick();

        assertThat(clickedType.get(), is(equalTo(Device.Type.PILL)));
    }

    //endregion
}
