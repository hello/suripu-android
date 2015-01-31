package is.hello.sense.ui.widget.graphing;

import android.support.annotation.NonNull;

import java.util.Comparator;
import java.util.Iterator;

import rx.functions.Func1;

public final class Extremes<T> {
    public final T minValue;
    public final int minPosition;

    public final T maxValue;
    public final int maxPosition;

    /**
     * Calculates the minimum and maximum extremes of a given Iterable.
     */
    public static <T> Extremes<T> of(@NonNull Iterable<T> haystack, @NonNull Comparator<T> comparator) {
        Iterator<T> iterator = haystack.iterator();

        T min = iterator.next();
        int minIndex = 0;

        T max = min;
        int maxIndex = 0;

        int index = 0;
        while (iterator.hasNext()) {
            index++;
            T next = iterator.next();

            if (comparator.compare(max, next) < 0) {
                max = next;
                maxIndex = index;
            }

            if (comparator.compare(min, next) > 0) {
                min = next;
                minIndex = index;
            }
        }

        return new Extremes<>(min, minIndex,
                              max, maxIndex);
    }

    public Extremes(T minValue, int minPosition,
                    T maxValue, int maxPosition) {
        this.minValue = minValue;
        this.minPosition = minPosition;

        this.maxValue = maxValue;
        this.maxPosition = maxPosition;
    }


    public <U> Extremes<U> map(@NonNull Func1<T, U> mapper) {
        return new Extremes<>(mapper.call(minValue), minPosition,
                              mapper.call(maxValue), maxPosition);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Extremes extremes = (Extremes) o;

        if (maxPosition != extremes.maxPosition) return false;
        if (minPosition != extremes.minPosition) return false;
        if (maxValue != null ? !maxValue.equals(extremes.maxValue) : extremes.maxValue != null) return false;
        if (minValue != null ? !minValue.equals(extremes.minValue) : extremes.minValue != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = minValue != null ? minValue.hashCode() : 0;
        result = 31 * result + minPosition;
        result = 31 * result + (maxValue != null ? maxValue.hashCode() : 0);
        result = 31 * result + maxPosition;
        return result;
    }

    @Override
    public String toString() {
        return "Extremes{" +
                "minValue=" + minValue +
                ", minPosition=" + minPosition +
                ", maxValue=" + maxValue +
                ", maxPosition=" + maxPosition +
                '}';
    }
}
