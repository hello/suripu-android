package is.hello.sense.ui.widget;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import is.hello.sense.R;

public class PasswordEditText extends EditText {
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

    private void init() {
        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.secreteye, 0);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (event.getRawX() > getWidth() - getCompoundDrawables()[2].getIntrinsicWidth()) {
                        togglePasswordShowing();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void togglePasswordShowing() {
        if (isPasswordMasked) {
            setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.secreteyehighlighted, 0);
        } else {
            setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.secreteye, 0);
        }
        setSelection(getText().length());
        isPasswordMasked = !isPasswordMasked;
    }
}
