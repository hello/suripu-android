package is.hello.sense.ui.fragments.settings;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.robolectric.util.FragmentTestUtil;

import is.hello.sense.R;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.graph.SenseTestCase;

import static is.hello.sense.ui.fragments.settings.DeviceDetailsFragment.ARG_DEVICE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class PillDetailsFragmentTest extends SenseTestCase {

    @Test
    public void newInstance() throws Exception {
        final SleepPillDevice testPill = createMockPill();
        final PillDetailsFragment fragment = setUpWith(testPill);
        assertThat(fragment.getArguments().getSerializable(ARG_DEVICE), equalTo(testPill));
    }

    @Test
    public void shouldShowUpdateFirmwareAction() throws Exception {
        final SleepPillDevice testPill = createMockPill();
        doReturn(false).when(testPill).hasLowBattery();
        doReturn(true).when(testPill).shouldUpdate();

        final PillDetailsFragment fragment = setUpWith(testPill);
        verify(fragment).addDeviceAction(eq(R.drawable.icon_settings_update),
                                         eq(R.string.action_update_firmware),
                                         any());
    }

    @Test
    public void shouldShowReplaceBatteryAction() throws Exception {
        final SleepPillDevice testPill = createMockPill();
        doReturn(true).when(testPill).hasRemovableBattery();

        final PillDetailsFragment fragment = setUpWith(testPill);
        verify(fragment).addDeviceAction(eq(R.drawable.icon_settings_battery),
                                         eq(R.string.action_replace_battery),
                                         any());
    }

    @Test
    public void shouldShowLowBatteryAlert() throws Exception {
        final SleepPillDevice testPill = createMockPill();
        doReturn(true).when(testPill).hasLowBattery();

        final PillDetailsFragment fragment = setUpWith(testPill);
        verify(fragment).showTroubleshootingAlert(any());
    }

    @Test
    public void shouldDeviceMissingAlert() throws Exception {
        final SleepPillDevice testPill = createMockPill();
        doReturn(true).when(testPill).isMissing();

        final PillDetailsFragment fragment = setUpWith(testPill);
        verify(fragment).showTroubleshootingAlert(any());
    }

    private SleepPillDevice createMockPill() {
        return mock(SleepPillDevice.class);
    }

    private PillDetailsFragment setUpWith(@NonNull final SleepPillDevice device) {
        final PillDetailsFragment fragment = spy(PillDetailsFragment.newInstance(device));
        FragmentTestUtil.startFragment(fragment);
        return fragment;
    }

}