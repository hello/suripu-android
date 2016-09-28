package is.hello.sense.ui.fragments.onboarding;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorType;
import is.hello.sense.mvp.view.onboarding.RoomCheckView;
import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.presenters.RoomCheckPresenter;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.util.Analytics;

public class OnboardingRoomCheckFragment extends BasePresenterFragment
implements RoomCheckPresenter.Output{

    @Inject
    RoomCheckPresenter presenter;

    private RoomCheckView presenterView;

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        this.presenterView = new RoomCheckView(getActivity(), getAnimatorContext());
        return presenterView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(presenterView != null) {
            presenterView.stopAnimations();
            presenterView.releaseViews();
            presenterView = null;
        }
    }

    //region RoomCheck Output

    @Override
    public void initialize(){
        presenterView.initSensorContainerXOffset();
    }

    @Override
    public void createSensorConditionViews(final List<SensorType> types) {
        presenterView.removeSensorContainerViews();

        final LinearLayout.LayoutParams layoutParams
                = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int padding = getResources().getDimensionPixelOffset(R.dimen.item_room_sensor_condition_view_width) / 2;
        for(final SensorType type : types) {
            presenterView.createSensorConditionView(getInitialIconForSensor(type),
                                                    padding,
                                                    layoutParams);
        }
    }

    @Override
    public void unavailableConditions(final Throwable e) {
        Analytics.trackError(e, Analytics.Onboarding.ERROR_MSG_ROOM_CHECK);
        presenterView.removeSensorContainerViews();
    }

    @Override
    public void showConditionAt(final int position,
                                @NonNull final Sensor currentPositionSensor,
                                final int convertedValue,
                                @NonNull final String unitSuffix,
                                @NonNull  final Runnable onComplete) {
        presenterView.animateSenseToGray();

        final Condition condition = currentPositionSensor.getCondition();
        final SensorType sensorType = currentPositionSensor.getType();
        presenterView.showConditionAt(position,
                                      getCheckStatusStringForSensor(currentPositionSensor.getType()),
                                      currentPositionSensor.getMessage(),
                                      getFinalIconForSensor(sensorType),
                                      getConditionDrawable(condition),
                                      condition.colorRes,
                                      convertedValue,
                                      unitSuffix,
                                      onComplete);
    }

    @Override
    public void updateSensorView(final List<Sensor> sensors) {
        for(int position = 0; position < sensors.size(); position++) {
            final Sensor sensor = sensors.get(position);
            presenterView.updateSensorView(position,
                                           sensor.getColor(getActivity()),
                                           getFinalIconForSensor(sensor.getType()));
        }
    }

    @Override
    public void stopAnimations(){
        presenterView.stopAnimations();
    }

    @Override
    public void animateSenseCondition(final boolean animate, @NonNull final Condition condition){
        presenterView.animateSenseCondition(animate, getConditionDrawable(condition));
    }

    @Override
    public void showCompletion(final boolean animate, @NonNull final Runnable onContinue) {
        presenterView.showCompletion(animate,
                                     ignored -> onContinue.run());
    }

    //endregion

    @Nullable
    private Drawable getConditionDrawable(@NonNull final Condition condition) {
        final Resources resources = getResources();
        switch (condition) {
            case ALERT: {
                return ResourcesCompat.getDrawable(resources, R.drawable.onboarding_sense_red, null);
            }

            case WARNING: {
                return ResourcesCompat.getDrawable(resources, R.drawable.onboarding_sense_yellow, null);
            }

            case IDEAL: {
                return ResourcesCompat.getDrawable(resources, R.drawable.onboarding_sense_green, null);
            }

            default:
            case UNKNOWN: {
                return presenterView.getDefaultSenseCondition();
            }
        }
    }

    private @StringRes
    int getCheckStatusStringForSensor(@NonNull final SensorType type) {
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
            default: {
                return R.string.missing_data_placeholder;
            }
        }
    }

    private @DrawableRes
    int getInitialIconForSensor(@NonNull final SensorType type) {
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
            case UNKNOWN: {
                return R.drawable.error_white;
            }
            default: {
                return 0;
            }
        }
    }

    private @DrawableRes int getFinalIconForSensor(@NonNull final SensorType type) {
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
            case UNKNOWN: {
                return R.drawable.error_white;
            }
            default: {
                return 0;
            }
        }
    }
}
