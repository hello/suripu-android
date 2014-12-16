package is.hello.sense.ui.fragments;

import android.app.Fragment;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import static is.hello.sense.ui.adapter.StaticFragmentAdapter.Item;

public class UndersideFragment extends Fragment implements ViewPager.OnPageChangeListener, SelectorLinearLayout.OnSelectionChangedListener {
    private SelectorLinearLayout tabs;
    private BottomLineDrawable tabLine;
    private ViewPager pager;

    private static int[] getButtonIcons() {
        return new int[] {
                R.drawable.underside_icon_currently,
                R.drawable.underside_icon_trends,
                R.drawable.underside_icon_insights,
                R.drawable.underside_icon_alarm,
                R.drawable.underside_icon_settings,
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_underside, container, false);

        this.pager = (ViewPager) view.findViewById(R.id.fragment_underside_pager);
        pager.setOnPageChangeListener(this);
        pager.setAdapter(new StaticFragmentAdapter(getFragmentManager(),
                new Item(CurrentConditionsFragment.class, getString(R.string.title_current_conditions)),
                new Item(Fragment.class, getString(R.string.title_trends)),
                new Item(InsightsFragment.class, getString(R.string.action_insights)),
                new Item(SmartAlarmListFragment.class, getString(R.string.action_alarm)),
                new Item(AppSettingsFragment.class, getString(R.string.action_settings))
        ));

        this.tabs = (SelectorLinearLayout) view.findViewById(R.id.fragment_underside_tabs);
        List<ToggleButton> toggleButtons = tabs.getToggleButtons();
        int[] iconResources = getButtonIcons();
        for (int i = 0; i < toggleButtons.size(); i++) {
            ToggleButton button = toggleButtons.get(i);
            ImageSpan imageSpan = new ImageSpan(getActivity(), iconResources[i]);
            SpannableString content = new SpannableString("X");
            content.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            button.setText(content);
            button.setTextOn(content);
            button.setTextOff(content);
            button.setPadding(0, 0, 0, 0);
            button.setBackground(null);
            button.setTag(R.id.underside_icon, imageSpan.getDrawable());
        }
        int accentColor = getResources().getColor(R.color.light_accent);
        tabs.setButtonStyler((button, checked) -> {
            Drawable icon = (Drawable) button.getTag(R.id.underside_icon);
            if (checked) {
                icon.setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);
            } else {
                icon.setColorFilter(null);
            }
        });
        tabs.setSelectedIndex(pager.getCurrentItem());
        tabs.setOnSelectionChangedListener(this);

        this.tabLine = new BottomLineDrawable(pager.getAdapter().getCount(), getResources().getDimensionPixelSize(R.dimen.shadow_height));
        tabLine.setFillColor(getResources().getColor(R.color.light_accent));
        tabLine.setBackgroundColor(Color.WHITE);
        tabs.setBackground(tabLine);

        return view;
    }


    public boolean isAtStart() {
        return (pager.getCurrentItem() == 0);
    }

    public void jumpToStart() {
        pager.setCurrentItem(0, true);
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        tabLine.setPositionOffset(positionOffset);
        tabLine.setSelectedItem(position);
    }

    @Override
    public void onPageSelected(int position) {
        tabs.setSelectedIndex(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        pager.setCurrentItem(newSelectionIndex, true);
    }


    static class BottomLineDrawable extends Drawable {
        private final Paint backgroundPaint = new Paint();
        private final Paint fillPaint = new Paint();

        private final int itemCount;
        private final int lineHeight;

        private int selectedItem = 0;
        private float positionOffset = 0f;

        BottomLineDrawable(int itemCount, int lineHeight) {
            this.itemCount = itemCount;
            this.lineHeight = lineHeight;
        }

        @Override
        public void draw(Canvas canvas) {
            int width = canvas.getWidth();
            int height = canvas.getHeight();

            canvas.drawRect(0, 0, width, height, backgroundPaint);

            int itemWidth = width / itemCount;
            float itemOffset = (itemWidth * selectedItem) + (itemWidth * positionOffset);

            canvas.drawRect(itemOffset, height - lineHeight, itemOffset + itemWidth, height, fillPaint);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }


        //region Attributes

        @Override
        public void setAlpha(int alpha) {
            fillPaint.setAlpha(alpha);
            backgroundPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            fillPaint.setColorFilter(colorFilter);
        }

        public void setSelectedItem(int selectedItem) {
            this.selectedItem = selectedItem;
            invalidateSelf();
        }

        public void setBackgroundColor(int color) {
            backgroundPaint.setColor(color);
            invalidateSelf();
        }

        public void setFillColor(int color) {
            fillPaint.setColor(color);
            invalidateSelf();
        }

        public void setPositionOffset(float positionOffset) {
            this.positionOffset = positionOffset;
            invalidateSelf();
        }

        //endregion
    }
}
