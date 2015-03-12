package is.hello.sense.bluetooth.stacks.util;

import junit.framework.TestCase;

import java.util.Arrays;

import static is.hello.sense.AssertExtensions.assertThrows;

public class BytesTests extends TestCase {
    public void testToString() throws Exception {
        byte[] emptyBytes = {};
        String emptyString = Bytes.toString(emptyBytes);
        assertEquals("", emptyString);


        byte[] testBytes = { 0x12, 0x14, 0x0f, 0x12 };
        String testString = Bytes.toString(testBytes);
        assertEquals("12140F12", testString);
    }

    public void testFromString() throws Exception {
        byte[] emptyBytes = {};
        assertTrue(Arrays.equals(emptyBytes, Bytes.fromString("")));

        byte[] testBytes = { 0x12, 0x14, 0x0f, 0x12 };
        assertTrue(Arrays.equals(testBytes, Bytes.fromString("12140F12")));

        assertThrows(() -> Bytes.fromString("121"));
    }

    public void testStartWith() throws Exception {
        byte[] sequence = { 0x1, 0x2, 0x3 };
        assertTrue(Bytes.startWith(sequence, new byte[] { 0x1, 0x2 }));
        assertFalse(Bytes.startWith(sequence, new byte[] { 0x2, 0x3 }));
    }

    public void testContains() throws Exception {
        byte[] sequence = { 0x1, 0x2, 0x3 };
        assertTrue(Bytes.contains(sequence, (byte) 0x1));
        assertTrue(Bytes.contains(sequence, (byte) 0x2));
        assertTrue(Bytes.contains(sequence, (byte) 0x3));
        assertFalse(Bytes.contains(sequence, (byte) 0x4));
    }
}
