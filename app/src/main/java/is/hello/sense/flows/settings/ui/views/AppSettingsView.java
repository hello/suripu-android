package is.hello.sense.flows.settings.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.databinding.ViewAppSettingsBinding;
import is.hello.sense.mvp.view.BindedSenseView;
import is.hello.sense.ui.widget.util.Views;

@SuppressLint("ViewConstructor")
public class AppSettingsView extends BindedSenseView<ViewAppSettingsBinding> {
    public static final int INDEX_ACCOUNT = 1000;
    public static final int INDEX_DEVICES = 1001;
    public static final int INDEX_NOTIFICATIONS = 1002;
    public static final int INDEX_EXPANSIONS = 1003;
    public static final int INDEX_VOICE = 1004;
    public static final int INDEX_SUPPORT = 1005;
    public static final int INDEX_SHARE = 1006;
    public static final int INDEX_DEBUG = 1007;

    private Listener listener = null;

    public AppSettingsView(@NonNull final Activity activity) {
        super(activity);
        Views.setTimeOffsetOnClickListener(this.binding.viewAppSettingsMyAccount, v -> onClicked(INDEX_ACCOUNT));
        Views.setTimeOffsetOnClickListener(this.binding.viewAppSettingsDevices, v -> onClicked(INDEX_DEVICES));
        Views.setTimeOffsetOnClickListener(this.binding.viewAppSettingsNotifications, v -> onClicked(INDEX_NOTIFICATIONS));
        Views.setTimeOffsetOnClickListener(this.binding.viewAppSettingsExpansions, v -> onClicked(INDEX_EXPANSIONS));
        Views.setTimeOffsetOnClickListener(this.binding.viewAppSettingsVoice, v -> onClicked(INDEX_VOICE));
        Views.setTimeOffsetOnClickListener(this.binding.viewAppSettingsSupport, v -> onClicked(INDEX_SUPPORT));
        Views.setTimeOffsetOnClickListener(this.binding.viewAppSettingsShare, v -> onClicked(INDEX_SHARE));
        Views.setTimeOffsetOnClickListener(this.binding.viewAppSettingsDebug, v -> onClicked(INDEX_DEBUG));
        showLoading(true);
    }

    //region BindedPresenterView
    @Override
    protected int getLayoutRes() {
        return R.layout.view_app_settings;
    }

    @Override
    public final void releaseViews() {
        this.listener = null;
    }
    //endregion

    //region methods
    public void setListener(@Nullable final Listener listener) {
        this.listener = listener;
    }

    private void onClicked(final int position) {
        if (this.listener == null) {
            return;
        }
        this.listener.onItemClicked(position);

    }

    public void enableDebug(final boolean enable) {
        this.binding.viewAppSettingsDebug.setClickable(enable);
    }

    public void setDebugText(@Nullable final String text) {
        this.binding.viewAppSettingsDebug.setText(text);
    }

    public final void showVoiceEnabledRows(final boolean show) {
        if (show) {
            this.binding.viewAppSettingsVoiceContainer.setVisibility(VISIBLE);
        } else {
            this.binding.viewAppSettingsVoiceContainer.setVisibility(GONE);
        }
        showLoading(false);
    }

    public void showLoading(final boolean show) {
        if (show) {
            this.binding.viewAppSettingsProgress.setVisibility(VISIBLE);
            this.binding.viewAppSettingsContent.setVisibility(GONE);
        } else {
            this.binding.viewAppSettingsProgress.setVisibility(GONE);
            this.binding.viewAppSettingsContent.setVisibility(VISIBLE);
        }
    }
    //endregion

    public interface Listener {
        void onItemClicked(int position);
    }
}
