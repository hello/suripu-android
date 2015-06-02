package is.hello.sense.ui.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.fragments.settings.AppSettingsFragment;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;

import static is.hello.sense.ui.adapter.StaticFragmentAdapter.Item;

public class UndersideFragment extends Fragment implements ViewPager.OnPageChangeListener, SelectorView.OnSelectionChangedListener {
    public static final int ITEM_ROOM_CONDITIONS = 0;
    public static final int ITEM_TRENDS = 1;
    public static final int ITEM_INSIGHTS = 2;
    public static final int ITEM_SMART_ALARM_LIST = 3;
    public static final int ITEM_APP_SETTINGS = 4;

    private static final int DEFAULT_START_ITEM = ITEM_INSIGHTS;

    public static final int OPTION_NONE = 0;
    public static final int OPTION_ANIMATE = (1 << 1);
    public static final int OPTION_NOTIFY = (1 << 2);

    private SharedPreferences preferences;

    private SelectorView tabs;
    private TabsBackgroundDrawable tabLine;
    private ViewPager pager;
    private StaticFragmentAdapter adapter;

    private final int[] ICONS_INACTIVE = {
            R.drawable.underside_icon_currently,
            R.drawable.underside_icon_trends,
            R.drawable.underside_icon_insights,
            R.drawable.underside_icon_alarm,
            R.drawable.underside_icon_settings,
    };

    private final int[] ICONS_ACTIVE = {
            R.drawable.underside_icon_currently_active,
            R.drawable.underside_icon_trends_active,
            R.drawable.underside_icon_insights_active,
            R.drawable.underside_icon_alarm_active,
            R.drawable.underside_icon_settings_active,
    };


    private static SharedPreferences getPreferences(@NonNull Context context) {
        return context.getSharedPreferences(Constants.INTERNAL_PREFS, 0);
    }

    public static void saveCurrentItem(@NonNull Context context, int currentItem) {
        SharedPreferences preferences = getPreferences(context);
        preferences.edit()
                   .putInt(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM, currentItem)
                   .putLong(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM_LAST_UPDATED, System.currentTimeMillis())
                   .apply();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.preferences = getPreferences(getActivity());

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_TOP_VIEW, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_underside, container, false);

        Resources resources = getResources();

        this.pager = (ViewPager) view.findViewById(R.id.fragment_underside_pager);
        this.adapter = new StaticFragmentAdapter(getChildFragmentManager(),
                new Item(RoomConditionsFragment.class, getString(R.string.title_current_conditions)),
                new Item(TrendsFragment.class, getString(R.string.title_trends)),
                new Item(InsightsFragment.class, getString(R.string.action_insights)),
                new Item(SmartAlarmListFragment.class, getString(R.string.action_alarm)),
                new Item(AppSettingsFragment.class, getString(R.string.action_settings))
        );
        pager.setAdapter(adapter);

        long itemLastUpdated = preferences.getLong(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM_LAST_UPDATED, 0);
        if ((System.currentTimeMillis() - itemLastUpdated) <= Constants.STALE_INTERVAL_MS) {
            int currentItem = preferences.getInt(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM, 0);
            setCurrentItem(currentItem, OPTION_NONE);
        } else {
            setCurrentItem(DEFAULT_START_ITEM, OPTION_NONE);
        }

        pager.addOnPageChangeListener(this);

        this.tabs = (SelectorView) view.findViewById(R.id.fragment_underside_tabs);
        for (int i = 0; i < tabs.getButtonCount(); i++) {
            ToggleButton button = tabs.getButtonAt(i);

            SpannableString inactiveContent = createIconSpan(adapter.getPageTitle(i), ICONS_INACTIVE[i]);
            button.setText(inactiveContent);
            button.setTextOff(inactiveContent);

            SpannableString activeContent = createIconSpan(adapter.getPageTitle(i), ICONS_ACTIVE[i]);
            button.setTextOn(activeContent);

            button.setPadding(0, 0, 0, 0);
        }
        tabs.setSelectedIndex(pager.getCurrentItem());
        tabs.setOnSelectionChangedListener(this);

        this.tabLine = new TabsBackgroundDrawable(resources, TabsBackgroundDrawable.Style.UNDERSIDE);
        tabs.setBackground(tabLine);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        pager.clearOnPageChangeListeners();
    }

    @Override
    public void onResume() {
        super.onResume();

        UndersideTabFragment fragment = getCurrentTabFragment();
        if (fragment != null) {
            fragment.onUpdate();
        }
    }

    public boolean onBackPressed() {
        if (pager.getCurrentItem() != DEFAULT_START_ITEM) {
            setCurrentItem(DEFAULT_START_ITEM, OPTION_ANIMATE | OPTION_NOTIFY);
            return true;
        } else {
            return false;
        }
    }

    private SpannableString createIconSpan(@NonNull CharSequence title, @DrawableRes int iconRes) {
        ImageSpan image = new ImageSpan(getActivity(), iconRes);
        SpannableString span = new SpannableString(title);
        span.setSpan(image, 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }


    public @Nullable UndersideTabFragment getCurrentTabFragment() {
        // This depends on semi-undefined behavior. It may break in a future update
        // of the Android support library, but won't break if the host OS changes.
        long itemId = adapter.getItemId(pager.getCurrentItem());
        String tag = "android:switcher:" + pager.getId() + ":" + itemId;
        return (UndersideTabFragment) getChildFragmentManager().findFragmentByTag(tag);
    }

    public void notifyTabSelected(boolean withDelay) {
        Runnable notify = () -> {
            UndersideTabFragment fragment = getCurrentTabFragment();
            if (fragment != null) {
                fragment.tabSelected();
            }
        };

        if (withDelay) {
            pager.postDelayed(notify, 500);
        } else {
            pager.post(notify);
        }
    }

    public void setCurrentItem(int currentItem, int options) {
        boolean animate = ((options & OPTION_ANIMATE) == OPTION_ANIMATE);
        pager.setCurrentItem(currentItem, animate);

        if ((options & OPTION_NOTIFY) == OPTION_NOTIFY) {
            notifyTabSelected(true);
        }
    }

    public void saveCurrentItem(int currentItem) {
        preferences.edit()
                   .putInt(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM, currentItem)
                   .putLong(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM_LAST_UPDATED, System.currentTimeMillis())
                   .apply();
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        tabLine.setPositionOffset(positionOffset);
        tabLine.setSelectedIndex(position);
    }

    @Override
    public void onPageSelected(int position) {
        tabs.setSelectedIndex(position);
        saveCurrentItem(position);
        notifyTabSelected(true);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        setCurrentItem(newSelectionIndex, OPTION_NOTIFY | OPTION_ANIMATE);
    }
}
