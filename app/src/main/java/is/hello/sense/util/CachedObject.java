package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

import is.hello.sense.functional.Functions;
import rx.Observable;
import rx.schedulers.Schedulers;

public class CachedObject<T extends Serializable> {
    private final TypeReference<T> valueType;
    private final File objectFile;
    private final ObjectMapper objectMapper;

    public CachedObject(@NonNull Context context,
                        @NonNull String filename,
                        @NonNull TypeReference<T> valueType,
                        @NonNull ObjectMapper objectMapper) {
        this.valueType = valueType;
        this.objectFile = new File(context.getCacheDir().getAbsolutePath() + File.separator + filename);
        this.objectMapper = objectMapper;
    }


    public Observable<T> get() {
        return Observable.create((Observable.OnSubscribe<T>) s -> {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(objectFile);
                T value = objectMapper.readValue(inputStream, this.valueType);
                s.onNext(value);
                s.onCompleted();
            } catch (IOException e) {
                s.onError(e);
            } finally {
                Functions.safeClose(inputStream);
            }
        }).subscribeOn(Schedulers.io());
    }

    public Observable<T> set(@Nullable T value) {
        return Observable.create((Observable.OnSubscribe<T>) s -> {
            if (value != null) {
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(objectFile);
                    objectMapper.writeValue(outputStream, value);
                    s.onNext(value);
                    s.onCompleted();
                } catch (IOException e) {
                    s.onError(e);
                } finally {
                    Functions.safeClose(outputStream);
                }
            } else {
                if (objectFile.delete()) {
                    s.onNext(null);
                    s.onCompleted();
                } else {
                    s.onError(new FileNotFoundException());
                }
            }
        }).subscribeOn(Schedulers.io());
    }
}
