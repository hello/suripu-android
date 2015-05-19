package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;

public class SenseBottomAlertDialog extends Dialog {
    private final TextView titleText;
    private final TextView messageText;

    private final Button neutralButton;
    private final Button positiveButton;

    public SenseBottomAlertDialog(@NonNull Context context) {
        super(context, R.style.AppTheme_Dialog_BottomAlert);

        setContentView(R.layout.dialog_bottom_alert);
        setCancelable(true);

        Window window = getWindow();
        window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

        this.titleText = (TextView) findViewById(R.id.dialog_bottom_alert);
        this.messageText = (TextView) findViewById(R.id.dialog_bottom_alert_message);

        this.neutralButton = (Button) findViewById(R.id.dialog_bottom_alert_neutral);
        this.positiveButton = (Button) findViewById(R.id.dialog_bottom_alert_positive);
    }

    //region Text

    @Override
    public void setTitle(@Nullable CharSequence title) {
        super.setTitle(title);

        titleText.setText(title);
    }

    public void setMessage(@Nullable CharSequence message) {
        messageText.setText(message);
    }

    public void setMessage(@StringRes int messageId) {
        setMessage(getContext().getString(messageId));
    }

    //endregion


    //region Buttons

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

    public void setPositiveButton(@Nullable CharSequence title, @Nullable OnClickListener onClickListener) {
        positiveButton.setText(title);
        Views.setSafeOnClickListener(positiveButton, createClickListener(onClickListener, DialogInterface.BUTTON_POSITIVE));
    }

    public void setPositiveButton(@StringRes int titleId, @Nullable OnClickListener onClickListener) {
        setPositiveButton(getContext().getString(titleId), onClickListener);
    }

    public void setNeutralButton(@Nullable CharSequence title, @Nullable OnClickListener onClickListener) {
        neutralButton.setText(title);
        Views.setSafeOnClickListener(neutralButton, createClickListener(onClickListener, DialogInterface.BUTTON_NEUTRAL));
    }

    public void setNeutralButton(@StringRes int titleId, @Nullable OnClickListener onClickListener) {
        setNeutralButton(getContext().getString(titleId), onClickListener);
    }

    //endregion
}
