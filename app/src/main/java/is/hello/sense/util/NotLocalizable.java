package is.hello.sense.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Project annotation to mark classes and methods that are
 * implemented in such a way that they will break down when
 * full localization is applied to the application.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface NotLocalizable {
    BecauseOf[] value();

    enum BecauseOf {
        ALPHABET,
        RTL,
        METRICS,
    }
}
