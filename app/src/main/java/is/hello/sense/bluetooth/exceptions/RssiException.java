package is.hello.sense.bluetooth.exceptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;

public class RssiException extends Exception{
    public RssiException(){
        super("Pill is to far"); // todo get text
    }
}
