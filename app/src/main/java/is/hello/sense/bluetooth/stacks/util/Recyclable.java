package is.hello.sense.bluetooth.stacks.util;

/**
 * An object for which there are a finite number of
 * instances, each of which must be recycled after use.
 */
public interface Recyclable {
    /**
     * Releases the object back into its class's object pool.
     */
    void recycle();
}
