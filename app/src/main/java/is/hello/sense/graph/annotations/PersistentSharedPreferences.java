package is.hello.sense.graph.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * To be used to store values that will be kept after user session ends
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface PersistentSharedPreferences {
}
