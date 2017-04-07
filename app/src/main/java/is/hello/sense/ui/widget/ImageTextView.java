package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Constants;

public class ImageTextView extends LinearLayout {
    private final ImageView imageView;
    private final TextView textView;


    public ImageTextView(final Context context) {
        this(context, null);
    }

    public ImageTextView(final Context context,
                         final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageTextView(final Context context,
                         final AttributeSet attrs,
                         final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        this.imageView = new ImageView(context);
        this.textView = new TextView(context);
        this.textView.setMaxLines(1);
        this.textView.setEllipsize(TextUtils.TruncateAt.END);
        addView(this.imageView);
        addView(this.textView);

        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ImageTextView,
                0, 0);

        try {
            final String text = a.getString(R.styleable.ImageTextView_labelText);
            @DrawableRes final int imageRes = a.getResourceId(R.styleable.ImageTextView_leftImage, Constants.NONE);
            @StyleRes final int textAppearanceRes = a.getResourceId(R.styleable.ImageTextView_textStyle, Constants.NONE);
            @DimenRes final int leftTextPaddingRes = a.getResourceId(R.styleable.ImageTextView_leftTextPadding, Constants.NONE);
            @ColorRes final int textColorRes = a.getResourceId(R.styleable.ImageTextView_textColor, Constants.NONE);
            @ColorRes final int tintColorRes = a.getResourceId(R.styleable.ImageTextView_tintColor, Constants.NONE);
            // ImageView
            if (imageRes != Constants.NONE) {
                if (tintColorRes == Constants.NONE) {
                    setImageResource(imageRes);
                } else {
                    setImageDrawable(Styles.tintDrawable(getContext(),
                                                         imageRes,
                                                         tintColorRes));
                }
            }

            // TextView
            Styles.setTextAppearance(textView, textAppearanceRes);
            setTextPaddingRes(leftTextPaddingRes,
                              Constants.NONE,
                              Constants.NONE,
                              Constants.NONE);
            setText(text);
            if (textColorRes != Constants.NONE) {
                setTextColor(textColorRes);
            }

        } finally {
            a.recycle();
        }

    }

    public void setText(@StringRes final int stringRes) {
        this.textView.setText(stringRes);
    }

    public void setText(@Nullable final String string) {
        this.textView.setText(string);
    }

    public CharSequence getText() {
        return this.textView.getText();
    }

    public void setTextColor(@ColorRes final int textColor) {
        this.textView.setTextColor(ContextCompat.getColor(getContext(), textColor));
    }

    public void setImageResource(@DrawableRes final int drawableRes) {
        this.imageView.setImageResource(drawableRes);
    }

    public void setImageDrawable(@Nullable final Drawable drawable) {
        this.imageView.setImageDrawable(drawable);
    }

    public void setImageResource(@Nullable final Drawable drawable) {
        this.imageView.setImageDrawable(drawable);
    }

    public void setTextViewVisibility(final boolean setVisible) {
        this.textView.setVisibility(setVisible ? VISIBLE : GONE);
    }

    public void setImageViewVisibility(final boolean setVisible) {
        this.imageView.setVisibility(setVisible ? VISIBLE : GONE);
    }

    public void setTextPaddingRes(@DimenRes final int left,
                                  @DimenRes final int top,
                                  @DimenRes final int right,
                                  @DimenRes final int bottom) {
        final Resources resources = getResources();
        setTextPadding(left != Constants.NONE ? resources.getDimensionPixelSize(left) : 0,
                       top != Constants.NONE ? resources.getDimensionPixelSize(top) : 0,
                       right != Constants.NONE ? resources.getDimensionPixelSize(right) : 0,
                       bottom != Constants.NONE ? resources.getDimensionPixelSize(bottom) : 0);
    }

    public void setTextPadding(final int left,
                               final int top,
                               final int right,
                               final int bottom) {
        this.textView.setPadding(left, top, right, bottom);
    }

}
