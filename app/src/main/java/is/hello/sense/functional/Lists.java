package is.hello.sense.functional;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Lists {
    @SafeVarargs
    public static <T> ArrayList<T> newArrayList(T... values) {
        ArrayList<T> list = new ArrayList<>(values.length);
        Collections.addAll(list, values);
        return list;
    }

    public static <T> boolean isEmpty(@Nullable Collection<T> source) {
        return (source == null || source.isEmpty());
    }

    public static <T, R> List<R> map(@NonNull Iterable<T> source, @NonNull Function<T, R> mapper) {
        List<R> accumulator = new ArrayList<>();
        for (T value : source) {
            accumulator.add(mapper.apply(value));
        }
        return accumulator;
    }

    public static <T> List<T> takeEvery(@NonNull List<T> source, int step) {
        List<T> result = new ArrayList<>();
        int i = 0;
        for (T value : source) {
            if ((i % step) == 0) {
                result.add(value);
            }
            i++;
        }
        return result;
    }

    public static <T extends Comparable<T>> List<T> sorted(@NonNull Collection<T> toSort, @NonNull Comparator<T> comparator) {
        List<T> sortedCopy = new ArrayList<>(toSort.size());
        sortedCopy.addAll(toSort);
        Collections.sort(sortedCopy, comparator);
        return sortedCopy;
    }

    public static <T> List<T> filtered(@NonNull Iterable<T> toFilter, @NonNull Function<T, Boolean> predicate) {
        List<T> results = new ArrayList<>();
        for (T value : toFilter) {
            if (predicate.apply(value))
                results.add(value);
        }
        return results;
    }

    public static <T> int sumInt(@NonNull Iterable<T> toSum, @NonNull Function<T, Integer> converter) {
        int sum = 0;
        for (T value : toSum) {
            sum += converter.apply(value);
        }
        return sum;
    }

}
