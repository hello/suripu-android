package is.hello.sense.util;

import android.content.Context;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import is.hello.sense.R;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class StringRefTests {
    @Test
    public void resolveSimpleRes() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        StringRef simpleMessage = StringRef.from(R.string.app_name);
        assertEquals("Sense (A)", simpleMessage.resolve(context));
    }

    @Test
    public void resolveFormatRes() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        StringRef formatMessage = StringRef.from(R.string.app_version_fmt, 4, 2);
        assertEquals("4 2", formatMessage.resolve(context));
    }

    @Test
    public void resolveString() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        StringRef stringMessage = StringRef.from("test");
        assertEquals("test", stringMessage.resolve(context));
    }

    @Test
    public void parceling() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        Bundle holder = new Bundle();

        StringRef formatMessage = StringRef.from(R.string.app_version_fmt, 4, 2);
        holder.putParcelable("test", formatMessage);

        StringRef message = holder.getParcelable("test");
        assertEquals("4 2", message.resolve(context));
    }
}
