package is.hello.sense.functional;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import is.hello.sense.util.Logger;
import rx.functions.Action1;

public final class Functions {

    //region General

    public static boolean isNotNull(@Nullable Object object) {
        return (object != null);
    }
    public static boolean safeClose(@Nullable Closeable closeable) {
        if (closeable == null)
            return false;

        try {
            closeable.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    public static int compareInts(int a, int b) {
        return (a < b) ? -1 : ((a > b) ? 1 : 0);
    }
    public static final Action1<Throwable> LOG_ERROR = e -> Logger.error("UnexpectedErrors", "An error occurred.", e);

    //endregion


    //region Collections

    public static <T, R> List<R> mapList(@NonNull Iterable<T> source, @NonNull Function<T, R> mapper) {
        List<R> accumulator = new ArrayList<>();
        for (T value : source) {
            accumulator.add(mapper.apply(value));
        }
        return accumulator;
    }

    public static <T, K> List<List<T>> segmentList(@NonNull Function<T, K> keyProducer, @NonNull Iterable<T> source) {
        LinkedHashMap<K, List<T>> result = new LinkedHashMap<>();
        for (T value : source) {
            K key = keyProducer.apply(value);
            List<T> segment = result.get(key);
            if (segment == null) {
                segment = new ArrayList<>();
                result.put(key, segment);
            }
            segment.add(value);
        }
        return new ArrayList<>(result.values());
    }

    public static <T extends Comparable<T>> List<T> sortedList(@NonNull Collection<T> toSort) {
        List<T> sortedCopy = new ArrayList<>(toSort.size());
        sortedCopy.addAll(toSort);
        Collections.sort(sortedCopy);
        return sortedCopy;
    }

    public static <T> List<T> filteredList(@NonNull Iterable<T> toFilter, @NonNull Function<T, Boolean> predicate) {
        List<T> results = new ArrayList<>();
        for (T value : toFilter) {
            if (predicate.apply(value))
                results.add(value);
        }
        return results;
    }

    public static @Nullable <T> T findFirst(@NonNull Iterable<T> haystack, @NonNull Function<T, Boolean> needle) {
        for (T value : haystack) {
            if (needle.apply(value))
                return value;
        }

        return null;
    }

    //endregion

}
