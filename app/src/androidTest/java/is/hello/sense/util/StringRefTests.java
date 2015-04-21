package is.hello.sense.util;

import android.content.Context;
import android.os.Bundle;
import android.test.InstrumentationTestCase;

import is.hello.sense.R;

public class StringRefTests extends InstrumentationTestCase {
    public void testResolveSimpleRes() throws Exception {
        Context context = getInstrumentation().getTargetContext();

        StringRef simpleMessage = StringRef.from(R.string.app_name);
        assertEquals("Sense (A)", simpleMessage.resolve(context));
    }

    public void testResolveFormatRes() throws Exception {
        Context context = getInstrumentation().getTargetContext();

        StringRef formatMessage = StringRef.from(R.string.app_version_fmt, 4, 2);
        assertEquals("4 2", formatMessage.resolve(context));
    }

    public void testResolveString() throws Exception {
        Context context = getInstrumentation().getTargetContext();

        StringRef stringMessage = StringRef.from("test");
        assertEquals("test", stringMessage.resolve(context));
    }

    public void testParceling() throws Exception {
        Context context = getInstrumentation().getTargetContext();

        Bundle holder = new Bundle();

        StringRef formatMessage = StringRef.from(R.string.app_version_fmt, 4, 2);
        holder.putParcelable("test", formatMessage);

        StringRef message = holder.getParcelable("test");
        assertEquals("4 2", message.resolve(context));
    }
}
