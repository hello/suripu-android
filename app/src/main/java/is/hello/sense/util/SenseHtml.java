package is.hello.sense.util;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;

import is.hello.sense.R;

/**
 * An extended html parser that adds additional tags specific to the Sense Android app.
 * <p/>
 * Included tags are:
 * <ol>
 *     <li>color-green</li>
 *     <li>color-orange</li>
 *     <li>color-red</li>
 *     <li>color-purple</li>
 * </ol>
 * <p/>
 * Unrecognized tags are logged and ignored.
 */
public class SenseHtml {
    private static final String TAG_COLOR_GREEN = "color-green";
    private static final String TAG_COLOR_ORANGE = "color-orange";
    private static final String TAG_COLOR_RED = "color-red";
    private static final String TAG_COLOR_PURPLE = "color-purple";

    public static @NonNull CharSequence fromHtml(@NonNull Context context, @Nullable String source) {
        if (TextUtils.isEmpty(source)) {
            return "";
        }

        return Html.fromHtml(source, null, (opening, tag, output, reader) -> {
            if (isColorTag(tag)) {
                handleColorTag(opening, context, tag, output);
            }
        });
    }


    private static boolean isColorTag(@NonNull String tag) {
        return (TAG_COLOR_GREEN.equals(tag) || TAG_COLOR_ORANGE.equals(tag) ||
                TAG_COLOR_RED.equals(tag) || TAG_COLOR_PURPLE.equals(tag));
    }

    private static int getColorForTag(@NonNull Context context, @NonNull String tag) {
        switch (tag) {
            case TAG_COLOR_GREEN: {
                return context.getResources().getColor(R.color.sensor_ideal);
            }

            case TAG_COLOR_ORANGE: {
                return context.getResources().getColor(R.color.sensor_alert);
            }

            case TAG_COLOR_RED: {
                return context.getResources().getColor(R.color.sensor_warning);
            }

            case TAG_COLOR_PURPLE: {
                return context.getResources().getColor(R.color.purple);
            }

            default: {
                Logger.warn(SenseHtml.class.getSimpleName(), "Unrecognized color tag " + tag);
                return Color.BLACK;
            }
        }
    }


    // Based on <http://stackoverflow.com/questions/4044509/android-how-to-use-the-html-taghandler>
    private static void handleColorTag(boolean opening, @NonNull Context context, @NonNull String tag, @NonNull Editable output) {
        int length = output.length();
        int color = getColorForTag(context, tag);
        if (opening) {
            output.setSpan(new ForegroundColorSpan(color), length, length, Spanned.SPAN_MARK_MARK);
        } else {
            ForegroundColorSpan lastSpan = findLastSpan(output, ForegroundColorSpan.class);
            if (lastSpan == null) {
                Logger.warn(SenseHtml.class.getSimpleName(), "Could not find last foreground color span.");
                return;
            }

            int start = output.getSpanStart(lastSpan);
            output.removeSpan(lastSpan);

            output.setSpan(new ForegroundColorSpan(color), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static @Nullable <T extends CharacterStyle> T findLastSpan(Editable text, Class<T> kind) {
        T[] spans = text.getSpans(0, text.length(), kind);
        if (spans.length == 0) {
            return null;
        } else {
            for (int i = spans.length; i > 0; i--) {
                if (text.getSpanFlags(spans[i - 1]) == Spanned.SPAN_MARK_MARK) {
                    return spans[i - 1];
                }
            }
            return null;
        }
    }
}
