package is.hello.sense.util.markup.text;

import android.graphics.Typeface;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.Spanned;

import org.junit.Test;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import is.hello.sense.BuildConfig;
import is.hello.sense.SenseApplication;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.SenseTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Config(constants = BuildConfig.class, application = SenseApplication.class)
public class MarkupStringTests extends SenseTestCase {
    private final MarkupString testString;
    private final MarkupSpan[] testSpans;

    public MarkupStringTests() {
        MarkupString.Builder builder = new MarkupString.Builder("hello, world");

        this.testSpans = new MarkupSpan[] {
            new MarkupStyleSpan(Typeface.BOLD),
            new MarkupStyleSpan(Typeface.ITALIC),
        };

        builder.setSpan(testSpans[0], 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(testSpans[1], 7, 12, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        this.testString = builder.build();
    }


    @Test
    public void serialization() throws Exception {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        ObjectOutputStream outObjectStream = new ObjectOutputStream(outByteStream);
        byte[] bytes = null;
        try {
            outObjectStream.writeObject(testString);
            bytes = outByteStream.toByteArray();
        } finally {
            Functions.safeClose(outObjectStream);
            Functions.safeClose(outByteStream);
        }

        assertNotNull(bytes);

        ByteArrayInputStream inByteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream inObjectStream = new ObjectInputStream(inByteStream);
        try {
            MarkupString deserialized = (MarkupString) inObjectStream.readObject();
            assertNotNull(deserialized);
            assertEquals(testString, deserialized);
        } finally {
            Functions.safeClose(inObjectStream);
            Functions.safeClose(inByteStream);
        }
    }

    @Test
    public void parceling() throws Exception {
        Parcel out = Parcel.obtain();
        byte[] bytes = null;
        try {
            out.writeParcelable(testString, 0);
            bytes = out.marshall();
        } finally {
            out.recycle();
        }

        assertNotNull(bytes);


        Parcel in = Parcel.obtain();
        try {
            in.unmarshall(bytes, 0, bytes.length);
            in.setDataPosition(0);

            MarkupString unparceled = in.readParcelable(MarkupString.class.getClassLoader());
            assertNotNull(unparceled);
            assertEquals(testString, unparceled);
        } finally {
            in.recycle();
        }
    }


    @Test
    public void getSpans() throws Exception {
        MarkupStyleSpan[] allSpans = testString.getSpans(0, testString.length(), MarkupStyleSpan.class);
        assertNotNull(allSpans);
        assertEquals(2, allSpans.length);
        assertEquals(Typeface.BOLD, allSpans[0].getStyle());
        assertEquals(Typeface.ITALIC, allSpans[1].getStyle());

        ParcelableSpan[] noSpans = testString.getSpans(0, testString.length(), ParcelableSpan.class);
        assertNotNull(noSpans);
        assertEquals(0, noSpans.length);

        MarkupStyleSpan[] boldSpan = testString.getSpans(0, 3, MarkupStyleSpan.class);
        assertNotNull(boldSpan);
        assertEquals(1, boldSpan.length);
        assertEquals(Typeface.BOLD, boldSpan[0].getStyle());

        MarkupStyleSpan[] italicSpan = testString.getSpans(8, testString.length(), MarkupStyleSpan.class);
        assertNotNull(italicSpan);
        assertEquals(1, italicSpan.length);
        assertEquals(Typeface.ITALIC, italicSpan[0].getStyle());
    }

    @Test
    public void getSpanStart() throws Exception {
        assertEquals(0, testString.getSpanStart(testSpans[0]));
        assertEquals(7, testString.getSpanStart(testSpans[1]));
        assertEquals(-1, testString.getSpanStart("not a span"));
        assertEquals(-1, testString.getSpanStart(null));
    }

    @Test
    public void getSpanEnd() throws Exception {
        assertEquals(5, testString.getSpanEnd(testSpans[0]));
        assertEquals(12, testString.getSpanEnd(testSpans[1]));
        assertEquals(-1, testString.getSpanEnd("not a span"));
        assertEquals(-1, testString.getSpanEnd(null));
    }

    @Test
    public void getSpanFlags() throws Exception {
        assertEquals(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, testString.getSpanFlags(testSpans[0]));
        assertEquals(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, testString.getSpanFlags(testSpans[1]));
        assertEquals(-1, testString.getSpanFlags("not a span"));
        assertEquals(-1, testString.getSpanFlags(null));
    }

    @Test
    public void nextSpanTransition() throws Exception {
        assertEquals(5, testString.nextSpanTransition(0, testString.length(), null));
        assertEquals(5, testString.nextSpanTransition(0, testString.length(), MarkupStyleSpan.class));
        assertEquals(4, testString.nextSpanTransition(0, 4, MarkupStyleSpan.class));
        assertEquals(7, testString.nextSpanTransition(5, testString.length(), MarkupStyleSpan.class));
    }
}
