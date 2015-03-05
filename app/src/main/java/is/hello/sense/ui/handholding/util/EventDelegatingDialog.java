package is.hello.sense.ui.handholding.util;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.MotionEvent;

public class EventDelegatingDialog extends Dialog {
    private final EventForwarder forwarder;

    public EventDelegatingDialog(@NonNull Context context, @StyleRes int theme, @NonNull EventForwarder forwarder) {
        super(context, theme);

        this.forwarder = forwarder;
    }


    //region Events

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return (forwarder.tryConsumeTouchEvent(event) ||
                super.dispatchTouchEvent(event));
    }

    //endregion


    public interface EventForwarder {
        boolean tryConsumeTouchEvent(@NonNull MotionEvent event);
    }
}
