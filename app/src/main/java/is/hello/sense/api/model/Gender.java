package is.hello.sense.api.model;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import is.hello.sense.R;

public enum Gender {
    FEMALE(R.string.gender_female),
    MALE(R.string.gender_male),
    OTHER(R.string.gender_other);

    public final @StringRes int nameRes;

    private Gender(int nameRes) {
        this.nameRes = nameRes;
    }

    public static Gender fromString(@Nullable String string) {
        return Enums.fromString(string, values(), OTHER);
    }
}
