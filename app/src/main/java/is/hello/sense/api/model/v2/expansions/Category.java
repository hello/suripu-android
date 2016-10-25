package is.hello.sense.api.model.v2.expansions;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public enum Category implements Enums.FromString {
    LIGHT("Room", R.string.expansion_category_lights),
    TEMPERATURE("Thermostat", R.string.expansion_category_thermostats),
    UNKNOWN("Configuration", R.string.expansion_category_configurations);

    //todo remove when ConfigurationType field is returned
    public final String displayString;

    public final int categoryDisplayString;

    Category(final String displayString,
             @StringRes final int categoryDisplayString){
        this.displayString = displayString;
        this.categoryDisplayString = categoryDisplayString;
    }

    public static Category fromString(@Nullable final String string) {
        return Enums.fromString(string, values(), UNKNOWN);
    }
}
