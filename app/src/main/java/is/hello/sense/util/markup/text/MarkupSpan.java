package is.hello.sense.util.markup.text;

import android.os.Parcelable;

import java.io.Serializable;

/**
 * A span to be used with {@link MarkupString} that supports serialization
 * through the built-in Java mechanism, and through Parcelable.
 * <p />
 * Implementors must implement the identity object methods.
 */
public interface MarkupSpan extends Serializable, Parcelable {
}
