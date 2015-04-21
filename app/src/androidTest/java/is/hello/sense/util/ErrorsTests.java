package is.hello.sense.util;

import android.content.Context;
import android.test.InstrumentationTestCase;

import is.hello.sense.R;
import is.hello.sense.bluetooth.errors.BluetoothError;
import is.hello.sense.bluetooth.errors.PeripheralBondAlterationError;

public class ErrorsTests extends InstrumentationTestCase {
    public void testGetType() throws Exception {
        assertEquals("java.lang.Throwable", Errors.getType(new Throwable()));
        assertEquals("java.lang.RuntimeException", Errors.getType(new RuntimeException()));
        assertEquals("is.hello.sense.bluetooth.errors.BluetoothError", Errors.getType(new BluetoothError("test")));
        assertNull(Errors.getType(null));
    }

    public void testGetContextInfo() throws Exception {
        assertNull(Errors.getContextInfo(new Throwable()));
        PeripheralBondAlterationError error = new PeripheralBondAlterationError(PeripheralBondAlterationError.REASON_REMOVED);
        assertEquals("REASON_REMOVED", Errors.getContextInfo(error));
    }

    public void testGetDisplayMessage() throws Exception {
        Context context = getInstrumentation().getTargetContext();

        assertNull(Errors.getDisplayMessage(null));

        StringRef throwableMessage = Errors.getDisplayMessage(new Throwable("test"));
        assertNotNull(throwableMessage);
        assertEquals("test", throwableMessage.resolve(context));

        PeripheralBondAlterationError error = new PeripheralBondAlterationError(PeripheralBondAlterationError.REASON_REMOTE_DEVICE_DOWN);
        StringRef errorMessage = Errors.getDisplayMessage(error);
        assertNotNull(errorMessage);
        assertEquals(context.getString(R.string.error_bluetooth_out_of_range), errorMessage.resolve(context));
    }
}
