package is.hello.sense.flows.expansions.utils;

import android.support.annotation.NonNull;

import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.ExpansionValueRange;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Constants;

public class ExpansionCategoryFormatter {

    public String getFormattedValueRange(@NonNull final Category category,
                                               @NonNull final ExpansionValueRange valueRange){
        final CharSequence suffix = getSuffix(category);
        if(valueRange.hasSameValues()){
            return String.format("%s%s", valueRange.min, suffix);
        } else {
            return String.format("%s%s - %s%s", valueRange.min, suffix, valueRange.max, suffix);
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

}
