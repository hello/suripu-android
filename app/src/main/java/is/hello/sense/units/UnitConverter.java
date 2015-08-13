package is.hello.sense.units;

/**
 * A functor that converts a raw value into the user's unit system.
 */
public interface UnitConverter {
    /**
     * The identity converter. Returns a value unmodified.
     */
    UnitConverter IDENTITY = v -> v;

    long convert(long value);
}
