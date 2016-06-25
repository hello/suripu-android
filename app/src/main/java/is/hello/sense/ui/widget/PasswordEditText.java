package is.hello.sense.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import is.hello.sense.R;

public class PasswordEditText extends EditText
        implements View.OnTouchListener, View.OnFocusChangeListener{
    private boolean isPasswordMasked = true;
    private final int HIDDEN_INPUT_TYPE = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    private final int VISIBLE_INPUT_TYPE = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
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

    @SuppressLint("ClickableViewAccessibility")
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

    /**
     * When this component is not in focus, it should be in a masked state.
     * Likewise, when in focus, it becomes visible.
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        setIsPasswordMasked(!hasFocus);
    }

    @Override
    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        if (!l.equals(this)) {
            throw new Error("Don't set focus on PasswordEditText");
        }
        super.setOnFocusChangeListener(this);
    }

    private void init() {
        setIsPasswordMasked(true);
        setOnTouchListener(this);
        setOnFocusChangeListener(this);
    }


    private void setIsPasswordMasked(final boolean isPasswordMasked){
        this.isPasswordMasked = isPasswordMasked;
        setInputType();
        setDrawableIcon();
        setSelection(getText().length());
    }

    private void togglePasswordShowing() {
        setIsPasswordMasked(!isPasswordMasked);
    }

    private void setInputType(){
        if (isPasswordMasked) {
            setInputType(HIDDEN_INPUT_TYPE);
        } else {
            setInputType(VISIBLE_INPUT_TYPE);
        }
    }
    private void setDrawableIcon(){
        final int drawableIcon = isPasswordMasked ? HIDDEN_ICON : VISIBLE_ICON;
        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, drawableIcon, 0);
    }
}
