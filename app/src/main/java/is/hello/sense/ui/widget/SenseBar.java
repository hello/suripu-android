package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;

public class SenseBar extends FrameLayout {
    private final ImageView leftImage;
    private final ImageView rightImage;
    private final TextView textView;

    public SenseBar(@NonNull final Context context) {
        this(context, null);
    }

    public SenseBar(@NonNull final Context context,
                    @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SenseBar(@NonNull final Context context,
                    @Nullable final AttributeSet attrs,
                    final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_sense_bar, this);
        this.leftImage = (ImageView) findViewById(R.id.view_sense_bar_left_image);
        this.rightImage = (ImageView) findViewById(R.id.view_sense_bar_right_image);
        this.textView = (TextView) findViewById(R.id.view_sense_bar_text);
    }

    public void setLeftImage(@DrawableRes final int drawableRes) {
        this.leftImage.setImageResource(drawableRes);
    }

    public void setRightImage(@DrawableRes final int drawableRes) {
        this.rightImage.setImageResource(drawableRes);
    }

    public void setText(@StringRes final int stringRes) {
        this.textView.setText(stringRes);
    }

    public void setText(@Nullable final String text) {
        this.textView.setText(text);
    }

    public void showLeftImage(final boolean show) {
        this.leftImage.setVisibility(show ? VISIBLE : INVISIBLE);
    }

    public void showRightImage(final boolean show) {
        this.rightImage.setVisibility(show ? VISIBLE : INVISIBLE);
    }

    public void setLeftImageOnClickListener(@Nullable final OnClickListener onClickListener) {
        this.leftImage.setOnClickListener(onClickListener);
    }

    public void setRightImageOnClickListener(@Nullable final OnClickListener onClickListener) {
        this.rightImage.setOnClickListener(onClickListener);
    }
    public void alignTextLeft(){
        this.textView.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
    }

}
