package is.hello.sense.util.markup;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

import is.hello.sense.util.markup.text.MarkupString;

public class MarkupDeserializer extends StdDeserializer<MarkupString> {
    private final MarkupProcessor processor;

    public MarkupDeserializer(@NonNull MarkupProcessor processor) {
        super(MarkupString.class);

        this.processor = processor;
    }

    @Override
    public MarkupString deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
            throw context.mappingException("expected string");
        }

        String markup = parser.getValueAsString();
        return processor.render(markup);
    }
}
