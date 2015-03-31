package is.hello.sense.bluetooth.stacks.util;

import rx.functions.Action1;

/**
 * Friendly name for an RxJava doOnError implementation.
 * Exists primarily to make DI cleaner.
 */
public interface ErrorListener extends Action1<Throwable> {
}
