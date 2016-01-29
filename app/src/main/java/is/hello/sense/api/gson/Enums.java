package is.hello.sense.api.gson;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class Enums {
    /**
     * Searches a given set of enum fields for one whose name matches a given string value,
     * returning a given unknown enum field if no match could be found.
     * <p />
     * Search is case-insensitive. For implementing {@link Enums.FromString}.
     */
    public static <T extends Enum<T>> T fromString(@Nullable String value, @NonNull T[] values, @NonNull T unknown) {
        if (!TextUtils.isEmpty(value)) {
            for (T possibleMatch : values) {
                if (possibleMatch.name().equalsIgnoreCase(value)) {
                    return possibleMatch;
                }
            }
        }

        return unknown;
    }
    public static <T extends Enum<T>> T fromHash(int hashCode, @NonNull T[] values, @NonNull T unknown) {
        for (T possibleMatch : values) {
            if (possibleMatch.name().hashCode() == hashCode) {
                return possibleMatch;
            }
        }

        return unknown;
    }

    /**
     * Marks an enum as being deserializable using {@link Serialization}.
     * <p />
     * Implementers of this interface must have a static method matching:
     * <code><pre>
     *     public static EnumType fromString(String value)
     * </pre></code>
     */
    public interface FromString {
    }

    public static class Serialization implements JsonDeserializer<Object>, JsonSerializer<Object> {
        @Override
        public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String string = json.getAsJsonPrimitive().getAsString();
            try {
                Class<?> enumClass = (Class<?>) typeOfT;
                Method fromString = enumClass.getDeclaredMethod("fromString", String.class);
                return fromString.invoke(null, string);
            } catch (NoSuchMethodException e) {
                throw new JsonParseException("Missing fromString method on enum implementing Enums.FromString", e);
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }

        @Override
        public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }
}
