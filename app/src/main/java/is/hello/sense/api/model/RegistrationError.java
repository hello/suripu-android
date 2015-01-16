package is.hello.sense.api.model;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.sense.R;

public enum RegistrationError {
    NAME_TOO_LONG(R.string.error_account_name_too_long),
    NAME_TOO_SHORT(R.string.error_account_name_too_short),
    EMAIL_INVALID(R.string.error_account_email_invalid),
    PASSWORD_INSECURE(R.string.error_account_password_insecure),
    PASSWORD_TOO_SHORT(R.string.error_account_password_too_short),
    UNKNOWN(R.string.error_account_generic);

    public final @StringRes int messageRes;

    private RegistrationError(@StringRes int messageRes) {
        this.messageRes = messageRes;
    }

    public static RegistrationError fromString(@Nullable String string) {
        return Enums.fromString(string, values(), UNKNOWN);
    }
}
