package is.hello.sense.ui.handholding;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.widget.RelativeLayout;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM;
import static android.widget.RelativeLayout.ALIGN_PARENT_TOP;

public class Tutorial implements Serializable {
    @IntDef({Gravity.TOP, Gravity.BOTTOM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DescriptionGravity {}

    public final CharSequence description;
    public final @DescriptionGravity int descriptionGravity;
    public final Interaction interaction;

    public Tutorial(@NonNull CharSequence description,
                    @DescriptionGravity int descriptionGravity,
                    @NonNull Interaction interaction) {
        this.description = description;
        this.descriptionGravity = descriptionGravity;
        this.interaction = interaction;
    }


    public RelativeLayout.LayoutParams generateDescriptionLayoutParams() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        switch (descriptionGravity) {
            case Gravity.TOP: {
                layoutParams.addRule(ALIGN_PARENT_TOP);
                break;
            }

            case Gravity.BOTTOM: {
                layoutParams.addRule(ALIGN_PARENT_BOTTOM);
                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown gravity constant " + descriptionGravity);
            }
        }
        return layoutParams;
    }
}
