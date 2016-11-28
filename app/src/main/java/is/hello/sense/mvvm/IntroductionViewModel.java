package is.hello.sense.mvvm;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.ObservableFloat;
import android.databinding.ObservableInt;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.ui.common.FragmentNavigation;

import static is.hello.sense.ui.fragments.onboarding.IntroductionFragment.RESPONSE_GET_STARTED;
import static is.hello.sense.ui.fragments.onboarding.IntroductionFragment.RESPONSE_SIGN_IN;

/**
 * Known issue
 * do not name the data variable in xml same camelCased name as class
 * example do not name this as introductionViewModel
 */
public class IntroductionViewModel{

    private final IntroductionModel model;
    private final FragmentNavigation router;

    public final ObservableInt observableStatusBarColor;
    public final ObservableFloat observableLoginButtonWeight;
    public final ObservableFloat observableLoginButtonAlpha;

    @ColorInt
    public final int introStatusBarColor;
    @ColorInt
    public final int featureStatusBarColor;

    public IntroductionViewModel(@NonNull final Context context,
                                 @NonNull final IntroductionModel model,
                                 @NonNull final FragmentNavigation router){
        this.model = model;
        this.router = router;
        this.introStatusBarColor = ContextCompat.getColor(context, R.color.status_bar_grey);
        this.featureStatusBarColor = ContextCompat.getColor(context, R.color.light_accent_darkened);
        this.observableStatusBarColor = new ObservableInt(introStatusBarColor);
        this.observableLoginButtonWeight = new ObservableFloat(1f);
        this.observableLoginButtonAlpha = new ObservableFloat(1f);
    }

    public void onDestroy(){
        this.observableStatusBarColor.removeOnPropertyChangedCallback(null);
    }

    @StringRes
    public int getLoginText(){
        return model.loginText;
    }

    @StringRes
    public int getGettingStartedText(){
        return model.gettingStartedText;
    }

    @ColorInt
    public int getStatusBarColor(){
        return observableStatusBarColor.get();
    }

    public void setStatusBarColor(@ColorInt final int color){
        observableStatusBarColor.set(color);
    }

    public void onLoginButtonClicked(@NonNull final View ignoredView){
        router.flowFinished(null, RESPONSE_SIGN_IN, null);
    }

    public void onGettingStartedButtonClicked(){
        router.flowFinished(null, RESPONSE_GET_STARTED, null);
    }

    public void updateLoginButtonWeight(final float weight){
        observableLoginButtonWeight.set(weight);
    }

    public void updateLoginButtonAlpha(final float alpha){
        observableLoginButtonAlpha.set(alpha);
    }

    public void updateStatusBarColor(final float offset) {
        @ColorInt
        final int color = Anime.interpolateColors(offset,
                                                  introStatusBarColor,
                                                  featureStatusBarColor);
        setStatusBarColor(color);
    }

    @BindingAdapter("android:layout_weight")
    /**
     * Not all xml attributes have predefined setters so custom static setters are required to be used
     * otherwise build will fail.
     */
    public static void setLayoutWeight(@NonNull final View view, final float weight){
        ((LinearLayout.LayoutParams) view.getLayoutParams()).weight = weight;
        // To avoid extra layouts immediately after inflate
        if (!view.isInLayout()) {
            view.requestLayout();
        }
    }
}
