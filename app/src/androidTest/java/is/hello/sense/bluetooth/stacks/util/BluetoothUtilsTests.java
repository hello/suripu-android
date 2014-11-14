package is.hello.sense.bluetooth.stacks.util;

import junit.framework.TestCase;

import java.util.Arrays;

import static is.hello.sense.AssertExtensions.assertThrows;

public class BluetoothUtilsTests extends TestCase {
    public void testBytesToString() throws Exception {
        byte[] emptyBytes = {};
        String emptyString = BluetoothUtils.bytesToString(emptyBytes);
        assertEquals("", emptyString);


        byte[] testBytes = { 0x12, 0x14, 0x0f, 0x12 };
        String testString = BluetoothUtils.bytesToString(testBytes);
        assertEquals("12140F12", testString);
    }

    public void testStringToBytes() throws Exception {
        byte[] emptyBytes = {};
        assertTrue(Arrays.equals(emptyBytes, BluetoothUtils.stringToBytes("")));

        byte[] testBytes = { 0x12, 0x14, 0x0f, 0x12 };
        assertTrue(Arrays.equals(testBytes, BluetoothUtils.stringToBytes("12140F12")));

        assertThrows(() -> BluetoothUtils.stringToBytes("121"));
    }
}
