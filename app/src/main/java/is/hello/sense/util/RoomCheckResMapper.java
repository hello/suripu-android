package is.hello.sense.util;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.sensors.SensorType;

public class RoomCheckResMapper {
    @DrawableRes
    public int getConditionDrawable(@NonNull final Condition condition) {
        switch (condition) {
            case ALERT: {
                return R.drawable.onboarding_sense_red;
            }

            case WARNING: {
                return R.drawable.onboarding_sense_yellow;
            }

            case IDEAL: {
                return R.drawable.onboarding_sense_green;
            }

            case UNKNOWN:
            default: {
                return R.drawable.onboarding_sense_grey;
            }
        }
    }

    @StringRes
    public int getCheckStatusStringForSensor(@NonNull final SensorType type) {
        switch (type) {
            case TEMPERATURE: {
                return R.string.checking_condition_temperature;
            }
            case HUMIDITY: {
                return R.string.checking_condition_humidity;
            }
            case PARTICULATES: {
                return R.string.checking_condition_airquality;
            }
            case LIGHT: {
                return R.string.checking_condition_light;
            }
            case SOUND: {
                return R.string.checking_condition_sound;
            }
            case CO2: {
                return R.string.checking_condition_co2;
            }
            case TVOC: {
                return R.string.checking_condition_voc;
            }
            case LIGHT_TEMPERATURE: {
                return R.string.checking_condition_light_temperature;
            }
            case UV: {
                return R.string.checking_condition_uv;
            }
            case PRESSURE: {
                return R.string.checking_condition_pressure;
            }
            case UNKNOWN:
            default: {
                return R.string.missing_data_placeholder;
            }
        }
    }

    @DrawableRes
    public int getInitialIconForSensor(@NonNull final SensorType type) {
        switch (type) {
            case TEMPERATURE: {
                return R.drawable.temperature_gray_nofill;
            }
            case HUMIDITY: {
                return R.drawable.humidity_gray_nofill;
            }
            case PARTICULATES: {
                return R.drawable.air_quality_gray_nofill;
            }
            case LIGHT: {
                return R.drawable.light_gray_nofill;
            }
            case SOUND: {
                return R.drawable.noise_gray_nofill;
            }
            case CO2: {
                return R.drawable.co2_gray_nofill;
            }
            case TVOC: {
                return R.drawable.voc_gray_nofill;
            }
            case LIGHT_TEMPERATURE: {
                return R.drawable.light_temperature_gray_nofill;
            }
            case UV: {
                return R.drawable.uv_gray_nofill;
            }
            case PRESSURE: {
                return R.drawable.pressure_gray_nofill;
            }
            case UNKNOWN:
            default:
                return R.drawable.error_white;
        }
    }

    @DrawableRes
    public int getFinalIconForSensor(@NonNull final SensorType type) {
        switch (type) {
            case TEMPERATURE: {
                return R.drawable.temperature_gray_fill;
            }
            case HUMIDITY: {
                return R.drawable.humidity_gray_fill;
            }
            case PARTICULATES: {
                return R.drawable.air_quality_gray_fill;
            }
            case LIGHT: {
                return R.drawable.light_gray_fill;
            }
            case SOUND: {
                return R.drawable.noise_gray_fill;
            }
            case CO2: {
                return R.drawable.co2_gray_fill;
            }
            case TVOC: {
                return R.drawable.voc_gray_fill;
            }
            case LIGHT_TEMPERATURE: {
                return R.drawable.light_temperature_gray_fill;
            }
            case UV: {
                return R.drawable.uv_gray_fill;
            }
            case PRESSURE: {
                return R.drawable.pressure_gray_fill;
            }
            case UNKNOWN:
            default:
                return R.drawable.error_white;
        }
    }
}
