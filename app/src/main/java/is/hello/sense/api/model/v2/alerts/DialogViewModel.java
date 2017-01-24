package is.hello.sense.api.model.v2.alerts;

import java.io.Serializable;

public interface DialogViewModel<T> extends Serializable {

    String getTitle();

    String getBody();

    String getPositiveButtonText();

    String getNeutralButtonText();

    /**
     * Used for comparison and sending to analytics
     */
    T getAnalyticPropertyType();
}
