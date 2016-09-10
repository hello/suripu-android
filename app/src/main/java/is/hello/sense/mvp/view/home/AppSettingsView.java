package is.hello.sense.mvp.view.home;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public final class AppSettingsView extends PresenterView {
    private ImageView breadcrumb;

    public AppSettingsView(@NonNull final Activity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public final View createView(@NonNull final LayoutInflater inflater,
                                 @NonNull final ViewGroup container,
                                 @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_settings, container, false);
    }

    public final void viewCreated(@NonNull final View view,
                                  @NonNull final ClickListenerGenerator generator,
                                  @NonNull final View.OnClickListener devicesListener,
                                  @NonNull final View.OnClickListener tellAFriendListener) {
        breadcrumb = (ImageView) view.findViewById(R.id.fragment_app_settings_breadcrumb);

        final View accountItem = view.findViewById(R.id.fragment_app_settings_account);
        Views.setSafeOnClickListener(accountItem, generator.create(AccountSettingsFragment.class, R.string.label_account, true));

        final View devicesItem = view.findViewById(R.id.fragment_app_settings_devices);
        Views.setSafeOnClickListener(devicesItem, devicesListener);

        final View notificationsItem = view.findViewById(R.id.fragment_app_settings_notifications);
        Views.setSafeOnClickListener(notificationsItem, generator.create(NotificationsSettingsFragment.class, R.string.label_notifications, false));

        final View unitsItem = view.findViewById(R.id.fragment_app_settings_units);
        Views.setSafeOnClickListener(unitsItem, generator.create(UnitSettingsFragment.class, R.string.label_units_and_time, false));

        final View supportItem = view.findViewById(R.id.fragment_app_settings_support);
        Views.setSafeOnClickListener(supportItem, ignored -> generator.create(SupportFragment.class, R.string.action_support, false));

        final View tellAFriendItem = view.findViewById(R.id.fragment_app_settings_tell_a_friend);
        Views.setSafeOnClickListener(tellAFriendItem, tellAFriendListener);

        final TextView version = (TextView) view.findViewById(R.id.fragment_app_settings_version);
        version.setText(context.getString(R.string.app_version_fmt, getString(R.string.app_name), BuildConfig.VERSION_NAME));
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            Views.setSafeOnClickListener(version, ignored -> Distribution.startDebugActivity((Activity) context));
        }
    }

    @Override
    public void pause() {
        breadcrumb.setVisibility(View.GONE);
    }

    @Override
    public void destroyView() {
        breadcrumb = null;
    }

    @Override
    public void detach() {
        super.detach();
        //todo remove click listeners from viewcreated
    }

    public final void setBreadcrumbVisible(final boolean visible) {
        breadcrumb.setVisibility(visible ? View.VISIBLE : View.GONE);
    }


    public interface ClickListenerGenerator {
        View.OnClickListener create(@NonNull final Class<? extends Fragment> fragmentClass,
                                    @StringRes
                                    final int titleRes,
                                    final boolean lockOrientation);
    }
}
