package is.hello.sense.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import is.hello.sense.R;
import is.hello.sense.util.Animation;

@SuppressWarnings("UnusedDeclaration")
public class GraphView extends LinearLayout {
    protected int maxValue = 100;
    protected int value = 60;

    protected int topInset = 0;
    protected int bottomInset = 0;
    protected int leftInset = 0;
    protected int rightInset = 0;

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

    protected void assertSaneValue(int value) {
        if (value < 0 || value > getMaxValue()) {
            throw new IllegalArgumentException("value " + value + " is out of bounds {0, 100}");
        }
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        if (maxValue == this.maxValue)
            return;

        this.maxValue = maxValue;
        postInvalidate();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        if (value == this.value)
            return;

        assertSaneValue(value);

        this.value = value;
        postInvalidate();
    }

    public @Nullable ValueAnimator animationForNewValue(int newValue, @NonNull Animation.Properties animationProperties) {
        if (newValue == getValue())
            return null;

        assertSaneValue(newValue);

        ValueAnimator animator = animationProperties.apply(ValueAnimator.ofInt(getValue(), newValue));
        animator.addUpdateListener(animation -> {
            int value = (Integer) animation.getAnimatedValue();
            setValue(value);
        });

        return animator;
    }

    public int getFillColor() {
        return fillColor;
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
        postInvalidate();
    }

    public int getTopInset() {
        return topInset;
    }

    public void setTopInset(int topInset) {
        this.topInset = topInset;
        postInvalidate();
    }

    public int getBottomInset() {
        return bottomInset;
    }

    public void setBottomInset(int bottomInset) {
        this.bottomInset = bottomInset;
        postInvalidate();
    }

    public int getLeftInset() {
        return leftInset;
    }

    public void setLeftInset(int leftInset) {
        this.leftInset = leftInset;
        postInvalidate();
    }

    public int getRightInset() {
        return rightInset;
    }

    public void setRightInset(int rightInset) {
        this.rightInset = rightInset;
        postInvalidate();
    }

    //endregion


    protected void initialize(@Nullable AttributeSet attrs, int defStyleAttr) {
        setWillNotDraw(false);

        if (attrs != null) {
            TypedArray styles = getContext().obtainStyledAttributes(attrs, R.styleable.GraphView, defStyleAttr, 0);
            this.value = styles.getInt(R.styleable.GraphView_value, 50);
            this.maxValue = styles.getInt(R.styleable.GraphView_maxValue, 100);
            this.fillColor = styles.getColor(R.styleable.GraphView_fillColor, Color.BLUE);
            this.topInset = (int) styles.getDimension(R.styleable.GraphView_topInset, 0f);
            this.leftInset = (int) styles.getDimension(R.styleable.GraphView_leftInset, 0f);
            this.rightInset = (int) styles.getDimension(R.styleable.GraphView_rightInset, 0f);
            this.bottomInset = (int) styles.getDimension(R.styleable.GraphView_bottomInset, 0f);
        }
    }
}
