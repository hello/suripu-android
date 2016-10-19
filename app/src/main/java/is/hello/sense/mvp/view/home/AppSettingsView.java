package is.hello.sense.mvp.view.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;
import is.hello.sense.ui.fragments.settings.NotificationsSettingsFragment;
import is.hello.sense.ui.fragments.settings.UnitSettingsFragment;
import is.hello.sense.ui.fragments.support.SupportFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Distribution;

@SuppressLint("ViewConstructor")
public class AppSettingsView extends PresenterView {
    private final ImageView breadcrumb;
    private final View accountItem;
    private final View devicesItem;
    private final View notificationsItem;
    private final View unitsItem;
    private final View supportItem;
    private final View tellAFriendItem;
    private final TextView version;
    //todo when we have time optimize how these rows are created. Maybe use a recycler view
    private final View expansionItem;
    private final View voiceItem;

    public AppSettingsView(@NonNull final Activity activity,
                           @NonNull final ClickListenerGenerator generator,
                           @NonNull final View.OnClickListener devicesListener,
                           @NonNull final View.OnClickListener tellAFriendListener,
                           @NonNull final View.OnClickListener expansionsListener,
                           @NonNull final View.OnClickListener voiceListener) {
        super(activity);

        this.breadcrumb = (ImageView) findViewById(R.id.fragment_app_settings_breadcrumb);

        this.accountItem = findViewById(R.id.fragment_app_settings_account);
        Views.setSafeOnClickListener(this.accountItem, generator.create(AccountSettingsFragment.class, R.string.label_account, true));

        this.devicesItem = findViewById(R.id.fragment_app_settings_devices);
        Views.setSafeOnClickListener(this.devicesItem, devicesListener);

        this.notificationsItem = findViewById(R.id.fragment_app_settings_notifications);
        Views.setSafeOnClickListener(this.notificationsItem, generator.create(NotificationsSettingsFragment.class, R.string.label_notifications, false));

        this.unitsItem = findViewById(R.id.fragment_app_settings_units);
        Views.setSafeOnClickListener(this.unitsItem, generator.create(UnitSettingsFragment.class, R.string.label_units_and_time, false));

        this.expansionItem = findViewById(R.id.fragment_app_settings_expansions);

        Views.setSafeOnClickListener(this.expansionItem, expansionsListener);

        this.voiceItem = findViewById(R.id.fragment_app_settings_voice);

        Views.setSafeOnClickListener(this.expansionItem, voiceListener);
        showVoiceEnabledRows(false);

        this.supportItem = findViewById(R.id.fragment_app_settings_support);
        Views.setSafeOnClickListener(this.supportItem, generator.create(SupportFragment.class, R.string.action_support, false));

        this.tellAFriendItem = findViewById(R.id.fragment_app_settings_tell_a_friend);
        Views.setSafeOnClickListener(this.tellAFriendItem, tellAFriendListener);

        this.version = (TextView) findViewById(R.id.fragment_app_settings_version);
        this.version.setText(context.getString(R.string.app_version_fmt, getString(R.string.app_name), BuildConfig.VERSION_NAME));
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            Views.setSafeOnClickListener(this.version, ignored -> Distribution.startDebugActivity((Activity) super.context));
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_app_settings;
    }


    @Override
    public final void pause() {
        this.breadcrumb.setVisibility(View.GONE);
    }

    @Override
    public final void releaseViews() {
        accountItem.setOnClickListener(null);
        devicesItem.setOnClickListener(null);
        notificationsItem.setOnClickListener(null);
        unitsItem.setOnClickListener(null);
        expansionItem.setOnClickListener(null);
        supportItem.setOnClickListener(null);
        tellAFriendItem.setOnClickListener(null);
        version.setOnClickListener(null);
        voiceItem.setOnClickListener(null);
    }

    public void setBreadcrumbVisible(final boolean visible) {
        this.breadcrumb.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public final void showVoiceEnabledRows(final boolean show) {
        if (show) {
            this.expansionItem.setVisibility(VISIBLE);
            this.voiceItem.setVisibility(VISIBLE);
        } else {
            this.expansionItem.setVisibility(GONE);
            this.voiceItem.setVisibility(GONE);
        }
    }

    public interface ClickListenerGenerator {
        View.OnClickListener create(@NonNull final Class<? extends Fragment> fragmentClass,
                                    @StringRes
                                    final int titleRes,
                                    final boolean lockOrientation);
    }
}
