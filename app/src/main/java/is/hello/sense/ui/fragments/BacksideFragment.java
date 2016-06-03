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

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.UnreadStatePresenter;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.fragments.settings.AppSettingsFragment;
import is.hello.sense.ui.fragments.sounds.SoundsFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;

import static is.hello.sense.ui.adapter.StaticFragmentAdapter.Item;

public class BacksideFragment extends InjectionFragment
        implements ViewPager.OnPageChangeListener, SelectorView.OnSelectionChangedListener {
    public static final int ITEM_ROOM_CONDITIONS = 0;
    public static final int ITEM_TRENDS = 1;
    public static final int ITEM_INSIGHTS = 2;
    public static final int ITEM_SOUNDS = 3;
    public static final int ITEM_APP_SETTINGS = 4;

    private static final int DEFAULT_START_ITEM = ITEM_INSIGHTS;

    public static final int OPTION_NONE = 0;
    public static final int OPTION_ANIMATE = (1 << 1);

    @Inject
    UnreadStatePresenter unreadStatePresenter;

    @Inject
    AccountPresenter accountPresenter;

    private SharedPreferences internalPreferences;

    private int tabSelectorHeight;
    private SelectorView tabSelector;
    private ExtendedViewPager pager;
    private StaticFragmentAdapter adapter;

    private boolean suppressNextSwipeEvent = false;
    private int lastState = ViewPager.SCROLL_STATE_IDLE;


    private static SharedPreferences getInternalPreferences(@NonNull Context context) {
        return context.getSharedPreferences(Constants.INTERNAL_PREFS, 0);
    }

    public static void saveCurrentItem(@NonNull Context context, int currentItem) {
        final SharedPreferences preferences = getInternalPreferences(context);
        preferences.edit()
                   .putInt(Constants.INTERNAL_PREF_BACKSIDE_CURRENT_ITEM, currentItem)
                   .putLong(Constants.INTERNAL_PREF_BACKSIDE_CURRENT_ITEM_LAST_UPDATED, System.currentTimeMillis())
                   .apply();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPresenter(accountPresenter);
        this.internalPreferences = getInternalPreferences(getActivity());

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Backside.EVENT_SHOWN, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_backside, container, false);

        final Resources resources = getResources();

        this.pager = (ExtendedViewPager) view.findViewById(R.id.fragment_backside_pager);
        this.adapter = new StaticFragmentAdapter(getChildFragmentManager(),
                                                 new Item(RoomConditionsFragment.class, getString(R.string.title_current_conditions)),
                                                 new Item(TrendsFragment.class, getString(R.string.title_trends)),
                                                 new Item(InsightsFragment.class, getString(R.string.action_insights)),
                                                 new Item(SoundsFragment.class, getString(R.string.action_alarm)),
                                                 new Item(AppSettingsFragment.class, getString(R.string.action_settings)));
        pager.setAdapter(adapter);

        final long itemLastUpdated =
                internalPreferences.getLong(Constants.INTERNAL_PREF_BACKSIDE_CURRENT_ITEM_LAST_UPDATED, 0);
        if ((System.currentTimeMillis() - itemLastUpdated) <= Constants.STALE_INTERVAL_MS) {
            final int currentItem =
                    internalPreferences.getInt(Constants.INTERNAL_PREF_BACKSIDE_CURRENT_ITEM, 0);
            setCurrentItem(currentItem, OPTION_NONE);
        } else {
            setCurrentItem(DEFAULT_START_ITEM, OPTION_NONE);
        }

        pager.addOnPageChangeListener(this);

        this.tabSelectorHeight = resources.getDimensionPixelSize(R.dimen.action_bar_height);
        this.tabSelector = (SelectorView) view.findViewById(R.id.fragment_backside_tabs);
        tabSelector.setButtonLayoutParams(new SelectorView.LayoutParams(0, SelectorView.LayoutParams.MATCH_PARENT, 1));
        final @DrawableRes int[] inactiveIcons = {
                R.drawable.backside_icon_currently,
                R.drawable.backside_icon_trends,
                R.drawable.backside_icon_insights,
                R.drawable.backside_icon_sounds,
                R.drawable.backside_icon_settings,
        };
        final @DrawableRes int[] activeIcons = {
                R.drawable.backside_icon_currently_active,
                R.drawable.backside_icon_trends_active,
                R.drawable.backside_icon_insights_active,
                R.drawable.backside_icon_sounds_active,
                R.drawable.backside_icon_settings_active,
        };
        for (int i = 0, count = adapter.getCount(); i < count; i++) {
            final SpannableString inactiveContent = createIconSpan(adapter.getPageTitle(i),
                                                                   inactiveIcons[i]);
            final SpannableString activeContent = createIconSpan(adapter.getPageTitle(i),
                                                                 activeIcons[i]);
            final ToggleButton button = tabSelector.addOption(activeContent, inactiveContent, false);
            button.setPadding(0, 0, 0, 0);
        }
        tabSelector.setSelectedIndex(pager.getCurrentItem());
        tabSelector.setOnSelectionChangedListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(unreadStatePresenter.hasUnreadItems,
                         this::setHasUnreadInsightItems,
                         Functions.LOG_ERROR);

        bindAndSubscribe(accountPresenter.account,
                         (a) -> {
                             if (pager.getCurrentItem() != ITEM_APP_SETTINGS
                                     && Tutorial.TAP_NAME.shouldShow(getActivity())
                                     && a.getCreated().isBefore(Constants.RELEASE_DATE_FOR_LAST_NAME)) {
                                 setHasUnreadAccountItems(true);
                             } else {
                                 setHasUnreadAccountItems(false);
                             }
                         },
                         Functions.LOG_ERROR);
        accountPresenter.update();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        pager.clearOnPageChangeListeners();
    }

    @Override
    public void onResume() {
        super.onResume();

        final BacksideTabFragment fragment = getCurrentTabFragment();
        if (fragment != null) {
            fragment.onUpdate();
        }
        accountPresenter.update();
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


    public
    @Nullable
    BacksideTabFragment getCurrentTabFragment() {
        if (adapter != null) {
            // This depends on semi-undefined behavior. It may break in a future update
            // of the Android support library, but won't break if the host OS changes.
            return (BacksideTabFragment) getChildFragmentManager().findFragmentByTag(getItemTag(pager.getCurrentItem()));
        } else {
            return null;
        }
    }

    public void setCurrentItem(int currentItem, int options) {
        boolean animate = ((options & OPTION_ANIMATE) == OPTION_ANIMATE);
        pager.setCurrentItem(currentItem, animate);
    }

    public void saveCurrentItem(int currentItem) {
        internalPreferences.edit()
                           .putInt(Constants.INTERNAL_PREF_BACKSIDE_CURRENT_ITEM, currentItem)
                           .putLong(Constants.INTERNAL_PREF_BACKSIDE_CURRENT_ITEM_LAST_UPDATED,
                                    System.currentTimeMillis())
                           .apply();
    }

    private String getItemTag(int position) {
        if (position < 0 || position > adapter.getCount() - 1) {
            position = pager.getCurrentItem();
        }
        return "android:switcher:" + pager.getId() + ":" + adapter.getItemId(position);
    }

    public void setChromeTranslationAmount(float amount) {
        final float tabsTranslationY = Anime.interpolateFloats(amount, 0f, -tabSelectorHeight);
        tabSelector.setTranslationY(tabsTranslationY);

        final float buttonAlpha = Anime.interpolateFloats(amount, 1f, 0f);
        for (int i = 0, count = tabSelector.getButtonCount(); i < count; i++) {
            tabSelector.getButtonAt(i).setAlpha(buttonAlpha);
        }
    }

    public float getChromeTranslationAmount() {
        return (-tabSelector.getTranslationY() / tabSelectorHeight);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        tabSelector.setSelectedIndex(position);
        saveCurrentItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (lastState == ViewPager.SCROLL_STATE_IDLE &&
                state != ViewPager.SCROLL_STATE_IDLE) {
            getAnimatorContext().beginAnimation("Backside swipe");
        } else if (lastState != ViewPager.SCROLL_STATE_IDLE &&
                state == ViewPager.SCROLL_STATE_IDLE) {
            if (suppressNextSwipeEvent) {
                this.suppressNextSwipeEvent = false;
            } else {
                Analytics.trackEvent(Analytics.Backside.EVENT_TAB_SWIPED, null);
            }
            getAnimatorContext().endAnimation("Backside swipe");
        }

        this.lastState = state;
    }

    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        Analytics.trackEvent(Analytics.Backside.EVENT_TAB_TAPPED, null);
        this.suppressNextSwipeEvent = true;

        setCurrentItem(newSelectionIndex, OPTION_ANIMATE);
    }


    public void setHasUnreadInsightItems(boolean hasUnreadInsightItems) {
        final @DrawableRes int iconRes = hasUnreadInsightItems
                ? R.drawable.backside_icon_insights_unread
                : R.drawable.backside_icon_insights;

        final ToggleButton button = tabSelector.getButtonAt(ITEM_INSIGHTS);
        final SpannableString inactiveContent = createIconSpan(adapter.getPageTitle(ITEM_INSIGHTS),
                                                               iconRes);
        button.setTextOff(inactiveContent);
        if (!button.isChecked()) {
            button.setText(inactiveContent);
        }
    }

    public void setHasUnreadAccountItems(boolean hasUnreadAccountItems) {
        final @DrawableRes int iconRes = hasUnreadAccountItems
                ? R.drawable.backside_icon_settings_unread
                : R.drawable.backside_icon_settings;

        final ToggleButton button = tabSelector.getButtonAt(ITEM_APP_SETTINGS);
        final SpannableString inactiveContent = createIconSpan(adapter.getPageTitle(ITEM_APP_SETTINGS),
                                                               iconRes);
        button.setTextOff(inactiveContent);
        if (!button.isChecked()) {
            button.setText(inactiveContent);
        }
    }
}
