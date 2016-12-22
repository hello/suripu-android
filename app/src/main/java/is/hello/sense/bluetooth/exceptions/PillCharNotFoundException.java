package is.hello.sense.bluetooth.exceptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.UUID;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;

public class PillCharNotFoundException extends Exception
        implements Errors.Reporting {
    public PillCharNotFoundException(final UUID id){
        super("Unable to find pill characteristic with id" + id +
                      ". Try clearing ble cache.");
    }

    @Nullable
    @Override
    public String getContextInfo() {
        return getDisplayMessage().toString();
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        return StringRef.from(R.string.error_bluetooth_pill_characteristic_discovery_failed);
    }
}
