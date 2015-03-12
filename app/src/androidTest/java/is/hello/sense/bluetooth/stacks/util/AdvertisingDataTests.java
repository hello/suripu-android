package is.hello.sense.bluetooth.stacks.util;

import junit.framework.TestCase;

import java.util.Collection;

public class AdvertisingDataTests extends TestCase {
    private static final byte[] TEST_PAYLOAD = {
            // Advertisement contents size
            (byte) 0x03,

            // Advertisement data type
            (byte) AdvertisingData.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS,

            // Begin data
            (byte) 0xE1,
            (byte) 0xFE,
    };

    @SuppressWarnings("ConstantConditions")
    public void testParse() throws Exception {
        AdvertisingData advertisingData = AdvertisingData.parse(TEST_PAYLOAD);
        assertFalse(advertisingData.isEmpty());

        Collection<byte[]> records = advertisingData.getRecordsForType(AdvertisingData.TYPE_LIST_OF_16_BIT_SERVICE_CLASS_UUIDS);
        assertNotNull(records);
        assertFalse(records.isEmpty());
        assertEquals(Bytes.toString(records.iterator().next()), "E1FE");
    }
}
