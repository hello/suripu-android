package is.hello.sense.util;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.widget.TextView;


public class Styler {

    public static void setTextAppearance(@NonNull TextView textView, @StyleRes int styleRes) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            textView.setTextAppearance(styleRes);
        } else {
            textView.setTextAppearance(textView.getContext(), styleRes);
        }
    }

    public static ColorStateList getColorStateList(@NonNull Resources resources, @ColorRes int id, @Nullable Resources.Theme theme) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            return resources.getColorStateList(id, theme);
        } else {
            return resources.getColorStateList(id);
        }
    }
}
