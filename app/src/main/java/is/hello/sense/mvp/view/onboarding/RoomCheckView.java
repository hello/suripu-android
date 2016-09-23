package is.hello.sense.mvp.view.onboarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorContext;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.go99.animators.OnAnimationCompleted;
import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorType;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.SensorConditionView;
import is.hello.sense.ui.widget.SensorTickerView;
import is.hello.sense.ui.widget.util.Views;

import static is.hello.go99.Anime.cancelAll;
import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class RoomCheckView extends PresenterView {
    private final ImageView sense;
    private final LinearLayout sensorViewContainer;
    private final LinearLayout dynamicContent;
    private final TextView status;
    private final SensorTickerView scoreTicker;
    private final Drawable graySense;

    private final int startColor;

    private @Nullable
    SensorConditionView animatingSensorView;
    private @Nullable
    ValueAnimator scoreAnimator;

    private final AnimatorContext animatorContext;
    private TimeInterpolator sensorContainerInterpolator;
    private final Resources resources;

    public RoomCheckView(@NonNull final Activity activity,
                         @NonNull final AnimatorContext animatorContext){
        super(activity);

        this.sense = (ImageView) findViewById(R.id.fragment_onboarding_room_check_sense);
        this.sensorViewContainer = (LinearLayout) findViewById(R.id.fragment_onboarding_room_check_sensors);
        this.dynamicContent = (LinearLayout) findViewById(R.id.fragment_onboarding_room_check_content);
        this.status = (TextView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_status);
        this.scoreTicker = (SensorTickerView) dynamicContent.findViewById(R.id.fragment_onboarding_room_check_ticker);
        scoreTicker.setAnimatorContext(animatorContext);
        this.animatorContext = animatorContext;
        this.sensorContainerInterpolator = new OvershootInterpolator(1.0f);

        this.resources = getResources();
        this.graySense = ResourcesCompat.getDrawable(resources, R.drawable.onboarding_sense_grey, null);
        this.startColor = ContextCompat.getColor(context, Condition.ALERT.colorRes);

        this.sensorViewContainer.setClickable(false);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_onboarding_room_check;
    }

    @Override
    public void releaseViews() {
        sensorContainerInterpolator = null;
        animatingSensorView = null;
        scoreAnimator = null;
    }

    public void setSensorContainerInterpolator(@NonNull final TimeInterpolator interpolator){
        this.sensorContainerInterpolator = interpolator;
    }

    //todo specifically for onboarding
    public void setSensorContainerXOffset() {
        sensorViewContainer.post( () -> {
            sensorViewContainer.setX(sense.getX() + sense.getWidth() / 2
                                             - resources.getDimensionPixelSize(R.dimen.item_room_sensor_condition_view_width) );
            sensorViewContainer.invalidate();
        });
    }

    public void updateSensorView(final List<Sensor> sensors) {
        for (int i = 0; i < sensors.size(); i++) {
            final SensorConditionView sensorView = (SensorConditionView) sensorViewContainer.getChildAt(i);
            final Sensor sensor = sensors.get(i);
            sensorView.clearAnimation();
            sensorView.setTint(ContextCompat.getColor(context, sensor.getCondition().colorRes));
            sensorView.setIcon(getFinalIconForSensor(sensor.getType()));
        }
    }

    public void createSensorConditionViews(final List<SensorType> types) {
        sensorViewContainer.removeAllViews();

        final Resources resources = getResources();
        final LinearLayout.LayoutParams layoutParams
                = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int padding = resources.getDimensionPixelOffset(R.dimen.item_room_sensor_condition_view_width) / 2;
        for (final SensorType type : types) {
            final SensorConditionView conditionView = new SensorConditionView(context);
            final int iconForSensor = getInitialIconForSensor(type);
            final Drawable iconDrawable = ResourcesCompat.getDrawable(resources, iconForSensor, null);
            conditionView.setIcon(iconDrawable);
            conditionView.setAnimatorContext(animatorContext);
            conditionView.setPadding(padding,0,padding,0);
            sensorViewContainer.addView(conditionView, layoutParams);
        }
    }

    public void showConditionAt(final int position,
                                final SensorType sensorType,
                                final String statusMessage,
                                final Condition condition,
                                final int convertedUnitTickerValue,
                                final String unitSuffix,
                                final Runnable onComplete) {
        final SensorConditionView sensorView = (SensorConditionView) sensorViewContainer.getChildAt(position);

        if(position != 0) {
            animatorFor(sensorViewContainer, animatorContext)
                    .withInterpolator(sensorContainerInterpolator)
                    .translationX(sensorViewContainer.getTranslationX() - sensorView.getWidth())
                    .start();
        }

        status.setText(getStatusStringForSensor(sensorType));
        this.animatingSensorView = sensorView;
        sensorView.fadeInProgressIndicator(() -> {

            final long duration = scoreTicker.animateToValue(convertedUnitTickerValue,
                                                             unitSuffix,
                                                             finishedTicker -> {
                if (!finishedTicker) {
                    return;
                }

                animatorFor(status, animatorContext)
                        .fadeOut(View.VISIBLE)
                        .addOnAnimationCompleted(finishedStatus -> {
                            if (!finishedStatus) {
                                return;
                            }

                            status.setText(null);
                            status.setTransformationMethod(null);
                            status.setText(statusMessage);

                            animateSenseCondition(condition, false);
                            sensorView.transitionToIcon(getFinalIconForSensor(sensorType), onComplete);

                            animatorFor(status, animatorContext)
                                    .fadeIn()
                                    .start();
                        })
                        .start();
            });

            final int endColor = ContextCompat.getColor(context, condition.colorRes);
            this.scoreAnimator = AnimatorTemplate.DEFAULT.createColorAnimator(startColor, endColor);
            scoreAnimator.setDuration(duration);
            scoreAnimator.addUpdateListener(a -> {
                final int color = (int) a.getAnimatedValue();
                sensorView.setTint(color);
                scoreTicker.setTextColor(color);
            });
            scoreAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(final Animator animation) {
                    sensorView.setTint(endColor);
                    scoreTicker.setTextColor(endColor);
                }

                @Override
                public void onAnimationEnd(final Animator animation) {
                    RoomCheckView.this.scoreAnimator = null;
                }
            });
            scoreAnimator.start();
        });
    }

    public void showCompletion(final boolean animate,
                               final Condition condition,
                               final OnClickListener onContinueClickListener) {
        if(condition != null) {
            if (animate) {
                animateSenseCondition(condition, true);
            } else {
                sense.setImageDrawable(getConditionDrawable(condition));
            }
        }

        final LayoutInflater inflater = LayoutInflater.from(context);
        final OnAnimationCompleted atEnd = finishedTransitionIn -> {
            if (!finishedTransitionIn) {
                return;
            }

            final Button continueButton = (Button) dynamicContent.findViewById(R.id.sub_fragment_room_check_end_continue);
            Views.setSafeOnClickListener(continueButton, onContinueClickListener);
        };
        if (animate) {
            animatorFor(dynamicContent, animatorContext)
                    .fadeOut(View.INVISIBLE)
                    .addOnAnimationCompleted(onCompleted -> {
                        if(onCompleted){
                            dynamicContent.removeAllViews();
                            inflater.inflate(R.layout.sub_fragment_onboarding_room_check_end_message, dynamicContent, true);

                            animatorFor(dynamicContent, animatorContext)
                                    .fadeIn()
                                    .addOnAnimationCompleted(atEnd)
                                    .postStart();
                        }
                    }).start();

        } else {
            dynamicContent.removeAllViews();
            inflater.inflate(R.layout.sub_fragment_onboarding_room_check_end_message, dynamicContent, true);
            atEnd.onAnimationCompleted(true);
        }
    }

    public void stopAnimations() {
        scoreTicker.stopAnimating();
        cancelAll(status, dynamicContent, sensorViewContainer);

        if (scoreAnimator != null) {
            scoreAnimator.cancel();
        }

        if (animatingSensorView != null) {
            animatingSensorView.clearAnimation();
        }
    }

    public void removeSensorContainerViews() {
        sensorViewContainer.removeAllViews();
    }

    private @StringRes
    int getStatusStringForSensor(@NonNull final SensorType type) {
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

    //region Animating Sense

    public void animateSenseToGray() {
        final Drawable senseDrawable = sense.getDrawable();
        if (senseDrawable instanceof TransitionDrawable) {
            ((TransitionDrawable) senseDrawable).reverseTransition(Anime.DURATION_NORMAL);
        }
    }

    private Drawable getConditionDrawable(@NonNull final Condition condition) {
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
                return graySense;
            }
        }
    }

    private void animateSenseCondition(@NonNull Condition condition, boolean fromCurrent) {
        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[] {
                fromCurrent ? sense.getDrawable() : graySense,
                getConditionDrawable(condition),
        });
        transitionDrawable.setCrossFadeEnabled(true);
        sense.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(Anime.DURATION_NORMAL);
    }

    //endregion
}
