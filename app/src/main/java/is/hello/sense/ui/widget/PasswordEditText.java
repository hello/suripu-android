package is.hello.sense.ui.widget;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import is.hello.sense.R;

public class PasswordEditText extends EditText implements View.OnTouchListener{
    private boolean isPasswordMasked = true;
    private final int HIDDEN_INPUT_TYPE = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    private final int VISIBLE_INPUT_TYPE = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    private final int HIDDEN_ICON = R.drawable.secreteye;
    private final int VISIBLE_ICON = R.drawable.secreteyehighlighted;

    public PasswordEditText(Context context) {
        super(context);
        init();
    }

    public PasswordEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PasswordEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        else if (event.getRawX() > getWidth() - getCompoundDrawables()[2].getIntrinsicWidth()) {
                togglePasswordShowing();
                return true;
        } else{
            return false;
        }
    }

    private void init() {
        setInputType(isPasswordMasked);
        setDrawableIcon(isPasswordMasked);
        setOnTouchListener(this);
    }

    private void togglePasswordShowing() {
        isPasswordMasked = !isPasswordMasked;
        setInputType(isPasswordMasked);
        setDrawableIcon(isPasswordMasked);
        setSelection(getText().length());
    }

    private void setInputType(boolean isPasswordMasked){
        if (isPasswordMasked) {
            setInputType(HIDDEN_INPUT_TYPE);
        } else {
            setInputType(VISIBLE_INPUT_TYPE);
        }
    }
    private void setDrawableIcon(boolean isPasswordMasked){
        final int drawableIcon = isPasswordMasked ? HIDDEN_ICON : VISIBLE_ICON;
        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, drawableIcon, 0);
    }
}
