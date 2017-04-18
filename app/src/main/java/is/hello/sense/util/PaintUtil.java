package is.hello.sense.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;

import is.hello.sense.SenseApplication;

public class PaintUtil {

    public static void getCorrectTextSize(@NonNull final TextPaint paint,
                                          @Nullable final String text,
                                          final int maxWidth,
                                          final int maxHeight,
                                          final int maxTextSize) {
        if (text == null || text.isEmpty()) {
            return;
        }
        paint.setTextSize(0);
        final Rect textBounds = new Rect();
        while (doesTextFit(paint, text, textBounds, maxWidth, maxHeight)) {
            if (maxTextSize != Constants.NONE && maxTextSize <= paint.getTextSize()) {
                return;
            }
            paint.setTextSize(paint.getTextSize() + 1);
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    private static boolean doesTextFit(@NonNull final TextPaint paint,
                                       @NonNull final String text,
                                       @NonNull final Rect textBounds,
                                       final int width,
                                       final int height) {
        //during robolectric testing bounds is always 0 so just quit
        if (SenseApplication.isRunningInRobolectric()) {
            return false;
        }
        paint.getTextBounds(text, 0, text.length(), textBounds);
        if (textBounds.height() > height) {
            return false;
        }
        if (textBounds.width() > width) {
            return false;
        }
        return true;
    }

    public static void drawAndCenterText(@NonNull final Canvas canvas,
                                         @NonNull final Paint paint,
                                         @NonNull final String text) {
        final Rect textBounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), textBounds);
        canvas.getClipBounds(textBounds);
        final float cHeight = textBounds.height();
        final float cWidth = textBounds.width();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), textBounds);
        final float x = cWidth / 2f - textBounds.width() / 2f - textBounds.left;
        final float y = cHeight / 2f + textBounds.height() / 2f - textBounds.bottom;
        canvas.drawText(text, x, y, paint);
    }

}
