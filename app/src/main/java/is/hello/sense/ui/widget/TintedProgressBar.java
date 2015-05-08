package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import is.hello.sense.R;

@Deprecated
public class TintedProgressBar extends ProgressBar {
    @SuppressWarnings("UnusedDeclaration")
    public TintedProgressBar(Context context) {
        super(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public TintedProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("UnusedDeclaration")
    public TintedProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    protected void applyColorFilter(@Nullable Drawable d) {
        if (d != null) {
            d.setColorFilter(getResources().getColor(R.color.light_accent), PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public void setIndeterminateDrawable(Drawable d) {
        applyColorFilter(d);
        super.setIndeterminateDrawable(d);
    }

    @Override
    public void setProgressDrawable(Drawable d) {
        applyColorFilter(d);
        super.setProgressDrawable(d);
    }
}
