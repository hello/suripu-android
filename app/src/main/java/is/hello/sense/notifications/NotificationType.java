package is.hello.sense.notifications;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.fasterxml.jackson.annotation.JsonCreator;

import is.hello.sense.R;
import is.hello.sense.api.model.Enums;

public enum NotificationType {
    MESSAGE(R.string.notification_type_message),
    QUESTION(R.string.notification_type_question);

    public final @StringRes int titleRes;

    private NotificationType(int titleRes) {
        this.titleRes = titleRes;
    }

    @JsonCreator
    public static NotificationType fromString(@Nullable String value) {
        return Enums.fromString(value, values(), MESSAGE);
    }
}
