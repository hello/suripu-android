package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.fragments.onboarding.RegisterFragment;
import is.hello.sense.ui.widget.util.Styles;

public class LabelEditText extends RelativeLayout {
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final String DASH = " - ";

    private final EditText input;
    private final TextView label;

    private final String labelText;

    public LabelEditText(Context context) {
        this(context, null);
    }

    public LabelEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LabelEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_label_edit_text, this);
        label = (TextView) findViewById(R.id.view_label_edit_text_label);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LabelEditText, 0, 0);
        try {
            if (ta.getBoolean(R.styleable.LabelEditText_isPassword, false)) {
                input = (EditText) findViewById(R.id.view_label_edit_text_input_password);
                findViewById(R.id.view_label_edit_text_input).setVisibility(GONE);
            } else {
                input = (EditText) findViewById(R.id.view_label_edit_text_input);
                findViewById(R.id.view_label_edit_text_input_password).setVisibility(GONE);
            }
            final String inputText = ta.getString(R.styleable.LabelEditText_inputText);
            labelText = ta.getString(R.styleable.LabelEditText_labelText);
            label.setText(labelText);
            input.setText(inputText);
        } finally {
            ta.recycle();
        }

        int value = attrs.getAttributeIntValue(ANDROID_NAMESPACE, "nextFocusForward", -1);
        if (value != -1) {
            input.setNextFocusForwardId(value);
        }

        value = attrs.getAttributeIntValue(ANDROID_NAMESPACE, "inputType", -1);
        if (value != -1) {
            input.setInputType(value);
        }

        value = attrs.getAttributeIntValue(ANDROID_NAMESPACE, "imeOptions", -1);
        if (value != -1) {
            input.setImeOptions(value);
        }

        boolean selectAllOnFocus = attrs.getAttributeBooleanValue(ANDROID_NAMESPACE, "selectAllOnFocus", false);
        input.setSelectAllOnFocus(selectAllOnFocus);

    }

    public void setInputText(@Nullable final String inputText) {
        input.setText(inputText);
    }

    public void setLabelText(@Nullable final String labelText) {
        label.setText(labelText);
    }

    public String getInputText() {
        return input.getText().toString();
    }

    public String getLabelText() {
        return label.getText().toString();
    }

    public void setOnEditorActionListener(@NonNull final TextView.OnEditorActionListener l) {
        input.setOnEditorActionListener(l);
    }

    public void removeError() {
        label.setText(labelText);
        Styles.setTextAppearance(label, R.style.Label);
        input.setBackgroundResource(R.drawable.edit_text_background_selector);
    }

    public void setError(@StringRes int stringRes) {
        setError(getContext().getString(stringRes));
    }

    public void setError(@Nullable String error) {
        if (error != null && !error.isEmpty()) {
            label.setText(String.format("%s %s %s", labelText, DASH, error));
        }
        Styles.setTextAppearance(label, R.style.Label_Error);
        input.setBackgroundResource(R.drawable.edit_text_background_error);
    }

    public void addTextChangedListener(TextWatcher tw) {
        input.addTextChangedListener(tw);
    }

    public void removeTextChangedListener(TextWatcher tw) {
        input.removeTextChangedListener(tw);
    }

    public boolean isInputEmpty() {
        return input.getText().toString().trim().isEmpty();
    }
}
