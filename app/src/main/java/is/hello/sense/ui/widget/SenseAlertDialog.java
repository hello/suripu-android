package is.hello.sense.ui.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.Serializable;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Logger;

public class SenseAlertDialog extends Dialog {
    public static final int NO_TITLE_ID = 0;
    private final LinearLayout container;

    private final TextView titleText;
    private final TextView messageText;

    private final View buttonDivider;
    private final Button negativeButton;
    private final Button positiveButton;

    private View view;
    private View topViewDivider, bottomViewDivider;


    @Deprecated // todo make private
    public SenseAlertDialog(@NonNull final Context context) {
        this(context, R.style.AppTheme_Dialog_Simple);
    }

    @Deprecated // todo make private
    public SenseAlertDialog(@NonNull final Context context, final int style) {
        this(context, style, R.layout.dialog_sense_alert);
    }

    @Deprecated // todo make private
    private SenseAlertDialog(@NonNull final Context context, final int style, @LayoutRes final int layout) {
        super(context, style);

        setContentView(layout);

        this.container = (LinearLayout) findViewById(R.id.dialog_sense_alert_container);

        this.titleText = (TextView) findViewById(R.id.dialog_sense_alert_title);
        this.messageText = (TextView) findViewById(R.id.dialog_sense_alert_message);
        messageText.setMovementMethod(LinkMovementMethod.getInstance());

        this.buttonDivider = findViewById(R.id.dialog_sense_alert_button_divider);
        this.negativeButton = (Button) findViewById(R.id.dialog_sense_alert_cancel);
        this.positiveButton = (Button) findViewById(R.id.dialog_sense_alert_ok);
    }

    /**
     * @return a dialog with the properties of {@link SenseBottomAlertDialog} except is dismissed easily.
     */
    public static SenseAlertDialog newBottomSheetInstance(@NonNull final Context context) {
        final SenseAlertDialog dialog = new SenseAlertDialog(context,
                                                             R.style.AppTheme_Dialog_BottomSheet,
                                                             R.layout.dialog_sense_bottom_sheet);
        final Window window = dialog.getWindow();
        window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return dialog;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Views.runWhenLaidOut(container, () -> {
            final DisplayMetrics metrics = container.getResources().getDisplayMetrics();

            final int padding = getContext().getResources().getDimensionPixelSize(R.dimen.gap_medium);
            final int maxHeight = metrics.heightPixels - (padding * 2);

            if (container.getMeasuredHeight() > maxHeight) {
                container.getLayoutParams().height = maxHeight;
                container.requestLayout();
                container.invalidate();
            }
        });
    }

    private void updatePaddingAndDividers() {
        final boolean hasText = (titleText.getVisibility() == View.VISIBLE ||
                messageText.getVisibility() == View.VISIBLE);
        if (hasText) {
            final int padding = getContext().getResources().getDimensionPixelSize(R.dimen.gap_outer);
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
    public void setTitle(@Nullable final CharSequence title) {
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
    public void setTitle(@StringRes final int titleId) {

        if (titleId == NO_TITLE_ID) {
            titleText.setVisibility(View.GONE);
        } else {
            super.setTitle(titleId);
            titleText.setVisibility(View.VISIBLE);
            titleText.setText(titleId);
        }

        updatePaddingAndDividers();
    }

    public void setMessage(@Nullable final CharSequence message) {
        if (TextUtils.isEmpty(message)) {
            messageText.setVisibility(View.GONE);
        } else {
            messageText.setVisibility(View.VISIBLE);
        }

        messageText.setText(message);
        updatePaddingAndDividers();
    }

    public void setMessage(@StringRes final int messageId) {
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

    private View.OnClickListener createClickListener(@Nullable final DialogInterface.OnClickListener onClickListener, final int which) {
        if (onClickListener != null) {
            return view1 -> {
                onClickListener.onClick(this, which);
                dismiss();
            };
        } else {
            return view1 -> dismiss();
        }
    }

    protected void updateButtonDivider() {
        if (positiveButton.getVisibility() == View.VISIBLE && negativeButton.getVisibility() == View.VISIBLE) {
            buttonDivider.setVisibility(View.VISIBLE);
        } else {
            buttonDivider.setVisibility(View.GONE);
        }
    }

    public void setPositiveButton(@Nullable final CharSequence title, @Nullable final OnClickListener onClickListener) {
        if (title != null) {
            positiveButton.setVisibility(View.VISIBLE);
            positiveButton.setText(title);
            Views.setSafeOnClickListener(positiveButton, createClickListener(onClickListener, DialogInterface.BUTTON_POSITIVE));
        } else {
            positiveButton.setVisibility(View.GONE);
        }

        updateButtonDivider();
    }

    public void setPositiveButton(@StringRes final int titleId, @Nullable final OnClickListener onClickListener) {
        setPositiveButton(getContext().getString(titleId), onClickListener);
    }

    public void setPositiveRunnableButton(@StringRes final int titleId, @Nullable final SerializedRunnable runnable) {
        setPositiveButton(titleId, runnable == null ? null : (dialog, which) -> runnable.run());
    }

    public void setNegativeButton(@Nullable final CharSequence title, @Nullable final OnClickListener onClickListener) {
        if (title != null) {
            negativeButton.setVisibility(View.VISIBLE);
            negativeButton.setText(title);
            Views.setSafeOnClickListener(negativeButton, createClickListener(onClickListener, DialogInterface.BUTTON_NEGATIVE));
        } else {
            negativeButton.setVisibility(View.GONE);
        }

        updateButtonDivider();
    }

    public void setNegativeButton(@StringRes final int titleId, @Nullable final OnClickListener onClickListener) {
        setNegativeButton(getContext().getString(titleId), onClickListener);
    }

    public void setNegativeRunnableButton(@StringRes final int titleId, @Nullable final SerializedRunnable runnable) {
        setNegativeButton(titleId, runnable == null ? null : (dialog, which) -> runnable.run());
    }

    /**
     * @see android.content.DialogInterface#BUTTON_POSITIVE
     * @see android.content.DialogInterface#BUTTON_NEGATIVE
     */
    public Button getButton(final int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                return positiveButton;

            case DialogInterface.BUTTON_NEGATIVE:
                return negativeButton;

            default:
                return null;
        }
    }

    public void setButtonDestructive(final int which, final boolean flag) {
        final Button button = getButton(which);
        if (button == null) {
            Logger.error(getClass().getSimpleName(), "Unknown button #" + which + ", ignoring.");
            return;
        }

        if (flag) {
            button.setTextColor(ContextCompat.getColor(getContext(), R.color.destructive_accent));
        } else {
            button.setTextColor(ContextCompat.getColor(getContext(), R.color.light_accent));
        }
    }

    public void setButtonDeemphasized(final int which, final boolean flag) {
        final Button button = getButton(which);
        if (button == null) {
            Logger.error(getClass().getSimpleName(), "Unknown button #" + which + ", ignoring.");
            return;
        }

        if (flag) {
            button.setTextColor(ContextCompat.getColor(getContext(), R.color.text_medium));
        } else {
            button.setTextColor(ContextCompat.getColor(getContext(), R.color.light_accent));
        }
    }

    public void setButtonEnabled(final int which, final boolean enabled) {
        final Button button = getButton(which);
        if (button == null) {
            Logger.error(getClass().getSimpleName(), "Unknown button #" + which + ", ignoring.");
            return;
        }

        button.setEnabled(enabled);
    }

    public void setView(@Nullable final View view, final boolean wantsDividers) {
        if (this.view == view) {
            return;
        }

        if (this.view != null) {
            container.removeView(this.view);
        }

        this.view = view;

        if (view != null) {
            final int end = container.getChildCount() - 1;

            if (bottomViewDivider == null && wantsDividers) {
                this.bottomViewDivider = Styles.createHorizontalDivider(getContext(), ViewGroup.LayoutParams.MATCH_PARENT);
                final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(bottomViewDivider.getLayoutParams());
                layoutParams.bottomMargin = getContext().getResources().getDimensionPixelSize(R.dimen.gap_medium);
                bottomViewDivider.setLayoutParams(layoutParams);
                container.addView(bottomViewDivider, end);
            }

            container.addView(view, end);

            if (topViewDivider == null && wantsDividers) {
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

    public void setView(@LayoutRes final int viewRes) {
        final View view = getLayoutInflater().inflate(viewRes, container, false);
        setView(view, true);
    }


    public interface SerializedRunnable extends Serializable {
        void run();
    }

    public static class Builder {
        private final static String ARG_TITLE = Builder.class.getSimpleName() + ".ARG_TITLE";
        private final static String ARG_MESSAGE = Builder.class.getSimpleName() + ".ARG_MESSAGE";
        private final static String ARG_POSITIVE_CLICK_LISTENER = Builder.class.getSimpleName() + ".ARG_POSITIVE_CLICK_LISTENER";
        private final static String ARG_POSITIVE_CLICK_TEXT = Builder.class.getSimpleName() + ".ARG_POSITIVE_CLICK_TEXT";
        private final static String ARG_NEGATIVE_CLICK_LISTENER = Builder.class.getSimpleName() + ".ARG_NEGATIVE_CLICK_LISTENER";
        private final static String ARG_NEGATIVE_CLICK_TEXT = Builder.class.getSimpleName() + ".ARG_NEGATIVE_CLICK_TEXT";
        private final static String ARG_DESTRUCTIVE_BUTTON = Builder.class.getSimpleName() + ".ARG_DESTRUCTIVE_BUTTON";
        private final static String ARG_DESTRUCTIVE_FLAG = Builder.class.getSimpleName() + ".ARG_DESTRUCTIVE_FLAG";
        private final Bundle bundle = new Bundle();


        public Builder setTitle(@StringRes final int titleRes) {
            bundle.putInt(ARG_TITLE, titleRes);
            return this;
        }

        public Builder setMessage(@StringRes final int messageRes) {
            bundle.putInt(ARG_MESSAGE, messageRes);
            return this;
        }

        public Builder setPositiveButton(@StringRes final int textRes, @Nullable final SerializedRunnable action) {
            if (action != null) {
                bundle.putSerializable(ARG_POSITIVE_CLICK_LISTENER, action);
            }
            bundle.putInt(ARG_POSITIVE_CLICK_TEXT, textRes);
            return this;
        }

        public Builder setNegativeButton(@StringRes final int textRes, @Nullable final SerializedRunnable action) {
            if (action != null) {
                bundle.putSerializable(ARG_NEGATIVE_CLICK_LISTENER, action);
            }
            bundle.putInt(ARG_NEGATIVE_CLICK_TEXT, textRes);
            return this;
        }

        public Builder setButtonDestructive(final int which, final boolean flag) {
            if (which < -2 || which > -1) {
                return this; // only support two buttons right now.
            }
            bundle.putInt(ARG_DESTRUCTIVE_BUTTON, which);
            bundle.putBoolean(ARG_DESTRUCTIVE_FLAG, flag);
            return this;
        }

        public SenseAlertDialog build(@NonNull final Activity activity) {
            final SenseAlertDialog alertDialog = new SenseAlertDialog(activity);
            if (bundle.containsKey(ARG_TITLE)) {
                alertDialog.setTitle(bundle.getInt(ARG_TITLE));
            }
            if (bundle.containsKey(ARG_MESSAGE)) {
                alertDialog.setMessage(bundle.getInt(ARG_MESSAGE));
            }
            if (bundle.containsKey(ARG_POSITIVE_CLICK_TEXT)) {
                final SerializedRunnable runnable;
                if (bundle.containsKey(ARG_POSITIVE_CLICK_LISTENER)) {
                    runnable = (SerializedRunnable) bundle.getSerializable(ARG_POSITIVE_CLICK_LISTENER);
                } else {
                    runnable = null;
                }
                alertDialog.setPositiveRunnableButton(bundle.getInt(ARG_POSITIVE_CLICK_TEXT), runnable);
            }
            if (bundle.containsKey(ARG_NEGATIVE_CLICK_TEXT)) {
                final SerializedRunnable runnable;
                if (bundle.containsKey(ARG_NEGATIVE_CLICK_LISTENER)) {
                    runnable = (SerializedRunnable) bundle.getSerializable(ARG_NEGATIVE_CLICK_LISTENER);
                } else {
                    runnable = null;
                }
                alertDialog.setNegativeRunnableButton(bundle.getInt(ARG_NEGATIVE_CLICK_TEXT), runnable);
            }
            return alertDialog;
        }
    }
}
