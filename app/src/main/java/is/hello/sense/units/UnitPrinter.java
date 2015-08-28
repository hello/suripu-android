package is.hello.sense.units;

import android.support.annotation.NonNull;

public interface UnitPrinter {
    UnitPrinter SIMPLE = v -> String.format("%.0f", v);

    @NonNull CharSequence print(double value);
}
