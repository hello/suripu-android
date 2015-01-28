package is.hello.sense.ui.fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
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

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.fragments.settings.AppSettingsFragment;
import is.hello.sense.ui.widget.SelectorLinearLayout;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;

import static is.hello.sense.ui.adapter.StaticFragmentAdapter.Item;

public class UndersideFragment extends Fragment implements ViewPager.OnPageChangeListener, SelectorLinearLayout.OnSelectionChangedListener {
    private static final int DEFAULT_START_ITEM = 0;

    private SharedPreferences preferences;

    private SelectorLinearLayout tabs;
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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.preferences = getActivity().getSharedPreferences(Constants.INTERNAL_PREFS, 0);

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
            setCurrentItem(currentItem, false);
        } else {
            setCurrentItem(DEFAULT_START_ITEM, false);
        }

        pager.setOnPageChangeListener(this);

        this.tabs = (SelectorLinearLayout) view.findViewById(R.id.fragment_underside_tabs);
        List<ToggleButton> toggleButtons = tabs.getToggleButtons();
        for (int i = 0; i < toggleButtons.size(); i++) {
            ToggleButton button = toggleButtons.get(i);

            SpannableString inactiveContent = createIconSpan(ICONS_INACTIVE[i]);
            button.setText(inactiveContent);
            button.setTextOff(inactiveContent);

            SpannableString activeContent = createIconSpan(ICONS_ACTIVE[i]);
            button.setTextOn(activeContent);

            button.setPadding(0, 0, 0, 0);
            button.setBackground(null);
        }
        tabs.setSelectedIndex(pager.getCurrentItem());
        tabs.setOnSelectionChangedListener(this);

        this.tabLine = new TabsBackgroundDrawable(resources, pager.getAdapter().getCount());
        tabs.setBackground(tabLine);

        return view;
    }

    private SpannableString createIconSpan(@DrawableRes int iconRes) {
        ImageSpan image = new ImageSpan(getActivity(), iconRes);
        SpannableString span = new SpannableString("X");
        span.setSpan(image, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }


    public boolean isAtStart() {
        return (pager.getCurrentItem() == 0);
    }

    public void jumpToStart() {
        setCurrentItem(0, true);
    }


    public void notifyPageSelected() {
        pager.postDelayed(() -> {
            // This depends on semi-undefined behavior. It may break in a future update
            // of the Android support library, but won't break if the host OS changes.
            long itemId = adapter.getItemId(pager.getCurrentItem());
            String tag = "android:switcher:" + pager.getId() + ":" + itemId;
            UndersideTabFragment fragment = (UndersideTabFragment) getChildFragmentManager().findFragmentByTag(tag);
            fragment.pageSelected();
        }, 500);
    }

    public void setCurrentItem(int currentItem, boolean animate) {
        pager.setCurrentItem(currentItem, animate);
        notifyPageSelected();
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
        tabLine.setSelectedItem(position);
    }

    @Override
    public void onPageSelected(int position) {
        tabs.setSelectedIndex(position);
        saveCurrentItem(position);
        notifyPageSelected();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        setCurrentItem(newSelectionIndex, true);
    }


    static class TabsBackgroundDrawable extends Drawable {
        private final Paint paint = new Paint();

        private final int itemCount;
        private final int lineHeight;
        private final int dividerHeight;

        private final int backgroundColor = Color.WHITE;
        private final int fillColor;
        private final int dividerColor;

        private int selectedItem = 0;
        private float positionOffset = 0f;

        TabsBackgroundDrawable(@NonNull Resources resources, int itemCount) {
            this.itemCount = itemCount;
            this.lineHeight = resources.getDimensionPixelSize(R.dimen.bottom_line);
            this.dividerHeight = resources.getDimensionPixelSize(R.dimen.divider_size);
            this.fillColor = resources.getColor(R.color.light_accent);
            this.dividerColor = resources.getColor(R.color.border);
        }

        @Override
        public void draw(Canvas canvas) {
            int width = canvas.getWidth();
            int height = canvas.getHeight() - dividerHeight;

            
            paint.setColor(backgroundColor);
            canvas.drawRect(0, 0, width, height, paint);


            int itemWidth = width / itemCount;
            float itemOffset = (itemWidth * selectedItem) + (itemWidth * positionOffset);
            paint.setColor(fillColor);
            canvas.drawRect(itemOffset, height - lineHeight, itemOffset + itemWidth, height, paint);


            paint.setColor(dividerColor);
            canvas.drawRect(0, height, width, height + dividerHeight, paint);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }


        //region Attributes

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        public void setSelectedItem(int selectedItem) {
            this.selectedItem = selectedItem;
            invalidateSelf();
        }

        public void setPositionOffset(float positionOffset) {
            this.positionOffset = positionOffset;
            invalidateSelf();
        }

        //endregion
    }
}
