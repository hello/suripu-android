package is.hello.sense.bluetooth.devices;

import javax.inject.Inject;

import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.TestBluetoothStackBehavior;
import is.hello.sense.graph.InjectionTestCase;

public class SensePeripheralTests extends InjectionTestCase {
    @Inject TestBluetoothStackBehavior stackBehavior;
    @Inject BluetoothStack stack;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        stackBehavior.reset();
    }

    public void testDiscovery() throws Exception {
        fail();
    }

    public void testRediscovery() throws Exception {
        fail();
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
