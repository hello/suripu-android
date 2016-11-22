package is.hello.sense.flows.expansions.utils;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v4.util.Pair;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Capability;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.ExpansionValueRange;
import is.hello.sense.api.model.v2.expansions.State;
import is.hello.sense.units.UnitConverter;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Constants;

public class ExpansionCategoryFormatter {
    private final static float DEFAULT_TEMP_MULTIPLIER = .32f; // will initialize 9-32 to 16.
    private final static float DEFAULT_LIGHT_MULTIPLIER = .2f; // will initialize 1-100 to 20.
    private final static int TEMP_RANGE = 3; // range of temperatures. this may be replaced by one in expansion picker
    private final UnitFormatter unitFormatter;

    @Inject
    public ExpansionCategoryFormatter(@NonNull final UnitFormatter unitFormatter) {
        this.unitFormatter = unitFormatter;
    }

    public String getFormattedValueRange(@NonNull final Category category,
                                         @NonNull final ExpansionValueRange valueRange,
                                         @NonNull final Context context) {
        final CharSequence suffix = getSuffix(category);

        final UnitConverter unitConverter = getUnitConverter(category);

        switch (category) {
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

    /**
     *
     * @param category of expansion or expansionAlarm
     * @param capabilities of configuration
     * @param valueRange of expansion or expansionAlarm or initial range
     * @return {@link Pair <Integer,Integer>} of values where first is min and optional second is max
     */
    public Pair<Integer,Integer> getInitialValuePair(@NonNull final Category category,
                                     @NonNull final List<Capability> capabilities,
                                     @NonNull final ExpansionValueRange valueRange){
        final UnitConverter unitConverter = getUnitConverter(category);
        if(capabilities.contains(Capability.HEAT)
                && capabilities.contains(Capability.COOL)){
            return new Pair<>(
                    unitConverter.convert(valueRange.min).intValue(),
                    unitConverter.convert(valueRange.max).intValue()
            );
        } else {
            return new Pair<>(
                    unitConverter.convert(valueRange.min).intValue(),
                    null
            );
        }
    }

    //todo use this method to default values
    public Pair<Integer, Integer> getInitialValueFor(@NonNull final Category category,
                                                     @NonNull final ExpansionValueRange valueRange) {
        final float count = valueRange.max - valueRange.min;
        int min;
        switch (category) {
            case TEMPERATURE:
                min = (int) (valueRange.min + (count * DEFAULT_TEMP_MULTIPLIER));
                return new Pair<>(min, min + TEMP_RANGE);
            case LIGHT:
                min = (int) (valueRange.min + (count * DEFAULT_LIGHT_MULTIPLIER));
                break;
            default:
                min = (int) (count);
        }
        return new Pair<>(min, min);
    }

    public UnitConverter getUnitConverter(@NonNull final Category category) {
        switch (category) {
            case TEMPERATURE:
                return unitFormatter.getTemperatureUnitConverter();
            default:
                return UnitConverter.IDENTITY;
        }
    }

    public UnitConverter getReverseUnitConverter(@NonNull final Category category) {
        switch (category) {
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
        switch (category) {
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
        switch (category) {
            case LIGHT:
                return R.xml.welcome_dialog_expansions_alarm_light;
            case TEMPERATURE:
                return R.xml.welcome_dialog_expansions_alarm_thermostat;
            default:
                return R.xml.welcome_dialog_expansions;
        }
    }
}
