package is.hello.sense.units;

import android.support.annotation.NonNull;

public interface UnitPrinter {
    UnitPrinter SIMPLE = Long::toString;

    @NonNull CharSequence print(long value);
}
