package is.hello.sense.flows.expansions.utils;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
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
        if(valueRange.hasSameValues()){
            return context.getString(R.string.smart_alarm_expansion_same_value_format,
                                     valueRange.min, suffix);
        } else {
            return context.getString(R.string.smart_alarm_expansion_range_value_format,
                                     valueRange.min, suffix,
                                     valueRange.max, suffix);
        }

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
            case CONNECTED_OFF:
                return R.string.smart_alarm_expansion_state_connected_off;
            default:
                return R.string.smart_alarm_expansion_state_connected_on;
        }
    }
}
