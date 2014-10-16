package is.hello.sense.functional;

import junit.framework.TestCase;

public final class FunctionsTests extends TestCase {
    public void testIsNotNull() throws Exception {
        assertTrue(Functions.isNotNull("this is not null"));
        assertFalse(Functions.isNotNull(null));
    }
}
