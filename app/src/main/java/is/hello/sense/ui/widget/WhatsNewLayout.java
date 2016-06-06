package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import is.hello.sense.R;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.util.Constants;

public class WhatsNewLayout extends FrameLayout {
    public static boolean shouldShow(@NonNull final Context context) {
        final SharedPreferences preferences =
                context.getSharedPreferences(Constants.WHATS_NEW_LAYOUT_SHOULD_SHOW_PREFS, 0);
        final String lastVersion = preferences.getString(Constants.WHATS_NEW_LAYOUT_LAST_VERSION_SHOWN, null);
        final String desiredVersion = context.getString(R.string.whats_new_version);
        if (lastVersion != null && lastVersion.equals(desiredVersion)) {
            return false;
        }
        return !(context.getString(R.string.whats_new_title_text).isEmpty()
                || context.getString(R.string.whats_new_message_text).isEmpty()
                || context.getString(R.string.whats_new_button_text).isEmpty()
                || context.getString(R.string.whats_new_version).isEmpty());
    }

    public static void clearState(@NonNull final Context context) {
        context.getSharedPreferences(Constants.WHATS_NEW_LAYOUT_SHOULD_SHOW_PREFS, 0)
               .edit()
               .clear()
               .apply();
    }

    public static void markShown(@NonNull final Context context) {
        context.getSharedPreferences(Constants.WHATS_NEW_LAYOUT_SHOULD_SHOW_PREFS, 0)
               .edit()
               .putString(Constants.WHATS_NEW_LAYOUT_LAST_VERSION_SHOWN, context.getString(R.string.whats_new_version))
               .apply();
    }


    private Listener listener;


    public WhatsNewLayout(@NonNull final Context context) {
        this(context, null);
    }

    public WhatsNewLayout(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WhatsNewLayout(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_whats_new_layout, this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        findViewById(R.id.view_whats_new_close).setOnClickListener((ignored) -> {
            if (listener != null) {
                listener.onDismiss();
            }
            markShown(context);
        });
        findViewById(R.id.view_whats_new_learn_more).setOnClickListener((ignored) -> {
            UserSupport.showLearnMore(context, R.string.whats_new_button_link);
        });
    }


    public void setListener(@Nullable final Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onDismiss();
    }
}
