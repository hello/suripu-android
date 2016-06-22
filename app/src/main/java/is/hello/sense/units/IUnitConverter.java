package is.hello.sense.units;

/**
 * A functor that converts a raw value into the user's unit system.
 */
public interface IUnitConverter {

    double convert(double value);
}
