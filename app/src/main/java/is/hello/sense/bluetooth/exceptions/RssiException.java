package is.hello.sense.bluetooth.exceptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.buruberi.bluetooth.errors.BuruberiException;
import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;

import static is.hello.sense.util.Analytics.PillUpdate.Error.PILL_TOO_FAR;

public class RssiException extends BuruberiException implements Errors.Reporting{
    public RssiException(){
        super("Pill is too far");
    }

    @Nullable
    @Override
    public String getContextInfo() {
        return PILL_TOO_FAR;
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        return null;
    }
}
