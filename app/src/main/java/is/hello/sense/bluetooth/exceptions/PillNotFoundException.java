package is.hello.sense.bluetooth.exceptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.buruberi.bluetooth.errors.BuruberiException;
import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;

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
        return null;
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        return StringRef.from(R.string.message_no_pills);
    }
}
