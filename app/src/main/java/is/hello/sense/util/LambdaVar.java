package is.hello.sense.util;

import android.support.annotation.NonNull;

import rx.functions.Func1;

public final class LambdaVar<T> {
    private T value;


    //region Creation

    public static <T> LambdaVar<T> empty() {
        return new LambdaVar<>(null);
    }

    public static <T> LambdaVar<T> of(@NonNull T value) {
        return new LambdaVar<>(value);
    }

    private LambdaVar(T value) {
        this.value = value;
    }

    //endregion


    //region Mutation

    public LambdaVar<T> clear() {
        this.value = null;
        return this;
    }

    public LambdaVar<T> set(T value) {
        this.value = value;
        return this;
    }

    public T get() {
        return value;
    }

    public T getAndMutate(@NonNull Func1<T, T> mutator) {
        T oldValue = value;
        set(mutator.call(oldValue));
        return oldValue;
    }

    public boolean isNull() {
        return (value == null);
    }

    //endregion


    @Override
    public int hashCode() {
        if (value != null) {
            return value.hashCode();
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LambdaVar lambdaVar = (LambdaVar) o;
        return !(value != null ? !value.equals(lambdaVar.value) : lambdaVar.value != null);

    }

    @Override
    public String toString() {
        return "LambdaVar{" +
                "value=" + value +
                '}';
    }
}
