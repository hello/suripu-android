package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.Arrays;
import java.util.List;

import is.hello.sense.R;

public class TickerView extends LinearLayout {
    private static final List<String> NUMBERS = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    private static final int NUMBER_VIEWS = 3;

    private NumberView[] numberViews = new NumberView[NUMBER_VIEWS];

    //region Lifecycle

    public TickerView(@NonNull Context context) {
        this(context, null);
    }

    public TickerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TickerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);

        for (int i = 0; i < NUMBER_VIEWS; i++) {
            NumberView numberView = new NumberView(context);
            addView(numberView);
            this.numberViews[i] = numberView;
        }
    }

    //endregion


    public void setColor(int color) {

    }

    public void setValue(@NonNull String value) {
        int length = value.length();
        if (length > NUMBER_VIEWS) {
            throw new IllegalArgumentException("length " + length + " exceeds allowed " + NUMBER_VIEWS);
        }

        int start = NUMBER_VIEWS - length;
        for (int i = start; i < NUMBER_VIEWS; i++) {
            int valuePosition = i - start;
            numberViews[i].setDigit(value.substring(valuePosition, valuePosition + 1));
        }
    }


    private class NumberView extends ViewFlipper implements Animation.AnimationListener {
        private NumberView(@NonNull Context context) {
            super(context);

            LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                                         LayoutParams.WRAP_CONTENT);
            for (String number : NUMBERS) {
                TextView textView = new TextView(context);
                textView.setTextAppearance(context, R.style.AppTheme_Text_BigScore);
                textView.setText(number);
                addView(textView, layoutParams);
            }

            TranslateAnimation slideIn = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 0f,
                    Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 1f
            );
            AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
            AnimationSet inAnimation = new AnimationSet(true);
            inAnimation.addAnimation(slideIn);
            inAnimation.addAnimation(fadeIn);
            inAnimation.setDuration(1000);
            setInAnimation(slideIn);

            TranslateAnimation slideOut = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 0f,
                    Animation.RELATIVE_TO_PARENT, 1f, Animation.RELATIVE_TO_PARENT, 0f
            );
            AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
            AnimationSet outAnimation = new AnimationSet(true);
            outAnimation.addAnimation(fadeOut);
            outAnimation.addAnimation(slideOut);
            outAnimation.setDuration(1000);
            outAnimation.setAnimationListener(this);
            setOutAnimation(outAnimation);

            setDisplayedChild(0);
        }

        void setDigit(@NonNull String digit) {
            int childIndex = NUMBERS.indexOf(digit);
            setDisplayedChild(childIndex);
        }

        //region Listener

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }

        //endregion
    }
}
