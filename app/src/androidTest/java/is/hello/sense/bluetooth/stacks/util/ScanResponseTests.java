package is.hello.sense.bluetooth.stacks.util;

import junit.framework.TestCase;

import java.util.Set;

public class ScanResponseTests extends TestCase {
    private static final byte[] TEST_PAYLOAD = {
            // Advertisement contents size
            (byte) 0x03,

            // Advertisement data type
            (byte) AdvertisingData.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS,

            // Begin data
            (byte) 0xE1,
            (byte) 0xFE,
    };

    public void testParse() throws Exception {
        Set<AdvertisingData.Payload> response = AdvertisingData.parse(TEST_PAYLOAD);
        assertEquals(1, response.size());

        AdvertisingData.Payload first = response.iterator().next();
        assertEquals(first.type, AdvertisingData.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS);
        assertEquals(BluetoothUtils.bytesToString(first.contents), "E1FE");
    }
}
