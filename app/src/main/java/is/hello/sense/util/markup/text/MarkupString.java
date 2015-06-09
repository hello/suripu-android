package is.hello.sense.util.markup.text;

import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;

public class MarkupString extends SpannableStringBuilder {
    public MarkupString(@NonNull CharSequence text) {
        super(text);
    }
}
