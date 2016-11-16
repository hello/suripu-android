package is.hello.sense.bluetooth;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.Bytes;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.AdvertisingDataBuilder;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class PillPeripheralTest extends SenseTestCase{
    @Test
    public void isPillOneFive() throws Exception {
        final AdvertisingDataBuilder builder = new AdvertisingDataBuilder();
        builder.add(AdvertisingData.TYPE_MANUFACTURER_SPECIFIC_DATA,
                    Bytes.toString(PillPeripheral.MANUFACTURE_DATA_PILL_ONE_FIVE_PREFIX));

        assertTrue(PillPeripheral.isPillOneFive(builder.build()));
    }

    @Test
    public void getClosestByRssi() throws Exception {
        final GattPeripheral weakerRssiPeripheral = mock(GattPeripheral.class);
        doReturn(-81)
                .when(weakerRssiPeripheral)
                .getScanTimeRssi();

        final GattPeripheral strongerRssiPeripheral = mock(GattPeripheral.class);
        doReturn(-80)
                .when(strongerRssiPeripheral)
                .getScanTimeRssi();

        final List<GattPeripheral> unSortedPeripherals = new ArrayList<>(2);
        unSortedPeripherals.add(weakerRssiPeripheral);
        unSortedPeripherals.add(strongerRssiPeripheral);
        assertEquals(unSortedPeripherals.get(0), weakerRssiPeripheral);

        final PillPeripheral returnedPillPeripheral = PillPeripheral.getClosestByRssi(unSortedPeripherals);
        assertNotNull(returnedPillPeripheral);
        assertEquals(strongerRssiPeripheral.getScanTimeRssi(), returnedPillPeripheral.getScanTimeRssi());
    }

}