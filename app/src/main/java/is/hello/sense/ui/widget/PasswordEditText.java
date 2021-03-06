package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import is.hello.sense.R;

public class PasswordEditText extends AppCompatEditText
        implements View.OnTouchListener, View.OnFocusChangeListener{
    private final static int HIDDEN_INPUT_TYPE = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    private final static int VISIBLE_INPUT_TYPE = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    private final static int HIDDEN_ICON = R.drawable.secreteye;
    private final static int VISIBLE_ICON = R.drawable.secreteyehighlighted;
    private boolean isPasswordMasked = true;

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
            throw new Error("Cannot override internal focusChangedListener for " + PasswordEditText.class.getSimpleName());
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
