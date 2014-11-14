package is.hello.sense.bluetooth.stacks.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the ownership of an object implementing
 * {@see is.hello.sense.bluetooth.stacks.util.Recyclable}
 * will be transferred to a callee, and the callee will
 * ensure the object is recycled when appropriate.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
@Inherited
@Documented
public @interface TakesOwnership {
}
