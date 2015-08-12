package is.hello.sense.units;

import android.support.annotation.NonNull;

public interface UnitPrinter {
    @NonNull CharSequence print(long value);
}
