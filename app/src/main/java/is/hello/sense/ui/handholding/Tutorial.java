package is.hello.sense.ui.handholding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.DimenRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Property;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.segment.analytics.Properties;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.InternalPrefManager;

import static is.hello.sense.util.Analytics.Breadcrumb.Description;
import static is.hello.sense.util.Analytics.Breadcrumb.EVENT_NAME;
import static is.hello.sense.util.Analytics.Breadcrumb.Source;

public enum Tutorial {
    SWIPE_TIMELINE(R.string.tutorial_swipe_timeline,
                   Gravity.BOTTOM,
                   R.id.fragment_timeline_recycler,
                   Interaction.SWIPE_RIGHT,
                   Analytics.createBreadcrumbTrackingProperties(Source.TIMELINE,
                                                                Description.SWIPE_TIMELINE)),
    ZOOM_OUT_TIMELINE(R.string.tutorial_zoom_out_timeline,
                      Gravity.BOTTOM,
                      R.id.view_timeline_toolbar_title,
                      Interaction.TAP,
                      Analytics.createBreadcrumbTrackingProperties(Source.TIMELINE,
                                                                   Description.ZOOM_OUT_TIMELINE)),
    SCRUB_SENSOR_HISTORY(R.string.tutorial_scrub_sensor_history,
                         Gravity.TOP,
                         R.id.fragment_sensor_history_graph,
                         Interaction.SWIPE_LEFT,
                         Analytics.createBreadcrumbTrackingProperties(Source.SENSOR_HISTORY,
                                                                      Description.SCRUB_SENSOR_HISTORY)),
    TAP_INSIGHT_CARD(R.string.tutorial_tap_insight_card,
                     Gravity.BOTTOM,
                     R.id.item_insight_card,
                     Interaction.TAP,
                     Analytics.createBreadcrumbTrackingProperties(Source.INSIGHTS,
                                                                  Description.TAP_INSIGHT_CARD)),
    TAP_HAMBURGER(R.string.tutorial_tap_hamburger,
                  Gravity.BOTTOM,
                  R.id.view_timeline_toolbar_overflow,
                  Interaction.TAP,
                  Analytics.createBreadcrumbTrackingProperties(Source.TIMELINE,
                                                               Description.TAP_HAMBURGER)),
    TAP_NAME(R.string.tutorial_tap_name,
             Gravity.BOTTOM,
             R.id.fragment_account_settings_name,
             Interaction.TAP,
             Analytics.createBreadcrumbTrackingProperties(Source.ACCOUNT,
                                                          Description.TAP_NAME));

    public final
    @StringRes
    int descriptionRes;
    public final int descriptionGravity;
    public final
    @IdRes
    int anchorId;
    public final Interaction interaction;
    public final Properties properties;

    Tutorial(@StringRes final int descriptionRes,
             final int descriptionGravity,
             @IdRes final int anchorId,
             @NonNull final Interaction interaction,
             @NonNull final Properties properties) {
        this.descriptionRes = descriptionRes;
        this.descriptionGravity = descriptionGravity;
        this.anchorId = anchorId;
        this.interaction = interaction;
        this.properties = properties;
    }


    public String getShownKey() {
        return "tutorial_" + toString().toLowerCase(Locale.getDefault()) + "_shown";
    }

    public boolean shouldShow(@NonNull final Activity activity) {
        final SharedPreferences preferences =
                activity.getSharedPreferences(getPrefName(activity), 0);
        return (!preferences.getBoolean(getShownKey(), false) &&
                activity.findViewById(TutorialOverlayView.ROOT_CONTAINER_ID) == null);
    }

    public void wasDismissed(@NonNull final Context context) {
        markShown(context);
        Analytics.trackEvent(EVENT_NAME, properties);
    }

    public void markShown(@NonNull final Context context) {
        final SharedPreferences preferences =
                context.getSharedPreferences(getPrefName(context), 0);
        preferences.edit()
                   .putBoolean(getShownKey(), true)
                   .apply();
    }

    private static String getPrefName(@NonNull final Context context) {
        return Constants.HANDHOLDING_PREFS + InternalPrefManager.getAccountId(context);
    }


    //region Vending Animations

    public static Animator createPulseAnimation(@NonNull final View view) {
        final ValueAnimator animator = ValueAnimator.ofFloat(1f, 0.8f, 1f);
        animator.setStartDelay(350);
        animator.setDuration(1250);
        animator.setInterpolator(new AnticipateOvershootInterpolator(3.0f, 1.5f));
        animator.addUpdateListener(a -> {
            final float scale = (float) a.getAnimatedValue();
            view.setScaleX(scale);
            view.setScaleY(scale);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            boolean canceled = false;

            @Override
            public void onAnimationCancel(final Animator animation) {
                this.canceled = true;
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                if (!canceled) {
                    animation.start();
                }
            }
        });
        return animator;
    }

    public static Animator createSlideAnimation(@NonNull final View view,
                                                @DimenRes final int deltaRes,
                                                final boolean isVertical) {
        final Property<View, Float> property;
        if (isVertical) {
            property = Property.of(View.class, float.class, "translationY");
        } else {
            property = Property.of(View.class, float.class, "translationX");
        }

        final long duration = 750;
        final TimeInterpolator interpolator = new AccelerateDecelerateInterpolator();

        final float delta = view.getResources().getDimension(deltaRes);
        final ObjectAnimator slide = ObjectAnimator.ofFloat(view, property, 0f, delta);
        slide.setInterpolator(interpolator);
        slide.setDuration(duration);

        final ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        fadeOut.setInterpolator(interpolator);
        fadeOut.setDuration(duration);

        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        fadeIn.setInterpolator(new LinearInterpolator());
        fadeIn.setDuration(Anime.DURATION_SLOW);

        final AnimatorSet slideAndFadeIn = new AnimatorSet();
        slideAndFadeIn.setStartDelay(150);
        slideAndFadeIn.play(slide)
                      .with(fadeOut)
                      .before(fadeIn);

        final AtomicBoolean canceled = new AtomicBoolean(false);
        slideAndFadeIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(final Animator animation) {
                canceled.set(true);
                fadeIn.cancel();
            }
        });

        fadeIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                if (!canceled.get()) {
                    slideAndFadeIn.start();
                }
            }

            @Override
            public void onAnimationStart(final Animator animation) {
                property.set(view, 0f);
            }
        });

        return slideAndFadeIn;
    }

    public Animator createAnimation(@NonNull final View view) {
        switch (interaction) {
            case TAP:
                return createPulseAnimation(view);

            case SWIPE_LEFT:
                return createSlideAnimation(view, R.dimen.interaction_slide_negative, interaction.isVertical);

            case SWIPE_RIGHT:
                return createSlideAnimation(view, R.dimen.interaction_slide_positive, interaction.isVertical);

            case SWIPE_UP:
                return createSlideAnimation(view, R.dimen.interaction_slide_negative, interaction.isVertical);

            case SWIPE_DOWN:
                return createSlideAnimation(view, R.dimen.interaction_slide_positive, interaction.isVertical);

            default:
                throw new IllegalStateException();
        }
    }

    //endregion


    public LayoutParams generateDescriptionLayoutParams() {
        final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                           LayoutParams.WRAP_CONTENT);
        switch (descriptionGravity) {
            case Gravity.TOP: {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                break;
            }
            case Gravity.BOTTOM: {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown gravity constant " + descriptionGravity);
            }
        }
        return layoutParams;
    }
}
