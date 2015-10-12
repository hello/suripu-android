package is.hello.sense.ui.widget;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
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

import is.hello.go99.Anime;
import is.hello.sense.R;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class LoadingView extends LinearLayout {
    public static final long DURATION_DONE_MESSAGE = 1000;

    private final ProgressBar progressBar;
    private boolean doneIconVisible = false;
    private @Nullable String doneText = null;


    //region Lifecycle

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
            layoutTransition.setDuration(type, Anime.DURATION_NORMAL);
            layoutTransition.setStartDelay(type, 0);
        }
        layoutTransition.disableTransitionType(LayoutTransition.DISAPPEARING);
        setLayoutTransition(layoutTransition);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.view_loading, this, true);

        this.progressBar = (ProgressBar) findViewById(R.id.view_loading_progress_bar);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.doneIconVisible = doneIconVisible;
        state.doneText = doneText;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        if (savedState.doneIconVisible) {
            showDoneIcon(false);
        }

        if (savedState.doneText != null) {
            showDoneText(savedState.doneText);
        }
    }

    //endregion


    //region Transitions

    @Override
    public void clearAnimation() {
        super.clearAnimation();

        Anime.cancelAll(progressBar);
    }

    public void dismissProgressBar(boolean animated, @NonNull Runnable onDone) {
        if (animated) {
            animatorFor(progressBar)
                    .withDuration(getLayoutTransition().getDuration(LayoutTransition.DISAPPEARING))
                    .withStartDelay(getLayoutTransition().getStartDelay(LayoutTransition.DISAPPEARING))
                    .scale(0)
                    .fadeOut(GONE)
                    .addOnAnimationCompleted(finished -> {
                        removeView(progressBar);
                        onDone.run();
                    })
                    .start();
        } else {
            removeView(progressBar);
            onDone.run();
        }
    }

    public ImageView showDoneIcon(boolean animated) {
        this.doneIconVisible = true;

        ImageView doneView = new ImageView(getContext());
        doneView.setImageResource(R.drawable.loading_done);

        if (animated) {
            getLayoutTransition().addTransitionListener(new LayoutTransition.TransitionListener() {
                @Override
                public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                    if (view == doneView) {
                        doneView.setScaleX(0f);
                        doneView.setScaleY(0f);
                        animatorFor(doneView)
                                .withDuration(transition.getDuration(transitionType))
                                .withStartDelay(transition.getStartDelay(transitionType))
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
        }

        addView(doneView);
        return doneView;
    }

    public TextView showDoneText(@NonNull String text) {
        Resources resources = getResources();

        TextView textView = new TextView(getContext());
        textView.setTextAppearance(getContext(), R.style.AppTheme_Text_Body);
        textView.setSingleLine();
        textView.setText(text);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.gap_medium);
        addView(textView, layoutParams);

        this.doneText = text;

        return textView;
    }

    public TextView showDoneText(@StringRes int textRes) {
        return showDoneText(getResources().getString(textRes));
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
        dismissProgressBar(true, () -> {
            showDoneIcon(true);
            afterTransition(100, () -> {
                showDoneText(doneTextRes);
                postDelayed(allDoneAction, LoadingView.DURATION_DONE_MESSAGE);
            });
        });
    }

    //endregion


    public static class SavedState extends BaseSavedState {
        boolean doneIconVisible = false;
        @Nullable String doneText = null;

        public SavedState(Parcel in) {
            super(in);

            this.doneIconVisible = in.readByte() != 0;
            this.doneText = in.readString();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }


        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);

            out.writeByte((byte) (doneIconVisible ? 1 : 0));
            out.writeString(doneText);
        }


        public static Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}

