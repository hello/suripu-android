package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.widget.util.Styles;

public class TimelineOverlayDrawable extends Drawable {
    private final Paint overlayPaint = new Paint();
    private final Rect textRect = new Rect();

    private final Paint topTextPaint = new Paint(Paint.SUBPIXEL_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetricsInt topTextFontMetrics;

    private final Paint bottomTextPaint = new Paint(Paint.SUBPIXEL_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetricsInt bottomTextFontMetrics;

    private final Rect maskedArea;
    private final String topText;
    private final String bottomText;

    public TimelineOverlayDrawable(@NonNull Context context,
                                   @NonNull Rect maskedArea,
                                   @NonNull TimelineSegment segment) {
        Resources resources = context.getResources();
        int overlayColor = resources.getColor(R.color.background_light_overlay);
        overlayPaint.setColor(overlayColor);

        int textSize = resources.getDimensionPixelOffset(R.dimen.text_size_body);

        topTextPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        topTextPaint.setTextSize(textSize);
        this.topTextFontMetrics = topTextPaint.getFontMetricsInt();

        bottomTextPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        bottomTextPaint.setTextSize(textSize);
        this.bottomTextFontMetrics = bottomTextPaint.getFontMetricsInt();


        this.maskedArea = maskedArea;
        this.topText = resources.getString(Styles.getSleepDepthStringRes(segment.getSleepDepth()));
        this.bottomText = DateUtils.getRelativeTimeSpanString(context, segment.getDuration()).toString();
    }

    @Override
    public void draw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        canvas.drawRect(0, 0, width, maskedArea.top, overlayPaint);
        canvas.drawRect(0, maskedArea.bottom, width, height, overlayPaint);

        bottomTextPaint.getTextBounds(bottomText, 0, bottomText.length(), textRect);

        int textX = (width / 2) - textRect.centerX();
        int textY = maskedArea.top - textRect.height() - topTextFontMetrics.bottom - bottomTextFontMetrics.bottom;
        canvas.drawText(bottomText, textX, textY, bottomTextPaint);


        topTextPaint.getTextBounds(topText, 0, topText.length(), textRect);

        textX = (width / 2) - textRect.centerX();
        textY -= textRect.height() + bottomTextFontMetrics.bottom;
        canvas.drawText(topText, textX, textY, topTextPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        overlayPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        overlayPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
