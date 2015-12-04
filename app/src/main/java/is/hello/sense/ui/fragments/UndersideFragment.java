package is.hello.sense.ui.fragments;

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

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.UnreadStatePresenter;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.fragments.settings.AppSettingsFragment;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.ui.widget.TabsBackgroundDrawable;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;

import static is.hello.sense.ui.adapter.StaticFragmentAdapter.Item;

public class UndersideFragment extends InjectionFragment
        implements ViewPager.OnPageChangeListener, SelectorView.OnSelectionChangedListener {
    public static final int ITEM_ROOM_CONDITIONS = 0;
    public static final int ITEM_TRENDS = 1;
    public static final int ITEM_INSIGHTS = 2;
    public static final int ITEM_SMART_ALARM_LIST = 3;
    public static final int ITEM_APP_SETTINGS = 4;

    private static final int DEFAULT_START_ITEM = ITEM_INSIGHTS;

    public static final int OPTION_NONE = 0;
    public static final int OPTION_ANIMATE = (1 << 1);

    @Inject UnreadStatePresenter unreadStatePresenter;
    private SharedPreferences internalPreferences;

    private SelectorView tabs;
    private TabsBackgroundDrawable tabLine;
    private ViewPager pager;
    private StaticFragmentAdapter adapter;

    private boolean suppressNextSwipeEvent = false;
    private int lastState = ViewPager.SCROLL_STATE_IDLE;


    private static SharedPreferences getInternalPreferences(@NonNull Context context) {
        return context.getSharedPreferences(Constants.INTERNAL_PREFS, 0);
    }

    public static void saveCurrentItem(@NonNull Context context, int currentItem) {
        SharedPreferences preferences = getInternalPreferences(context);
        preferences.edit()
                   .putInt(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM, currentItem)
                   .putLong(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM_LAST_UPDATED, System.currentTimeMillis())
                   .apply();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.internalPreferences = getInternalPreferences(getActivity());

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_TOP_VIEW, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_underside, container, false);

        final Resources resources = getResources();

        this.pager = (ViewPager) view.findViewById(R.id.fragment_underside_pager);
        this.adapter = new StaticFragmentAdapter(getChildFragmentManager(),
                                                 new Item(RoomConditionsFragment.class, getString(R.string.title_current_conditions)),
                                                 new Item(TrendsFragment.class, getString(R.string.title_trends)),
                                                 new Item(InsightsFragment.class, getString(R.string.action_insights)),
                                                 new Item(SmartAlarmListFragment.class, getString(R.string.action_alarm)),
                                                 new Item(AppSettingsFragment.class, getString(R.string.action_settings)));
        pager.setAdapter(adapter);

        final long itemLastUpdated =
                internalPreferences.getLong(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM_LAST_UPDATED, 0);
        if ((System.currentTimeMillis() - itemLastUpdated) <= Constants.STALE_INTERVAL_MS) {
            final int currentItem =
                    internalPreferences.getInt(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM, 0);
            setCurrentItem(currentItem, OPTION_NONE);
        } else {
            setCurrentItem(DEFAULT_START_ITEM, OPTION_NONE);
        }

        pager.setPageMargin(resources.getDimensionPixelSize(R.dimen.divider_size));
        pager.setPageMarginDrawable(R.color.border);

        pager.addOnPageChangeListener(this);

        this.tabs = (SelectorView) view.findViewById(R.id.fragment_underside_tabs);
        final @DrawableRes int[] inactiveIcons = {
                R.drawable.underside_icon_currently,
                R.drawable.underside_icon_trends,
                R.drawable.underside_icon_insights,
                R.drawable.underside_icon_alarm,
                R.drawable.underside_icon_settings,
        };
        final @DrawableRes int[] activeIcons = {
                R.drawable.underside_icon_currently_active,
                R.drawable.underside_icon_trends_active,
                R.drawable.underside_icon_insights_active,
                R.drawable.underside_icon_alarm_active,
                R.drawable.underside_icon_settings_active,
        };
        for (int i = 0; i < tabs.getButtonCount(); i++) {
            final ToggleButton button = tabs.getButtonAt(i);

            final SpannableString inactiveContent = createIconSpan(adapter.getPageTitle(i),
                                                                   inactiveIcons[i]);
            button.setText(inactiveContent);
            button.setTextOff(inactiveContent);

            final SpannableString activeContent = createIconSpan(adapter.getPageTitle(i),
                                                                 activeIcons[i]);
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(unreadStatePresenter.hasUnreadItems,
                         this::setHasUnreadInsightItems,
                         Functions.LOG_ERROR);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        pager.clearOnPageChangeListeners();
    }

    @Override
    public void onResume() {
        super.onResume();

        final UndersideTabFragment fragment = getCurrentTabFragment();
        if (fragment != null) {
            fragment.onUpdate();
        }
    }

    public boolean onBackPressed() {
        if (pager.getCurrentItem() != DEFAULT_START_ITEM) {
            setCurrentItem(DEFAULT_START_ITEM, OPTION_ANIMATE);
            return true;
        } else {
            return false;
        }
    }

    private SpannableString createIconSpan(@NonNull CharSequence title, @DrawableRes int iconRes) {
        final ImageSpan image = new ImageSpan(getActivity(), iconRes);
        final SpannableString span = new SpannableString(title);
        span.setSpan(image, 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }


    public @Nullable UndersideTabFragment getCurrentTabFragment() {
        // This depends on semi-undefined behavior. It may break in a future update
        // of the Android support library, but won't break if the host OS changes.
        final long itemId = adapter.getItemId(pager.getCurrentItem());
        final String tag = "android:switcher:" + pager.getId() + ":" + itemId;
        return (UndersideTabFragment) getChildFragmentManager().findFragmentByTag(tag);
    }

    public void setCurrentItem(int currentItem, int options) {
        boolean animate = ((options & OPTION_ANIMATE) == OPTION_ANIMATE);
        pager.setCurrentItem(currentItem, animate);
    }

    public void saveCurrentItem(int currentItem) {
        internalPreferences.edit()
                           .putInt(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM, currentItem)
                           .putLong(Constants.INTERNAL_PREF_UNDERSIDE_CURRENT_ITEM_LAST_UPDATED,
                                    System.currentTimeMillis())
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
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (lastState != ViewPager.SCROLL_STATE_IDLE &&
                state == ViewPager.SCROLL_STATE_IDLE) {
            if (suppressNextSwipeEvent) {
                this.suppressNextSwipeEvent = false;
            } else {
                Analytics.trackEvent(Analytics.TopView.EVENT_TAB_SWIPED, null);
            }
        }

        this.lastState = state;
    }

    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        Analytics.trackEvent(Analytics.TopView.EVENT_TAB_TAPPED, null);
        this.suppressNextSwipeEvent = true;

        setCurrentItem(newSelectionIndex, OPTION_ANIMATE);
    }


    public void setHasUnreadInsightItems(boolean hasUnreadInsightItems) {
        final @DrawableRes int iconRes = hasUnreadInsightItems
                ? R.drawable.underside_icon_insights_unread
                : R.drawable.underside_icon_insights;

        final ToggleButton button = tabs.getButtonAt(ITEM_INSIGHTS);
        final SpannableString inactiveContent = createIconSpan(adapter.getPageTitle(ITEM_INSIGHTS),
                                                               iconRes);
        button.setTextOff(inactiveContent);
        if (!button.isChecked()) {
            button.setText(inactiveContent);
        }
    }
}
