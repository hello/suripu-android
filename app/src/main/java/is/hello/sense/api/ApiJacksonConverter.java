package is.hello.sense.api;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

import is.hello.sense.api.model.VoidResponse;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.converter.JacksonConverter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class ApiJacksonConverter implements Converter {
    private final ObjectMapper objectMapper;
    private final JacksonConverter wrappedConverter;

    public ApiJacksonConverter(@NonNull ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.wrappedConverter = new JacksonConverter(objectMapper);
    }


    @Override
    public Object fromBody(TypedInput body, Type type) throws ConversionException {
        JavaType javaType = objectMapper.getTypeFactory().constructType(type);
        if (javaType.hasRawClass(VoidResponse.class)) {
            return new VoidResponse();
        } else {
            return wrappedConverter.fromBody(body, type);
        }
    }

    @Override
    public TypedOutput toBody(Object object) {
        return wrappedConverter.toBody(object);
    }
}
