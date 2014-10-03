package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import is.hello.sense.R;
import is.hello.sense.util.ColorUtils;

@SuppressWarnings("UnusedDeclaration")
public class GraphView extends RelativeLayout {
    protected int maxValue = 100;
    protected int value = 60;

    protected int fillColor = Color.GREEN;

    public GraphView(Context context) {
        super(context);
        initialize(null, 0);
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs, 0);
    }

    public GraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs, defStyleAttr);
    }


    //region Properties

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        postInvalidate();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
        postInvalidate();
    }

    public int getFillColor() {
        return fillColor;
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
        postInvalidate();
    }

    public void showSleepScore(int sleepScore) {
        if (sleepScore < 0 || sleepScore > 100)
            throw new IllegalArgumentException("sleepScore " + sleepScore + " is out of bounds {0, 100}");

        int fillColor = getResources().getColor(ColorUtils.colorResForSleepDepth(sleepScore));
        setFillColor(fillColor);
        setValue(sleepScore);
    }

    //endregion


    protected void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
        setWillNotDraw(false);

        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.GraphView, defStyleAttr, 0);
            int value = styles.getInt(R.styleable.GraphView_value, 50);
            int maxValue = styles.getInt(R.styleable.GraphView_maxValue, 100);
            int foregroundColor = styles.getColor(R.styleable.GraphView_fillColor, Color.BLUE);

            setMaxValue(maxValue);
            setValue(value);
            setFillColor(foregroundColor);
        }
    }
}
