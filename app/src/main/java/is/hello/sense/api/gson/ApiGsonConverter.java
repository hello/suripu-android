package is.hello.sense.api.gson;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.lang.reflect.Type;

import is.hello.sense.api.model.VoidResponse;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class ApiGsonConverter implements Converter {
    private final GsonConverter wrappedConverter;

    public ApiGsonConverter(@NonNull Gson gson) {
        this.wrappedConverter = new GsonConverter(gson);
    }


    @Override
    public Object fromBody(TypedInput body, Type type) throws ConversionException {
        if (type == VoidResponse.class) {
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
