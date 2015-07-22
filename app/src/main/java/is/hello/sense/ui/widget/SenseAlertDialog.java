package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Logger;

public class SenseAlertDialog extends Dialog {
    private final LinearLayout container;

    private final TextView titleText;
    private final TextView messageText;

    private final View buttonDivider;
    private final Button negativeButton;
    private final Button positiveButton;

    private View view;
    private View topViewDivider, bottomViewDivider;

    public SenseAlertDialog(@NonNull Context context) {
        super(context, R.style.AppTheme_Dialog_Simple);

        setContentView(R.layout.dialog_sense_alert);

        this.container = (LinearLayout) findViewById(R.id.dialog_sense_alert_container);

        this.titleText = (TextView) findViewById(R.id.dialog_sense_alert_title);
        this.messageText = (TextView) findViewById(R.id.dialog_sense_alert_message);
        messageText.setMovementMethod(LinkMovementMethod.getInstance());

        this.buttonDivider = findViewById(R.id.dialog_sense_alert_button_divider);
        this.negativeButton = (Button) findViewById(R.id.dialog_sense_alert_cancel);
        this.positiveButton = (Button) findViewById(R.id.dialog_sense_alert_ok);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Views.observeNextLayout(container)
             .subscribe(view -> {
                 DisplayMetrics metrics = new DisplayMetrics();
                 getWindow().getWindowManager().getDefaultDisplay().getMetrics(metrics);

                 int padding = getContext().getResources().getDimensionPixelSize(R.dimen.gap_outer);
                 int maxHeight = metrics.heightPixels - (padding * 2);

                 if (view.getMeasuredHeight() > maxHeight) {
                     view.getLayoutParams().height = maxHeight;
                     view.requestLayout();
                     view.invalidate();
                 }
             });
    }

    private void updatePaddingAndDividers() {
        boolean hasText = (titleText.getVisibility() == View.VISIBLE ||
                messageText.getVisibility() == View.VISIBLE);
        if (hasText) {
            int padding = getContext().getResources().getDimensionPixelSize(R.dimen.gap_outer);
            container.setPadding(0, padding, 0, 0);
            if (topViewDivider != null) {
                topViewDivider.setVisibility(View.VISIBLE);
            }
        } else {
            container.setPadding(0, 0, 0, 0);
            if (topViewDivider != null) {
                topViewDivider.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void setTitle(@Nullable CharSequence title) {
        super.setTitle(title);

        if (TextUtils.isEmpty(title)) {
            titleText.setVisibility(View.GONE);
        } else {
            titleText.setVisibility(View.VISIBLE);
        }

        titleText.setText(title);
        updatePaddingAndDividers();
    }

    @Override
    public void setTitle(@StringRes int titleId) {
        super.setTitle(titleId);

        if (titleId == 0) {
            titleText.setVisibility(View.GONE);
        } else {
            titleText.setVisibility(View.VISIBLE);
        }

        titleText.setText(titleId);
        updatePaddingAndDividers();
    }

    public void setTitleColor(int color) {
        titleText.setTextColor(color);
    }

    public void setMessage(@Nullable CharSequence message) {
        if (TextUtils.isEmpty(message)) {
            messageText.setVisibility(View.GONE);
        } else {
            messageText.setVisibility(View.VISIBLE);
        }

        messageText.setText(message);
        updatePaddingAndDividers();
    }

    public void setMessage(@StringRes int messageId) {
        if (messageId == 0) {
            messageText.setVisibility(View.GONE);
            messageText.setText(null);
        } else {
            messageText.setVisibility(View.VISIBLE);
            messageText.setText(messageId);
        }
        updatePaddingAndDividers();
    }

    public CharSequence getMessage() {
        return messageText.getText();
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

    protected void updateButtonDivider() {
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

    /**
     * @see android.content.DialogInterface#BUTTON_POSITIVE
     * @see android.content.DialogInterface#BUTTON_NEGATIVE
     */
    public Button getButton(int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                return positiveButton;

            case DialogInterface.BUTTON_NEGATIVE:
                return negativeButton;

            default:
                return null;
        }
    }

    public void setButtonDestructive(int which, boolean flag) {
        Button button = getButton(which);
        if (button == null) {
            Logger.error(getClass().getSimpleName(), "Unknown button #" + which + ", ignoring.");
            return;
        }

        if (flag) {
            button.setTextColor(getContext().getResources().getColor(R.color.destructive_accent));
        } else {
            button.setTextColor(getContext().getResources().getColor(R.color.light_accent));
        }
    }

    public void setButtonDeemphasized(int which, boolean flag) {
        Button button = getButton(which);
        if (button == null) {
            Logger.error(getClass().getSimpleName(), "Unknown button #" + which + ", ignoring.");
            return;
        }

        if (flag) {
            button.setTextColor(getContext().getResources().getColor(R.color.text_medium));
        } else {
            button.setTextColor(getContext().getResources().getColor(R.color.light_accent));
        }
    }

    public void setButtonEnabled(int which, boolean enabled) {
        Button button = getButton(which);
        if (button == null) {
            Logger.error(getClass().getSimpleName(), "Unknown button #" + which + ", ignoring.");
            return;
        }

        button.setEnabled(enabled);
    }

    public void setView(@Nullable View view) {
        if (this.view == view) {
            return;
        }

        if (this.view != null) {
            container.removeView(this.view);
        }

        this.view = view;

        if (view != null) {
            int end = container.getChildCount() - 1;

            if (bottomViewDivider == null) {
                this.bottomViewDivider = Styles.createHorizontalDivider(getContext(), ViewGroup.LayoutParams.MATCH_PARENT);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(bottomViewDivider.getLayoutParams());
                layoutParams.bottomMargin = getContext().getResources().getDimensionPixelSize(R.dimen.gap_medium);
                bottomViewDivider.setLayoutParams(layoutParams);
                container.addView(bottomViewDivider, end);
            }

            container.addView(view, end);

            if (topViewDivider == null) {
                this.topViewDivider = Styles.createHorizontalDivider(getContext(), ViewGroup.LayoutParams.MATCH_PARENT);
                container.addView(topViewDivider, end);
            }
        } else {
            if (bottomViewDivider != null) {
                container.removeView(bottomViewDivider);
                this.bottomViewDivider = null;
            }

            if (topViewDivider != null) {
                container.removeView(topViewDivider);
                this.topViewDivider = null;
            }
        }

        updatePaddingAndDividers();
    }

    public void setView(@LayoutRes int viewRes) {
        View view = getLayoutInflater().inflate(viewRes, container, false);
        setView(view);
    }
}
