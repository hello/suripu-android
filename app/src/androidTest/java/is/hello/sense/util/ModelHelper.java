package is.hello.sense.util;

import android.support.annotation.NonNull;

import java.lang.reflect.Field;

import is.hello.sense.api.model.ApiResponse;

public final class ModelHelper<T extends ApiResponse> {
    private final T object;
    private final Class<? extends ApiResponse> objectClass;

    public static <T extends ApiResponse> ModelHelper<T> manipulate(@NonNull T object) {
        return new ModelHelper<>(object);
    }
    
    private ModelHelper(T object) {
        this.object = object;
        this.objectClass = object.getClass();
    }
    
    
    public ModelHelper<T> set(@NonNull String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = objectClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
        return this;
    }

    public ModelHelper<T> set(@NonNull String fieldName, boolean value) throws NoSuchFieldException, IllegalAccessException {
        Field field = objectClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(object, value);
        return this;
    }

    public ModelHelper<T> set(@NonNull String fieldName, int value) throws NoSuchFieldException, IllegalAccessException {
        Field field = objectClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(object, value);
        return this;
    }

    public ModelHelper<T> set(@NonNull String fieldName, long value) throws NoSuchFieldException, IllegalAccessException {
        Field field = objectClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setLong(object, value);
        return this;
    }

    public ModelHelper<T> set(@NonNull String fieldName, float value) throws NoSuchFieldException, IllegalAccessException {
        Field field = objectClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setFloat(object, value);
        return this;
    }

    public ModelHelper<T> set(@NonNull String fieldName, double value) throws NoSuchFieldException, IllegalAccessException {
        Field field = objectClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setDouble(object, value);
        return this;
    }

    public <U> U get(@NonNull String fieldName, @NonNull Class<U> clazz) throws NoSuchFieldException, IllegalAccessException {
        Field field = objectClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return clazz.cast(field.get(object));
    }
    
    
    public @NonNull T unwrap() {
        return object;
    }
}
