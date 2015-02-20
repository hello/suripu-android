package is.hello.sense.api.model;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.sense.R;

public enum Gender {
    MALE(R.string.gender_male),
    FEMALE(R.string.gender_female),
    OTHER(R.string.gender_other);

    public final @StringRes int nameRes;

    private Gender(int nameRes) {
        this.nameRes = nameRes;
    }

    public static Gender fromString(@Nullable String string) {
        return Enums.fromString(string, values(), OTHER);
    }
}
