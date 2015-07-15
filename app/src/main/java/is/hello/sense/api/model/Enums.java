package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class Enums {
    public static <T extends Enum<T>> T fromString(@Nullable String value, T[] values, @NonNull T unknown) {
        if (!TextUtils.isEmpty(value)) {
            for (T possibleMatch : values) {
                if (possibleMatch.name().equalsIgnoreCase(value)) {
                    return possibleMatch;
                }
            }
        }

        return unknown;
    }

    public interface FromString {
    }

    public static class Deserializer implements JsonDeserializer<Object> {
        @Override
        public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String string = json.getAsJsonPrimitive().getAsString();

            try {
                Method fromString = ((Class<?>) typeOfT).getMethod("fromString", String.class);
                return fromString.invoke(null, string);
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }
    }
}
