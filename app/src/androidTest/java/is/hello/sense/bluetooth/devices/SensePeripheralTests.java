package is.hello.sense.bluetooth.devices;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.TestBluetoothStackBehavior;
import is.hello.sense.bluetooth.stacks.TestPeripheralBehavior;
import is.hello.sense.bluetooth.stacks.util.ScanCriteria;
import is.hello.sense.bluetooth.stacks.util.ScanResponse;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;

public class SensePeripheralTests extends InjectionTestCase {
    private static final String TEST_DEVICE_ID = "ca154ffa";

    @Inject TestBluetoothStackBehavior stackBehavior;
    @Inject BluetoothStack stack;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        stackBehavior.reset();
    }

    @SuppressWarnings("ConstantConditions")
    public void testDiscovery() throws Exception {
        Set<ScanResponse> scanResponse = new HashSet<>();
        scanResponse.add(new ScanResponse(ScanResponse.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT));

        TestPeripheralBehavior device1 = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
        device1.setScanResponse(scanResponse);
        stackBehavior.addPeripheralInRange(device1);

        TestPeripheralBehavior device2 = new TestPeripheralBehavior("Sense-Test2", "c2:18:4e:fb:b3:0a", -90);
        device1.setScanResponse(scanResponse);
        stackBehavior.addPeripheralInRange(device2);

        ScanCriteria scanCriteria = new ScanCriteria();
        SyncObserver<List<SensePeripheral>> peripherals = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, SensePeripheral.discover(stack, scanCriteria));
        peripherals.await();

        assertNull(peripherals.getError());
        assertEquals(2, peripherals.getSingle().size());
    }

    public void testRediscovery() throws Exception {
        Set<ScanResponse> scanResponse = new HashSet<>();
        scanResponse.add(new ScanResponse(ScanResponse.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS, SenseIdentifiers.ADVERTISEMENT_SERVICE_128_BIT));
        scanResponse.add(new ScanResponse(ScanResponse.TYPE_SERVICE_DATA, SenseIdentifiers.ADVERTISEMENT_SERVICE_16_BIT + TEST_DEVICE_ID));

        TestPeripheralBehavior device = new TestPeripheralBehavior("Sense-Test", "ca:15:4f:fa:b7:0b", -50);
        device.setScanResponse(scanResponse);
        stackBehavior.addPeripheralInRange(device);

        SyncObserver<SensePeripheral> peripherals = SyncObserver.subscribe(SyncObserver.WaitingFor.COMPLETED, SensePeripheral.rediscover(stack, TEST_DEVICE_ID));
        peripherals.await();

        assertNull(peripherals.getError());
        assertNotNull(peripherals.getSingle());
        assertEquals("Sense-Test", peripherals.getSingle().getName());
    }


    public void testPerformCommand() throws Exception {
        fail();
    }

    public void testPerformSimpleCommand() throws Exception {
        fail();
    }

    public void testWriteLargeCommand() throws Exception {
        fail();
    }
}
