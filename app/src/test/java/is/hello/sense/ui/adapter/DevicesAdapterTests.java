package is.hello.sense.ui.adapter;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.FrameLayout;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.PlaceholderDevice;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.LambdaVar;
import is.hello.sense.util.RecyclerAdapterTesting;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DevicesAdapterTests extends SenseTestCase {
    private Activity parent;
    private final FrameLayout fakeParent = new FrameLayout(getContext());
    private DevicesAdapter adapter;

    //region Lifecycle

    @Before
    public void setUp() {
        this.parent = Robolectric.setupActivity(Activity.class);
        this.adapter = new DevicesAdapter(parent);
    }

    public static SenseDevice createOkSense() {
        return new SenseDevice(BaseDevice.State.NORMAL,
                               SenseDevice.Color.BLACK,
                               "1234",
                               "ffffff",
                               DateTime.now().minusHours(2),
                               new SenseDevice.WiFiInfo("Mostly Radiation", 50, DateTime.now(), "GOOD"),
                               SenseDevice.HardwareVersion.SENSE);
    }

    public static SenseDevice createSenseWithUpgrade() {
        return new SenseDevice(BaseDevice.State.NORMAL,
                               SenseDevice.Color.BLACK,
                               "1234",
                               "ffffff",
                               DateTime.now().minusHours(2),
                               new SenseDevice.WiFiInfo("Mostly Radiation", 50, DateTime.now(), "GOOD"),
                               SenseDevice.HardwareVersion.SENSE_WITH_VOICE);
    }

    //endregion


    //region Rendering

    @Test
    public void senseNormalDisplay() throws Exception {
        final SenseDevice sense = DevicesAdapterTests.createOkSense();
        adapter.bindDevices(new Devices(Lists.newArrayList(sense), new ArrayList<>()));

        final DevicesAdapter.SenseViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                                                    fakeParent, adapter.getItemViewType(0), 0);

        assertThat(holder.title.getText().toString(), is(equalTo("Sense")));
        assertThat(holder.lastSeen.getText().toString(), is(equalTo("2 hours ago")));
        assertThat(holder.lastSeen.getCurrentTextColor(),
                   is(equalTo(getResources().getColor(R.color.text_dark))));
        assertThat(holder.status1Label.getText().toString(), is(equalTo("Wi-Fi")));
        assertThat(holder.status1.getText().toString(), is(equalTo("Mostly Radiation")));
        assertThat(holder.status2Label.getText().toString(), is(equalTo("Firmware")));
        assertThat(holder.status2.getText().toString(), is(equalTo("ffffff")));
    }

    @Test
    public void senseMissingDisplay() throws Exception {
        final SenseDevice sense = new SenseDevice(SenseDevice.State.UNKNOWN,
                                                  SenseDevice.Color.BLACK,
                                                  "1234",
                                                  "ffffff",
                                                  DateTime.now().minusDays(5),
                                                  null,
                                                  SenseDevice.HardwareVersion.SENSE);

        adapter.bindDevices(new Devices(Lists.newArrayList(sense), new ArrayList<>()));

        final DevicesAdapter.SenseViewHolder senseHolder = RecyclerAdapterTesting.createAndBindView(adapter,
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
    public void senseNoWiFiDisplay() throws Exception {
        final SenseDevice sense = new SenseDevice(SenseDevice.State.NORMAL,
                                                  SenseDevice.Color.BLACK,
                                                  "1234",
                                                  "ffffff",
                                                  DateTime.now().minusHours(2),
                                                  null,
                                                  SenseDevice.HardwareVersion.SENSE);
        adapter.bindDevices(new Devices(Lists.newArrayList(sense), new ArrayList<>()));

        final DevicesAdapter.SenseViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                                                                                               fakeParent, adapter.getItemViewType(0), 0);

        assertThat(holder.title.getText().toString(), is(equalTo("Sense")));
        assertThat(holder.lastSeen.getText().toString(), is(equalTo("2 hours ago")));
        assertThat(holder.lastSeen.getCurrentTextColor(),
                   is(equalTo(getResources().getColor(R.color.text_dark))));
        assertThat(holder.status1Label.getText().toString(), is(equalTo("Wi-Fi")));
        assertThat(holder.status1.getText().toString(), is(equalTo("--")));
        assertThat(holder.status2Label.getText().toString(), is(equalTo("Firmware")));
        assertThat(holder.status2.getText().toString(), is(equalTo("ffffff")));
    }

    @Test
    public void senseWithWiFiNameMissingDisplay() throws Exception {
        final SenseDevice sense = new SenseDevice(SenseDevice.State.NORMAL,
                                                  SenseDevice.Color.BLACK,
                                                  "1234",
                                                  "ffffff",
                                                  DateTime.now().minusHours(2),
                                                  new SenseDevice.WiFiInfo(null, 50, DateTime.now(), "GOOD"),
                                                  SenseDevice.HardwareVersion.SENSE);
        adapter.bindDevices(new Devices(Lists.newArrayList(sense), new ArrayList<>()));

        final DevicesAdapter.SenseViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
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
    public void sleepPillNormalDisplay() throws Exception {
        final SenseDevice sense = DevicesAdapterTests.createOkSense();

        final SleepPillDevice sleepPill = new SleepPillDevice(SleepPillDevice.State.NORMAL,
                                                              SleepPillDevice.Color.BLUE,
                                                              "1234",
                                                              "ffffff",
                                                              DateTime.now().minusHours(2),
                                                              0);

        adapter.bindDevices(new Devices(Lists.newArrayList(sense), Lists.newArrayList(sleepPill)));

        final DevicesAdapter.SleepPillViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
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
        final SenseDevice sense = DevicesAdapterTests.createOkSense();

        final SleepPillDevice sleepPill = new SleepPillDevice(SleepPillDevice.State.LOW_BATTERY,
                                                              SleepPillDevice.Color.BLUE,
                                                              "1234",
                                                              "ffffff",
                                                              DateTime.now().minusHours(2),
                                                              0);

        adapter.bindDevices(new Devices(Lists.newArrayList(sense), Lists.newArrayList(sleepPill)));

        final DevicesAdapter.SleepPillViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
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
        final SenseDevice sense = DevicesAdapterTests.createOkSense();

        final SleepPillDevice sleepPill = new SleepPillDevice(SleepPillDevice.State.UNKNOWN,
                                                              SleepPillDevice.Color.BLUE,
                                                              "1234",
                                                              "ffffff",
                                                              DateTime.now().minusDays(5),
                                                              0);

        adapter.bindDevices(new Devices(Lists.newArrayList(sense), Lists.newArrayList(sleepPill)));

        final DevicesAdapter.SleepPillViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
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
        adapter.bindDevices(new Devices(new ArrayList<>(), new ArrayList<>()));

        final LambdaVar<PlaceholderDevice.Type> clickedType = LambdaVar.empty();
        adapter.setOnDeviceInteractionListener(new DevicesAdapter.OnDeviceInteractionListener() {
            @Override
            public void onPairNewDevice(@NonNull PlaceholderDevice.Type type) {
                clickedType.set(type);
            }

            @Override
            public void onUpdateDevice(@NonNull BaseDevice device) {
                //do nothing
            }
        });

        final DevicesAdapter.PlaceholderViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, adapter.getItemViewType(0), 0);

        assertThat(holder.title.getText().toString(), is(equalTo("Sense")));
        assertThat(holder.actionButton.getText().toString(), is(equalTo("Pair New Sense")));
        assertThat(holder.actionButton.isEnabled(), is(true));

        // For SafeOnClickListener to function properly
        ShadowSystemClock.setCurrentTimeMillis(1000);
        holder.actionButton.performClick();

        assertThat(clickedType.get(), is(equalTo(PlaceholderDevice.Type.SENSE)));
    }

    @Test
    public void pairPillNoSense() throws Exception {
        adapter.bindDevices(new Devices(new ArrayList<>(), new ArrayList<>()));

        assertThat(adapter.getItemCount(), is(equalTo(1)));
    }

    @Test
    public void pairPillDisplayWithSense() throws Exception {
        final SenseDevice sense = new SenseDevice(SenseDevice.State.UNKNOWN,
                                                  SenseDevice.Color.UNKNOWN,
                                                  "1234",
                                                  "ffffff",
                                                  DateTime.now().minusDays(5),
                                                  null,
                                                  SenseDevice.HardwareVersion.SENSE);

        adapter.bindDevices(new Devices(Lists.newArrayList(sense), new ArrayList<>()));

        final LambdaVar<PlaceholderDevice.Type> clickedType = LambdaVar.empty();
        adapter.setOnDeviceInteractionListener(new DevicesAdapter.OnDeviceInteractionListener() {
            @Override
            public void onPairNewDevice(@NonNull PlaceholderDevice.Type type) {
                clickedType.set(type);
            }

            @Override
            public void onUpdateDevice(@NonNull BaseDevice device) {
                //do nothing
            }
        });

        final DevicesAdapter.PlaceholderViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                 fakeParent, adapter.getItemViewType(1), 1);

        assertThat(holder.title.getText().toString(), is(equalTo("Sleep Pill")));
        assertThat(holder.actionButton.getText().toString(), is(equalTo("Pair New Pill")));
        assertThat(holder.actionButton.isEnabled(), is(true));

        // For SafeOnClickListener to function properly
        ShadowSystemClock.setCurrentTimeMillis(1000);
        holder.actionButton.performClick();

        assertThat(clickedType.get(), is(equalTo(PlaceholderDevice.Type.SLEEP_PILL)));
    }

    @Test
    public void sleepPillUpdateButton() throws Exception {

        final SleepPillDevice sleepPill = new SleepPillDevice(SleepPillDevice.State.NORMAL,
                                                              SleepPillDevice.Color.BLUE,
                                                              "1234",
                                                              "ffffff",
                                                              DateTime.now().minusHours(2),
                                                              0);
        sleepPill.setShouldUpdateOverride(true);

        adapter.bindDevices(new Devices(new ArrayList<>(), Lists.newArrayList(sleepPill)));

        final LambdaVar<BaseDevice> clickedDevice = LambdaVar.empty();
        adapter.setOnDeviceInteractionListener(new DevicesAdapter.OnDeviceInteractionListener() {
            @Override
            public void onPairNewDevice(@NonNull PlaceholderDevice.Type type) {
                //do nothing
            }

            @Override
            public void onUpdateDevice(@NonNull BaseDevice device) {
                clickedDevice.set(device);
            }
        });

        final DevicesAdapter.SleepPillViewHolder holder = RecyclerAdapterTesting.createAndBindView(
                adapter, fakeParent, adapter.getItemViewType(1), 1);

        assertThat(holder.title.getText().toString(), is(equalTo("Sleep Pill")));
        assertThat(holder.actionButton.getText().toString(), is(equalTo(getString(R.string.action_update))));
        assertThat(holder.actionButton.isEnabled(), is(true));
        assertThat(holder.actionButton.getVisibility(), is(View.VISIBLE));
        // For SafeOnClickListener to function properly
        ShadowSystemClock.setCurrentTimeMillis(1000);
        holder.actionButton.performClick();
        //fails because getAdapterPosition() returns -1 instead of mocked position
        //assertThat(clickedDevice.get(), is(instanceOf(SleepPillDevice.class)));
    }

    @Test
    public void showSenseUpgradeButton() throws Exception {

        final SenseDevice senseDevice = createOkSense();

        adapter.bindDevices(new Devices(Lists.newArrayList(senseDevice), new ArrayList<>()));

        final LambdaVar<BaseDevice> clickedDevice = LambdaVar.empty();
        adapter.setOnDeviceInteractionListener(new DevicesAdapter.OnDeviceInteractionListener() {
            @Override
            public void onPairNewDevice(@NonNull PlaceholderDevice.Type type) {
                //do nothing
            }

            @Override
            public void onUpdateDevice(@NonNull BaseDevice device) {
                clickedDevice.set(device);
            }
        });

        final DevicesAdapter.SenseViewHolder holder = RecyclerAdapterTesting.createAndBindView(
                adapter, fakeParent, adapter.getItemViewType(0), 0);

        assertThat(holder.title.getText().toString(), is(equalTo(getString(R.string.device_hardware_version_sense))));
        assertThat(holder.actionButton.getText().toString(), is(equalTo(getString(R.string.action_upgrade_sense))));
        assertThat(holder.actionButton.isEnabled(), is(true));

        // For SafeOnClickListener to function properly
        ShadowSystemClock.setCurrentTimeMillis(1000);
        holder.actionButton.performClick();
        //fails because getAdapterPosition() returns -1 instead of mocked position
        //assertThat(clickedDevice.get(), is(instanceOf(SenseDevice.class)));
    }

    @Test
    public void hideSenseUpgradeButton() throws Exception {

        final SenseDevice senseDevice = createSenseWithUpgrade();

        adapter.bindDevices(new Devices(Lists.newArrayList(senseDevice), new ArrayList<>()));

        final DevicesAdapter.SenseViewHolder holder = RecyclerAdapterTesting.createAndBindView(
                adapter, fakeParent, adapter.getItemViewType(0), 0);

        assertThat(holder.title.getText().toString(), is(equalTo(getString(R.string.device_hardware_version_sense_with_voice))));
        assertThat(holder.actionButton.isEnabled(), is(false));
        assertThat(holder.actionButton.getVisibility(), is(View.GONE));
    }

    //endregion
}
