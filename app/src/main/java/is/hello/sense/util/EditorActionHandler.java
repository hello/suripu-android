package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public class EditorActionHandler implements TextView.OnEditorActionListener {
    private static final long ACTION_RATE_LIMIT = 350; // A long animation

    public final int actionId;
    public final Runnable onAction;

    private long actionLastCalled;

    public EditorActionHandler(int actionId, @NonNull Runnable onAction) {
        this.actionId = actionId;
        this.onAction = onAction;
    }

    public EditorActionHandler(Runnable onAction) {
        this(EditorInfo.IME_ACTION_GO, onAction);
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
        if (actionId == this.actionId || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            long now = AnimationUtils.currentAnimationTimeMillis();
            if ((now - actionLastCalled) < ACTION_RATE_LIMIT) {
                return true;
            }

            this.actionLastCalled = now;

            InputMethodManager inputMethodManager = (InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(textView.getWindowToken(), 0);
            textView.clearFocus();

            onAction.run();

            return true;
        }

        return false;
    }
}
