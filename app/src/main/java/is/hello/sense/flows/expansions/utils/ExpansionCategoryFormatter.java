package is.hello.sense.flows.expansions.utils;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.ExpansionValueRange;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Constants;

public class ExpansionCategoryFormatter {

    public String getFormattedValueRange(@NonNull final Category category,
                                         @NonNull final ExpansionValueRange valueRange,
                                         @NonNull final Context context){
        final CharSequence suffix = getSuffix(category);

        return context.getString(R.string.smart_alarm_expansion_same_value_format,
                                     valueRange.min, suffix);
        //todo uncomment if need to handle displaying min and max range.
        /*return context.getString(R.string.smart_alarm_expansion_range_value_format,
                                     valueRange.min, suffix,
                                     valueRange.max, suffix);*/

    }

    public String getFormattedAttributionValueRange(@NonNull final Category category,
                                                    @NonNull final ExpansionValueRange expansionRange,
                                                    @NonNull final Context context) {
        return context.getString(R.string.smart_alarm_expansion_attribute_format,
                                 getFormattedValueRange(category,
                                                        expansionRange,
                                                        context));
    }

    public String getSuffix(@NonNull final Category category){
        switch (category){
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
        switch (category){
            case LIGHT:
                return R.drawable.icon_alarm_light;
            case TEMPERATURE:
                return R.drawable.icon_alarm_thermostat;
            default:
                return R.drawable.error_white;
        }
    }

    @StringRes
    public int getDisplayValueResFromState(@NonNull final State expansionState){
        switch (expansionState) {
            case CONNECTED_ON:
                return R.string.smart_alarm_expansion_state_connected_on;
            default:
                return R.string.smart_alarm_expansion_state_connected_off;
        }
    }

    public int[] getInitialValues(@NonNull final Category category,
                                  @Nullable final ExpansionValueRange initialValueRange,
                                  @NonNull final ExpansionValueRange defaultValueRange) {
        final int minValue = initialValueRange != null ? initialValueRange.min : defaultValueRange.max - defaultValueRange.min;
        final int maxValue = initialValueRange != null ? initialValueRange.max : defaultValueRange.max - defaultValueRange.min;

        switch (category){
            case LIGHT:
                return new int[]{maxValue};
            case TEMPERATURE:
                return new int[]{maxValue}; //if revert back to range use {minValue, maxValue}
            default:
                throw new IllegalStateException("no initial values provided yet for category " + category);
        }
    }
}
