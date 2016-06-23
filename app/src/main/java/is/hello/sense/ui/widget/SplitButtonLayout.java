package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;

public class SplitButtonLayout extends FrameLayout{

    private final Button leftButton;
    private final Button rightButton;
    private final View centerDivider;
    private final View topHorizontalDivider;

    public SplitButtonLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.split_button_layout, this);
        leftButton = (Button) findViewById(R.id.button_left);
        rightButton = (Button) findViewById(R.id.button_right);
        centerDivider = findViewById(R.id.center_divider_vertical);
        topHorizontalDivider = findViewById(R.id.top_divider_horizontal);
        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SplitButtonLayout, 0, 0);
        try {
            final CharSequence leftText = ta.getText(R.styleable.SplitButtonLayout_leftButtonText);
            if(leftText != null){
                leftButton.setText(leftText);
            } else{
                hideLeftButton();
            }

            final CharSequence rightText = ta.getText(R.styleable.SplitButtonLayout_rightButtonText);
            if(rightText != null){
                rightButton.setText(rightText);
            } else{
                hideRightButton();
            }
        } finally {
            ta.recycle();
        }
    }

    public SplitButtonLayout(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SplitButtonLayout(final Context context) {
        this(context, null);
    }

    public void hideLeftButton(){
        hideButton(leftButton);
        expandButton(rightButton);
    }

    public void hideRightButton(){
        hideButton(rightButton);
        expandButton(leftButton);
    }

    public void setLeftButtonOnClickListener(@Nullable final OnClickListener listener){
        setButtonOnClickListener(leftButton, listener);
    }

    public void setRightButtonOnClickListener(@Nullable final OnClickListener listener){
        setButtonOnClickListener(rightButton, listener);
    }

    private void hideButton(@NonNull final Button button){
        button.setVisibility(GONE);
        centerDivider.setVisibility(GONE);
    }

    private void expandButton(@NonNull final Button button){
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                                   ViewGroup.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.BELOW, R.id.top_divider_horizontal);
        button.setLayoutParams(params);
    }

    private void setButtonOnClickListener(@NonNull final Button button, @Nullable final OnClickListener listener){
        if(listener == null){
            button.setOnClickListener(null);
        } else{
            Views.setSafeOnClickListener(button, listener);
        }
    }
}
