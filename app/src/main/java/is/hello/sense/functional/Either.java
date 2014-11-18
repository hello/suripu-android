package is.hello.sense.functional;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rx.functions.Action1;
import rx.functions.Func1;

/**
 * <code>type Either 'l 'r = Left 'l | Right 'r</code>
 * <p/>
 * A pseudo-discriminated union which represents one of two possible values.
 * @param <Left>    The left value type. A successful value by convention.
 * @param <Right>   The right value type. An error value by convention.
 */
public final class Either<Left, Right> {
    private @Nullable Left left;
    private @Nullable Right right;


    //region Creation

    public static <Left, Right> Either<Left, Right> left(@NonNull Left value) {
        return new Either<>(value, null);
    }

    public static <Left, Right> Either<Left, Right> right(@NonNull Right value) {
        return new Either<>(null, value);
    }

    private Either(@Nullable Left left, @Nullable Right right) {
        this.left = left;
        this.right = right;
    }

    //endregion


    //region Introspection

    public boolean isLeft() {
        return (left != null && right == null);
    }

    public @NonNull Left getLeft() {
        if (left == null) {
            throw new NullPointerException();
        }

        return left;
    }

    public @NonNull Right getRight() {
        if (right == null) {
            throw new NullPointerException();
        }

        return right;
    }

    public void match(@NonNull Action1<Left> onLeft,
                      @NonNull Action1<Right> onRight) {
        if (isLeft()) {
            onLeft.call(getLeft());
        } else {
            onRight.call(getRight());
        }
    }

    public <R> R map(@NonNull Func1<Left, R> onLeft,
                     @NonNull Func1<Right, R> onRight) {
        if (isLeft()) {
            return onLeft.call(getLeft());
        } else {
            return onRight.call(getRight());
        }
    }

    //endregion


    //region Identity

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Either either = (Either) o;

        return !(left != null ? !left.equals(either.left) : either.left != null) &&
               !(right != null ? !right.equals(either.right) : either.right != null);
    }

    @Override
    public int hashCode() {
        if (left != null) {
            return left.hashCode();
        } else if (right != null) {
            return right.hashCode();
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        if (left != null) {
            return "{Either left=" + left + "}";
        } else if (right != null) {
            return "{Either right=" + right + "}";
        } else {
            return "{Either invalid}";
        }
    }

    //endregion
}
