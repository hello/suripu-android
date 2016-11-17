package is.hello.sense.flows.expansions.utils;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.ExpansionValueRange;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.units.UnitConverter;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Constants;

public class ExpansionCategoryFormatter {

    private final UnitFormatter unitFormatter;

    public ExpansionCategoryFormatter(@NonNull final UnitFormatter unitFormatter){
        this.unitFormatter = unitFormatter;
    }

    public String getFormattedValueRange(@NonNull final Category category,
                                         @NonNull final ExpansionValueRange valueRange,
                                         @NonNull final Context context) {
        final CharSequence suffix = getSuffix(category);

        final UnitConverter unitConverter = getUnitConverter(category);

        switch (category){
            case TEMPERATURE:
                if (!valueRange.hasSameValues()) {
                    return context.getString(R.string.smart_alarm_expansion_range_value_format,
                                             unitConverter.convert(valueRange.min)
                                                          .intValue(), suffix,
                                             unitConverter.convert(valueRange.max)
                                                          .intValue(), suffix);
                }
            case LIGHT:
            default:
                return context.getString(R.string.smart_alarm_expansion_same_value_format,
                                         unitConverter.convert(valueRange.min)
                                                      .intValue(),
                                         suffix);
        }
    }

    public UnitConverter getUnitConverter(@NonNull final Category category) {
        switch (category){
            case TEMPERATURE:
                return unitFormatter.getTemperatureUnitConverter();
            default:
                return UnitConverter.IDENTITY;
        }
    }

    public UnitConverter getReverseUnitConverter(@NonNull final Category category) {
        switch (category){
            case TEMPERATURE:
                return unitFormatter.getReverseTemperatureUnitConverter();
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

    /**
     * @return display string resource that will be used primarily in
     * {@link is.hello.sense.flows.smartalarm.ui.fragments.SmartAlarmDetailFragment}
     */
    @StringRes
    public int getDisplayValueResFromState(@NonNull final State expansionState) {
        switch (expansionState) {
            case CONNECTED_ON:
                return R.string.smart_alarm_expansion_state_connected_on;
            case NOT_AVAILABLE:
                return R.string.expansions_state_not_available; //used in expansions as well
            default:
                return R.string.smart_alarm_expansion_state_connected_off;

        }
    }

    @XmlRes
    public int getExpansionInfoDialogXmlRes(@NonNull final Category category) {
        switch (category){
            case LIGHT:
                return R.xml.welcome_dialog_expansions_settings_light;
            case TEMPERATURE:
                return R.xml.welcome_dialog_expansions_settings_thermostat;
            default:
                return R.xml.welcome_dialog_expansions;
        }
    }

    @XmlRes
    public int getExpansionAlarmInfoDialogXmlRes(@NonNull final Category category) {
        switch (category){
            case LIGHT:
                return R.xml.welcome_dialog_expansions_alarm_light;
            case TEMPERATURE:
                return R.xml.welcome_dialog_expansions_alarm_thermostat;
            default:
                return R.xml.welcome_dialog_expansions;
        }
    }
}
