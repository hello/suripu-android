package is.hello.sense.ui.widget;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import is.hello.sense.R;
import is.hello.sense.databinding.ItemWelcomeCardCloseBinding;
import is.hello.sense.ui.widget.util.Views;

/**
 * Widget to display inside feeds to introduce features
 */
public class WelcomeCard extends FrameLayout {

    private final ItemWelcomeCardCloseBinding binding;

    public WelcomeCard(@NonNull final Context context) {
        this(context, null, 0);
    }

    public WelcomeCard(final Context context,
                       final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WelcomeCard(final Context context,
                       final AttributeSet attrs,
                       final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.binding = DataBindingUtil.inflate(LayoutInflater.from(context),
                                               R.layout.item_welcome_card_close, this, true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.binding.itemWelcomeClose.setOnClickListener(null);
    }

    public void setContent(@DrawableRes final int imageRes,
                           @StringRes final int titleRes,
                           @StringRes final int messageRes) {
        this.binding.itemWelcomeCard.itemWelcomeImage.setImageResource(imageRes);
        this.binding.itemWelcomeCard.itemWelcomeTitle.setText(titleRes);
        this.binding.itemWelcomeCard.itemWelcomeMessage.setText(messageRes);
    }

    public void setOnCloseButtonListener(@NonNull final OnClickListener listener) {
        Views.setSafeOnClickListener(this.binding.itemWelcomeClose, listener);
    }

    public void showCloseButton(final boolean isVisible) {
        if (isVisible) {
            this.binding.getRoot().setBackgroundResource(R.drawable.raised_item_normal);
            this.binding.itemWelcomeCloseDivider.setVisibility(VISIBLE);
            this.binding.itemWelcomeClose.setVisibility(VISIBLE);
        } else {
            this.binding.getRoot().setBackground(null);
            this.binding.itemWelcomeCloseDivider.setVisibility(GONE);
            this.binding.itemWelcomeClose.setVisibility(GONE);
        }
    }
}
