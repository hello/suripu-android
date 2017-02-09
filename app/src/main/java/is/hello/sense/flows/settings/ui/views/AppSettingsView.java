package is.hello.sense.flows.settings.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.R;
import is.hello.sense.databinding.ViewAppSettingsBinding;
import is.hello.sense.mvp.view.BindedPresenterView;

@SuppressLint("ViewConstructor")
public class AppSettingsView extends BindedPresenterView<ViewAppSettingsBinding> {
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
        this.binding.viewAppSettingsMyAccount.setOnClickListener(v -> onClicked(INDEX_ACCOUNT));
        this.binding.viewAppSettingsDevices.setOnClickListener(v -> onClicked(INDEX_DEVICES));
        this.binding.viewAppSettingsNotifications.setOnClickListener(v -> onClicked(INDEX_NOTIFICATIONS));
        this.binding.viewAppSettingsExpansions.setOnClickListener(v -> onClicked(INDEX_EXPANSIONS));
        this.binding.viewAppSettingsVoice.setOnClickListener(v -> onClicked(INDEX_VOICE));
        this.binding.viewAppSettingsSupport.setOnClickListener(v -> onClicked(INDEX_SUPPORT));
        this.binding.viewAppSettingsShare.setOnClickListener(v -> onClicked(INDEX_SHARE));
        this.binding.viewAppSettingsDebug.setOnClickListener(v -> onClicked(INDEX_DEBUG));
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
            this.binding.viewAppSettingsExpansions.setVisibility(VISIBLE);
            this.binding.viewAppSettingsVoice.setVisibility(VISIBLE);
        } else {
            this.binding.viewAppSettingsExpansions.setVisibility(GONE);
            this.binding.viewAppSettingsVoice.setVisibility(GONE);
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
