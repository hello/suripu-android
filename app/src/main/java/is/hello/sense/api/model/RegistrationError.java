package is.hello.sense.api.model;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public enum RegistrationError implements Enums.FromString {
    NAME_TOO_LONG(R.string.error_account_name_too_long),
    NAME_TOO_SHORT(R.string.error_account_name_too_short),
    EMAIL_INVALID(R.string.error_account_email_invalid),
    EMAIL_IN_USE(R.string.error_account_email_in_use),
    PASSWORD_INSECURE(R.string.error_account_password_insecure),
    PASSWORD_TOO_SHORT(R.string.error_account_password_too_short),
    UNKNOWN(R.string.error_account_generic);

    public final @StringRes int messageRes;

    RegistrationError(@StringRes int messageRes) {
        this.messageRes = messageRes;
    }

    public static RegistrationError fromString(@Nullable String string) {
        return Enums.fromString(string, values(), UNKNOWN);
    }
}
