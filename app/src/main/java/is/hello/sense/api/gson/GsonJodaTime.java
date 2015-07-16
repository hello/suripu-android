package is.hello.sense.api.gson;

import android.support.annotation.NonNull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;

import java.lang.reflect.Type;

public class GsonJodaTime {
    public static class DateTimeSerialization implements JsonDeserializer<DateTime>, JsonSerializer<DateTime> {
        private final DateTimeFormatter formatter;
        private final SerializeAs serializeAs;

        public DateTimeSerialization(@NonNull DateTimeFormatter formatter,
                                     @NonNull SerializeAs serializeAs) {
            this.formatter = formatter;
            this.serializeAs = serializeAs;
        }

        @Override
        public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                long timestamp = primitive.getAsLong();
                return new DateTime(timestamp, DateTimeZone.UTC);
            } else if (primitive.isString()) {
                return formatter.parseDateTime(primitive.getAsString());
            } else {
                throw new JsonParseException("Unexpected primitive " + primitive);
            }
        }

        @Override
        public JsonElement serialize(DateTime src, Type typeOfSrc, JsonSerializationContext context) {
            switch (serializeAs) {
                case STRING:
                    return new JsonPrimitive(src.toString(formatter));
                case NUMBER:
                    return new JsonPrimitive(src.getMillis());
                default:
                    throw new IllegalArgumentException("Unknown serialization format '" + serializeAs + "'");
            }
        }
    }

    public static class LocalDateSerialization implements JsonDeserializer<LocalDate>, JsonSerializer<LocalDate> {
        private final DateTimeFormatter formatter;

        public LocalDateSerialization(@NonNull DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                long timestamp = primitive.getAsLong();
                return new LocalDate(timestamp);
            } else if (primitive.isString()) {
                return formatter.parseLocalDate(primitive.getAsString());
            } else {
                throw new JsonParseException("Unexpected primitive " + primitive);
            }
        }

        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString(formatter));
        }
    }

    public static class LocalTimeSerialization implements JsonDeserializer<LocalTime>, JsonSerializer<LocalTime> {
        private final DateTimeFormatter format;

        public LocalTimeSerialization(@NonNull DateTimeFormatter format) {
            this.format = format;
        }

        @Override
        public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                long timestamp = primitive.getAsLong();
                return new LocalTime(timestamp);
            } else if (primitive.isString()) {
                return format.parseLocalTime(primitive.getAsString());
            } else {
                throw new JsonParseException("Unexpected primitive " + primitive);
            }
        }

        @Override
        public JsonElement serialize(LocalTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString(format));
        }
    }


    public enum SerializeAs {
        STRING,
        NUMBER,
    }
}
