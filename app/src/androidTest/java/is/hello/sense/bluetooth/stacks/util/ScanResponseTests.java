package is.hello.sense.bluetooth.stacks.util;

import junit.framework.TestCase;

import java.util.Set;

public class ScanResponseTests extends TestCase {
    private static final byte[] TEST_PAYLOAD = {
            // Advertisement payload size
            (byte) 0x03,

            // Advertisement data type
            (byte) ScanResponse.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS,

            // Begin data
            (byte) 0xE1,
            (byte) 0xFE,
    };

    public void testParse() throws Exception {
        Set<ScanResponse> response = ScanResponse.parse(TEST_PAYLOAD);
        assertEquals(1, response.size());

        ScanResponse first = response.iterator().next();
        assertEquals(first.type, ScanResponse.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS);
        assertEquals(BluetoothUtils.bytesToString(first.payload), "E1FE");
    }
}
