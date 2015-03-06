package is.hello.sense.ui.handholding.util;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;

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

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
        return (forwarder.tryConsumeTrackballEvent(event) ||
                super.dispatchTouchEvent(event));
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return (forwarder.tryConsumeKeyEvent(event) ||
                super.dispatchKeyEvent(event));
    }

    @Override
    public boolean dispatchKeyShortcutEvent(@NonNull KeyEvent event) {
        return (forwarder.tryConsumeKeyShortcutKey(event) ||
                super.dispatchKeyShortcutEvent(event));
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(@NonNull AccessibilityEvent event) {
        return (forwarder.tryConsumePopulateAccessibilityEvent(event) ||
                super.dispatchPopulateAccessibilityEvent(event));
    }

    //endregion


    public interface EventForwarder {
        boolean tryConsumeTouchEvent(@NonNull MotionEvent event);
        boolean tryConsumeTrackballEvent(@NonNull MotionEvent event);
        boolean tryConsumeKeyEvent(@NonNull KeyEvent event);
        boolean tryConsumeKeyShortcutKey(@NonNull KeyEvent event);
        boolean tryConsumePopulateAccessibilityEvent(@NonNull AccessibilityEvent event);
    }
}
