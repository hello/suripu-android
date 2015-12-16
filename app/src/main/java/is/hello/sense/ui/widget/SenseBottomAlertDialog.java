package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;

public class SenseBottomAlertDialog extends Dialog {
    private final LinearLayout container;

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

        this.container = (LinearLayout) findViewById(R.id.item_bottom_alert);

        this.titleText = (TextView) container.findViewById(R.id.dialog_bottom_alert);
        this.messageText = (TextView) container.findViewById(R.id.dialog_bottom_alert_message);
        messageText.setMovementMethod(LinkMovementMethod.getInstance());

        this.neutralButton = (Button) container.findViewById(R.id.dialog_bottom_alert_neutral);
        this.positiveButton = (Button) container.findViewById(R.id.dialog_bottom_alert_positive);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Views.runWhenLaidOut(container, () -> {
                 final DisplayMetrics metrics = new DisplayMetrics();
                 getWindow().getWindowManager().getDefaultDisplay().getMetrics(metrics);

                 final Resources resources = getContext().getResources();

                 final int padding = resources.getDimensionPixelSize(R.dimen.gap_outer);
                 final int paddedScreenHeight = metrics.heightPixels - (padding * 2);
                 final int maxDialogHeight = resources.getDimensionPixelSize(R.dimen.dialog_bottom_max_height);
                 final int maxHeight = Math.min(paddedScreenHeight, maxDialogHeight);

                 if (container.getMeasuredHeight() > maxHeight) {
                     container.getLayoutParams().height = maxHeight;
                     container.requestLayout();
                     container.invalidate();
                 }
             });
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
