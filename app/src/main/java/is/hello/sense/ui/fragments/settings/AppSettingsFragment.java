package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.HelpUtil;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.BuildValues;

import static android.widget.LinearLayout.LayoutParams;

public class AppSettingsFragment extends InjectionFragment {
    @Inject ApiSessionManager sessionManager;
    @Inject BuildValues buildValues;

    private final LayoutParams itemTextLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    private LayoutParams dividerLayoutParams;
    private int itemTextHorizontalPadding, itemTextVerticalPadding;

    private LinearLayout itemContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources resources = getResources();
        this.dividerLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.divider_height));
        this.itemTextHorizontalPadding = resources.getDimensionPixelSize(R.dimen.gap_outer);
        this.itemTextVerticalPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_settings, container, false);

        this.itemContainer = (LinearLayout) view.findViewById(R.id.fragment_app_settings_container);
        addItem(R.string.label_my_info, true, ignored -> showFragment(MyInfoFragment.class, R.string.label_my_info, null));
        addItem(R.string.label_account, true, ignored -> showFragment(AccountSettingsFragment.class, R.string.label_account, null));
        addItem(R.string.label_units_and_time, true, ignored -> showFragment(R.xml.settings_units_and_time, R.string.label_units_and_time));
        addItem(R.string.label_devices, true, ignored -> showFragment(DeviceListFragment.class, R.string.label_devices, null));
        addItem(R.string.action_help, true, this::showHelp);
        addItem(R.string.action_log_out, buildValues.debugScreenEnabled, this::logOut);

        if (buildValues.debugScreenEnabled) {
            addItem(R.string.activity_debug, false, ignored -> startActivity(new Intent(getActivity(), DebugActivity.class)));
        }

        return view;
    }


    public void addItem(@StringRes int titleRes, boolean wantsDivider, @NonNull View.OnClickListener onClick) {
        TextView itemView = new TextView(getActivity());
        itemView.setBackgroundResource(R.drawable.button_selector_dark);
        itemView.setTextAppearance(getActivity(), R.style.AppTheme_Text_Body_Light);
        itemView.setPadding(itemTextHorizontalPadding, itemTextVerticalPadding, itemTextHorizontalPadding, itemTextVerticalPadding);
        itemView.setText(titleRes);
        itemView.setOnClickListener(onClick);
        itemContainer.addView(itemView, itemTextLayoutParams);

        if (wantsDivider) {
            View divider = new View(getActivity());
            divider.setBackgroundResource(R.color.border);
            itemContainer.addView(divider, dividerLayoutParams);
        }
    }


    //region Actions

    private void showFragment(@NonNull Class<? extends Fragment> fragmentClass,
                              @StringRes int titleRes,
                              @Nullable Bundle fragmentArguments) {
        Bundle intentArguments = FragmentNavigationActivity.getArguments(getString(titleRes), fragmentClass, fragmentArguments);
        Intent intent = new Intent(getActivity(), FragmentNavigationActivity.class);
        intent.putExtras(intentArguments);
        startActivity(intent);
    }

    private void showFragment(@XmlRes int prefsRes,
                              @StringRes int titleRes) {
        showFragment(StaticPreferencesFragment.class, titleRes, StaticPreferencesFragment.getArguments(prefsRes));
    }

    public void showHelp(@NonNull View sender) {
        HelpUtil.showHelp(getActivity());
    }

    public void logOut(@NonNull View sender) {
        SenseAlertDialog builder = new SenseAlertDialog(getActivity());
        builder.setTitle(R.string.dialog_title_log_out);
        builder.setMessage(R.string.dialog_message_log_out);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            Activity activity = getActivity();
            sessionManager.logOut(activity);
            activity.finish();

            Analytics.event(Analytics.EVENT_SIGNED_OUT, null);
        });
        builder.show();
    }

    //endregion
}
