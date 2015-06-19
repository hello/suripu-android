package is.hello.sense.util;

import android.content.Context;

import org.junit.Test;

import is.hello.sense.R;
import is.hello.sense.bluetooth.errors.BluetoothError;
import is.hello.sense.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.sense.graph.SenseTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

public class ErrorsTests extends SenseTestCase {
    @Test
    public void getType() throws Exception {
        assertEquals("java.lang.Throwable", Errors.getType(new Throwable()));
        assertEquals("java.lang.RuntimeException", Errors.getType(new RuntimeException()));
        assertEquals("is.hello.sense.bluetooth.errors.BluetoothError", Errors.getType(new BluetoothError("test")));
        assertNull(Errors.getType(null));
    }

    @Test
    public void getContextInfo() throws Exception {
        assertNull(Errors.getContextInfo(new Throwable()));
        PeripheralBondAlterationError error = new PeripheralBondAlterationError(PeripheralBondAlterationError.REASON_REMOVED);
        assertEquals("REASON_REMOVED", Errors.getContextInfo(error));
    }

    @Test
    public void getDisplayMessage() throws Exception {
        Context context = getContext();

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
