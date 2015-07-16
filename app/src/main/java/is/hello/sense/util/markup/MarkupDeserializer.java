package is.hello.sense.util.markup;

import android.support.annotation.NonNull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import is.hello.sense.util.markup.text.MarkupString;

public class MarkupDeserializer implements JsonDeserializer<MarkupString> {
    private final MarkupProcessor processor;

    public MarkupDeserializer(@NonNull MarkupProcessor processor) {
        this.processor = processor;
    }

    @Override
    public MarkupString deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Markup value is not a string");
        }

        String markup = json.getAsJsonPrimitive().getAsString();
        return processor.render(markup);
    }
}
