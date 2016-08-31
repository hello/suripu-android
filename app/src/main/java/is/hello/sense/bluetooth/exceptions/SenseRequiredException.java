package is.hello.sense.bluetooth.exceptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.buruberi.bluetooth.errors.BuruberiException;
import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;

import static is.hello.sense.util.Analytics.SenseUpdate.ERROR_SENSE_REQUIRED;

public class SenseRequiredException extends BuruberiException implements Errors.Reporting {
    public SenseRequiredException() {
        super("Sense Required to perform operation");
    }

    public SenseRequiredException(final String detailMessage) {
        super(detailMessage);
    }

    @Nullable
    @Override
    public String getContextInfo() {
        return ERROR_SENSE_REQUIRED;
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        return StringRef.from(R.string.error_sense_update_failed_message);
    }
}
