package is.hello.sense.ui.handholding;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import is.hello.sense.R;

public class InteractionView extends View {
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ovalRect = new RectF();

    public InteractionView(@NonNull Context context) {
        this(context, null);
    }

    public InteractionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InteractionView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources resources = getResources();

        fillPaint.setColor(resources.getColor(R.color.light_accent));
        fillPaint.setAlpha(128);

        borderPaint.setColor(resources.getColor(R.color.light_accent));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(resources.getDimensionPixelSize(R.dimen.shadow_size));

        int area = resources.getDimensionPixelSize(R.dimen.view_interaction_area);
        setMinimumWidth(area);
        setMinimumHeight(area);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        ovalRect.set(0f, 0f, width, height);
        canvas.drawOval(ovalRect, fillPaint);

        float inset = borderPaint.getStrokeWidth() / 2f;
        ovalRect.inset(inset, inset);
        canvas.drawOval(ovalRect, borderPaint);
    }
}
