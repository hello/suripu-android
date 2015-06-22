package is.hello.sense.functional;

import org.junit.Test;

import java.io.Closeable;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class FunctionsTests {
    @Test
    public void safeClose() throws Exception {
        Closeable goodClosable = () -> {};
        Closeable badClosable = () -> { throw new FileNotFoundException(); };
        assertTrue(Functions.safeClose(goodClosable));
        assertFalse(Functions.safeClose(badClosable));
        assertFalse(Functions.safeClose(null));
    }

    @Test
    public void compareInts() throws Exception {
        assertEquals(0, Functions.compareInts(1, 1));
        assertEquals(-1, Functions.compareInts(0, 1));
        assertEquals(1, Functions.compareInts(1, 0));
    }

    @Test
    public void isTrue() throws Exception {
        assertTrue(Functions.IS_TRUE.call(true));
        assertFalse(Functions.IS_TRUE.call(false));
    }

    @Test
    public void isFalse() throws Exception {
        assertFalse(Functions.IS_FALSE.call(true));
        assertTrue(Functions.IS_FALSE.call(false));
    }

    @Test
    public void toVoid() throws Exception {
        assertNull(Functions.TO_VOID.call("test"));
        assertNull(Functions.TO_VOID.call(42));
    }
}
