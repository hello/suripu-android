package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.bluetooth.exceptions.SenseRequiredException;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SleepSoundsInteractorTest extends InjectionTestCase {
    @Inject
    SleepSoundsInteractor sleepSoundsInteractor;

    public static SenseDevice createSense(@NonNull final DateTime lastSeen) {
        return new SenseDevice(BaseDevice.State.NORMAL,
                               SenseDevice.Color.BLACK,
                               "Not real",
                               "0.0.0",
                               lastSeen,
                               null,
                               SenseDevice.HardwareVersion.SENSE);
    }

    @Test
    public void hasSensePaired() throws Exception {
        final ArrayList<SenseDevice> senseDeviceList = new ArrayList<>(1);
        senseDeviceList.add(createSense(DateTime.now()));
        final Devices devicesList = new Devices(senseDeviceList, new ArrayList<>(0));
        final Boolean result = Sync.wrap(sleepSoundsInteractor.hasSensePaired(devicesList)).last();
        assertThat(result, equalTo(true));

        senseDeviceList.clear();
        senseDeviceList.add(createSense(DateTime.now().minusMinutes(30)));
        final Boolean resultWithMissingSense = Sync.wrap(sleepSoundsInteractor.hasSensePaired(devicesList)).last();
        assertThat(resultWithMissingSense, equalTo(true));
    }

    @Test
    public void hasNoSensePaired() throws Exception {
        final ArrayList<SenseDevice> senseDeviceList = new ArrayList<>(0);
        final Devices devicesList = new Devices(senseDeviceList, new ArrayList<>(0));

        Sync.wrap(sleepSoundsInteractor.hasSensePaired(null)).assertThrows(SenseRequiredException.class);

        Sync.wrap(sleepSoundsInteractor.hasSensePaired(devicesList)).assertThrows(SenseRequiredException.class);
    }

    @Test
    public void hasNullState() throws Exception {
        assertThat(sleepSoundsInteractor.onSaveState(), equalTo(null));
    }
}