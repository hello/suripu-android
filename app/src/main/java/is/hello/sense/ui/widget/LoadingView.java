package is.hello.sense.ui.widget;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.animation.PropertyAnimatorProxy;

public class LoadingView extends LinearLayout {
    public static final long DURATION_DONE_MESSAGE = 2 * 1000;

    private final ProgressBar progressBar;

    public LoadingView(@NonNull Context context) {
        this(context, null);
    }

    public LoadingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setGravity(Gravity.CENTER);
        setOrientation(VERTICAL);
        LayoutTransition layoutTransition = new LayoutTransition();
        for (int type = LayoutTransition.CHANGE_APPEARING; type <= LayoutTransition.CHANGING; type++) {
            layoutTransition.setDuration(type, Animation.DURATION_NORMAL);
            layoutTransition.setStartDelay(type, 0);
        }
        layoutTransition.disableTransitionType(LayoutTransition.DISAPPEARING);
        setLayoutTransition(layoutTransition);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.view_loading, this, true);

        this.progressBar = (ProgressBar) findViewById(R.id.view_loading_progress_bar);
    }


    //region Transitions

    public void animateOutProgressBar(@NonNull Runnable onDone) {
        PropertyAnimatorProxy.animate(progressBar)
                .setDuration(getLayoutTransition().getDuration(LayoutTransition.DISAPPEARING))
                .setStartDelay(getLayoutTransition().getStartDelay(LayoutTransition.DISAPPEARING))
                .scale(0)
                .fadeOut(GONE)
                .addOnAnimationCompleted(finished -> {
                    removeView(progressBar);
                    onDone.run();
                })
                .start();
    }

    public ImageView showDoneIcon() {
        ImageView doneView = new ImageView(getContext());
        doneView.setImageResource(R.drawable.loading_done);
        getLayoutTransition().addTransitionListener(new LayoutTransition.TransitionListener() {
            @Override
            public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                if (view == doneView) {
                    doneView.setScaleX(0f);
                    doneView.setScaleY(0f);
                    PropertyAnimatorProxy.animate(doneView)
                            .setDuration(transition.getDuration(transitionType))
                            .setStartDelay(transition.getStartDelay(transitionType))
                            .scale(1f)
                            .start();
                }
            }

            @Override
            public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                if (view == doneView) {
                    transition.removeTransitionListener(this);
                }
            }
        });
        addView(doneView);
        return doneView;
    }

    public TextView showDoneText(@NonNull CharSequence text) {
        Resources resources = getResources();

        TextView textView = new TextView(getContext());
        textView.setTextAppearance(getContext(), R.style.AppTheme_Text_Body_Thin);
        textView.setSingleLine();
        textView.setText(text);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.gap_large);
        addView(textView, layoutParams);

        return textView;
    }

    public TextView showDoneText(@StringRes int textRes) {
        return showDoneText(getResources().getText(textRes));
    }

    public void afterTransition(long extraDelay, @NonNull Runnable action) {
        getLayoutTransition().addTransitionListener(new LayoutTransition.TransitionListener() {
            @Override
            public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            }

            @Override
            public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                transition.removeTransitionListener(this);
                postDelayed(action, extraDelay);
            }
        });
    }

    public void playDoneTransition(@StringRes int doneTextRes, @NonNull Runnable allDoneAction) {
        animateOutProgressBar(() -> {
            showDoneIcon();
            afterTransition(100, () -> {
                showDoneText(doneTextRes);
                postDelayed(allDoneAction, LoadingView.DURATION_DONE_MESSAGE);
            });
        });
    }

    //endregion
}

