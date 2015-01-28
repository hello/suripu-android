package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;

public class SenseAlertDialog extends Dialog {
    private TextView titleText;
    private TextView messageText;

    private View buttonDivider;
    private Button negativeButton;
    private Button positiveButton;

    public SenseAlertDialog(Context context) {
        super(context, R.style.AppTheme_Dialog_Simple);
        initialize();
    }

    protected void initialize() {
        setContentView(R.layout.dialog_sense_alert);

        this.titleText = (TextView) findViewById(R.id.dialog_sense_alert_title);
        this.messageText = (TextView) findViewById(R.id.dialog_sense_alert_message);

        this.buttonDivider = findViewById(R.id.dialog_sense_alert_button_divider);
        this.negativeButton = (Button) findViewById(R.id.dialog_sense_alert_cancel);
        this.positiveButton = (Button) findViewById(R.id.dialog_sense_alert_ok);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        titleText.setText(title);
    }

    @Override
    public void setTitle(@StringRes int titleId) {
        super.setTitle(titleId);
        titleText.setText(titleId);
    }

    public void setIcon(@Nullable Drawable icon) {
        titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
    }

    public void setIcon(@DrawableRes int iconId) {
        titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(iconId, 0, 0, 0);
    }

    public void setMessage(CharSequence message) {
        messageText.setText(message);
    }

    public void setMessage(@StringRes int messageId) {
        messageText.setText(messageId);
    }

    private View.OnClickListener createClickListener(@Nullable DialogInterface.OnClickListener onClickListener, int which) {
        if (onClickListener != null) {
            return view -> {
                onClickListener.onClick(this, which);
                dismiss();
            };
        } else {
            return view -> dismiss();
        }
    }

    private void updateButtonDivider() {
        if (positiveButton.getVisibility() == View.VISIBLE && negativeButton.getVisibility() == View.VISIBLE) {
            buttonDivider.setVisibility(View.VISIBLE);
        } else {
            buttonDivider.setVisibility(View.GONE);
        }
    }

    public void setPositiveButton(@Nullable CharSequence title, @Nullable OnClickListener onClickListener) {
        if (title != null) {
            positiveButton.setVisibility(View.VISIBLE);
            positiveButton.setText(title);
            Views.setSafeOnClickListener(positiveButton, createClickListener(onClickListener, DialogInterface.BUTTON_POSITIVE));
        } else {
            positiveButton.setVisibility(View.GONE);
        }

        updateButtonDivider();
    }

    public void setPositiveButton(@StringRes int titleId, @Nullable OnClickListener onClickListener) {
        setPositiveButton(getContext().getString(titleId), onClickListener);
    }

    public void setNegativeButton(@Nullable CharSequence title, @Nullable OnClickListener onClickListener) {
        if (title != null) {
            negativeButton.setVisibility(View.VISIBLE);
            negativeButton.setText(title);
            Views.setSafeOnClickListener(negativeButton, createClickListener(onClickListener, DialogInterface.BUTTON_NEGATIVE));
        } else {
            negativeButton.setVisibility(View.GONE);
        }

        updateButtonDivider();
    }

    public void setNegativeButton(@StringRes int titleId, @Nullable OnClickListener onClickListener) {
        setNegativeButton(getContext().getString(titleId), onClickListener);
    }

    public void setDestructive(boolean isDestructive) {
        if (isDestructive) {
            positiveButton.setTextColor(getContext().getResources().getColor(R.color.destructive_accent));
        } else {
            positiveButton.setTextColor(getContext().getResources().getColor(R.color.light_accent));
        }
    }
}
