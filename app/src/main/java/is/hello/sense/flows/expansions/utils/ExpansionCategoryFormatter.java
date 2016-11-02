package is.hello.sense.flows.expansions.utils;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.ExpansionValueRange;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.units.UnitConverter;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.util.Constants;

public class ExpansionCategoryFormatter {

    private final PreferencesInteractor preferences;

    public ExpansionCategoryFormatter(@NonNull final PreferencesInteractor preferencesInteractor){
        this.preferences = preferencesInteractor;
    }

    public String getFormattedValueRange(@NonNull final Category category,
                                         @NonNull final ExpansionValueRange valueRange,
                                         @NonNull final Context context) {
        final CharSequence suffix = getSuffix(category);

        final UnitConverter unitConverter = getUnitConverter(category);

        return context.getString(R.string.smart_alarm_expansion_same_value_format,
                                 unitConverter.convert((float) valueRange.min)
                                              .intValue(),
                                 suffix);
    }

    public UnitConverter getUnitConverter(@NonNull final Category category) {
        switch (category){
            case TEMPERATURE:
                if(preferences.getBoolean(PreferencesInteractor.USE_CELSIUS, true)){
                    return UnitConverter.IDENTITY;
                } else {
                    return UnitOperations::celsiusToFahrenheit;
                }
            default:
                return UnitConverter.IDENTITY;
        }
    }

    public String getFormattedAttributionValueRange(@NonNull final Category category,
                                                    @NonNull final ExpansionValueRange expansionRange,
                                                    @NonNull final Context context) {
        return context.getString(R.string.smart_alarm_expansion_attribute_format,
                                 getFormattedValueRange(category,
                                                        expansionRange,
                                                        context));
    }

    public String getSuffix(@NonNull final Category category) {
        switch (category) {
            case LIGHT:
                return UnitFormatter.UNIT_SUFFIX_PERCENT;
            case TEMPERATURE:
                return UnitFormatter.UNIT_SUFFIX_TEMPERATURE;
            default:
                return Constants.EMPTY_STRING;
        }
    }

    @DrawableRes
    public int getDisplayIconRes(@NonNull final Category category) {
        switch (category) {
            case LIGHT:
                return R.drawable.icon_alarm_light;
            case TEMPERATURE:
                return R.drawable.icon_alarm_thermostat;
            default:
                return R.drawable.error_white;
        }
    }

    @StringRes
    public int getDisplayValueResFromState(@NonNull final State expansionState) {
        switch (expansionState) {
            case CONNECTED_ON:
                return R.string.smart_alarm_expansion_state_connected_on;
            default:
                return R.string.smart_alarm_expansion_state_connected_off;

        }
    }
}
