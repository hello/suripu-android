package is.hello.sense.bluetooth.exceptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.buruberi.bluetooth.errors.BuruberiException;
import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;

import static is.hello.sense.util.Analytics.PillUpdate.Error.PILL_NOT_DETECTED;

public class PillNotFoundException extends BuruberiException implements Errors.Reporting {
    public PillNotFoundException() {
        super("No Pills Found");
    }

    public PillNotFoundException(final String detailMessage) {
        super(detailMessage);
    }


    @Nullable
    @Override
    public String getContextInfo() {
        return PILL_NOT_DETECTED;
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        return StringRef.from(R.string.message_no_pills);
    }
}
