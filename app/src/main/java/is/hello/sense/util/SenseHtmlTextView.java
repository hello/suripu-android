package is.hello.sense.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

import is.hello.sense.R;

public class SenseHtmlTextView extends TextView {
    public SenseHtmlTextView(Context context) {
        super(context);
        initialize(null, 0);
    }

    public SenseHtmlTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs, 0);
    }

    public SenseHtmlTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(attrs, defStyle);
    }

    public void setSenseHtml(@Nullable String html) {
        setText(SenseHtml.fromHtml(getContext(), html));
    }

    protected void initialize(@Nullable AttributeSet attrs, int defStyle) {
        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.SenseHtmlTextView, defStyle, 0);
            String senseHtml = styles.getString(R.styleable.SenseHtmlTextView_senseHtml);
            setSenseHtml(senseHtml);
        }
    }
}
