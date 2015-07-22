package is.hello.sense.util.markup;

import android.graphics.Typeface;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.markup.text.MarkupString;
import is.hello.sense.util.markup.text.MarkupStyleSpan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MarkupGsonDeserializerTests extends SenseTestCase {
    private final Gson gson;

    public MarkupGsonDeserializerTests() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(new TypeToken<MarkupString>(){}.getType(), new MarkupDeserializer(new MarkupProcessor()))
                .create();
    }


    @Test
    public void deserialize() throws Exception {
        String json = "{\"message\": \"This **really** works\"}";
        TestObject testObject = gson.fromJson(json, TestObject.class);
        assertNotNull(testObject);

        MarkupString message = testObject.message;
        assertNotNull(message);
        assertEquals("This really works", message.toString());

        MarkupStyleSpan[] spans = message.getSpans(0, message.length(), MarkupStyleSpan.class);
        assertEquals(1, spans.length);
        assertEquals(Typeface.BOLD, spans[0].getStyle());
        assertEquals(5, message.getSpanStart(spans[0]));
        assertEquals(11, message.getSpanEnd(spans[0]));
    }


    public static class TestObject {
        @SerializedName("message")
        public MarkupString message;
    }
}
