package is.hello.sense.functional;

import junit.framework.TestCase;

import java.io.Closeable;
import java.io.FileNotFoundException;

public final class FunctionsTests extends TestCase {
    public void testSafeClose() throws Exception {
        Closeable goodClosable = () -> {};
        Closeable badClosable = () -> { throw new FileNotFoundException(); };
        assertTrue(Functions.safeClose(goodClosable));
        assertFalse(Functions.safeClose(badClosable));
        assertFalse(Functions.safeClose(null));
    }

    public void testCompareInts() throws Exception {
        assertEquals(0, Functions.compareInts(1, 1));
        assertEquals(-1, Functions.compareInts(0, 1));
        assertEquals(1, Functions.compareInts(1, 0));
    }

    public void testIsTrue() throws Exception {
        assertTrue(Functions.IS_TRUE.call(true));
        assertFalse(Functions.IS_TRUE.call(false));
    }

    public void testIsFalse() throws Exception {
        assertFalse(Functions.IS_FALSE.call(true));
        assertTrue(Functions.IS_FALSE.call(false));
    }

    public void testToVoid() throws Exception {
        assertNull(Functions.TO_VOID.call("test"));
        assertNull(Functions.TO_VOID.call(42));
    }
}
