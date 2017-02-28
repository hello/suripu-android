package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;

public class ImageTextView extends FrameLayout {
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
        LayoutInflater.from(context).inflate(R.layout.widget_image_textview, this);
        this.imageView = (ImageView) findViewById(R.id.widget_image_textview_image);
        this.textView = (TextView) findViewById(R.id.widget_image_textview_text);

    }

    public void setText(@StringRes final int stringRes) {
        this.textView.setText(stringRes);
    }

    public void setImageResource(@DrawableRes final int drawableRes) {
        this.imageView.setImageResource(drawableRes);
    }

    public void setTextViewVisibility(final boolean setVisible) {
        this.textView.setVisibility(setVisible ? VISIBLE : GONE);
    }

    public void setImageViewVisibility(final boolean setVisible) {
        this.imageView.setVisibility(setVisible ? VISIBLE : GONE);
    }

}
