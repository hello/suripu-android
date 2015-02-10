package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.widget.PageDots;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class OnboardingSenseColorsFragment extends SenseFragment implements ViewPager.OnPageChangeListener {
    private ViewGroup bottomContainer;
    private Button nextButton;

    private int finalItem;
    private float nextButtonMaxY, nextButtonMinY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SENSE_COLORS, null);
        }

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_sense_colors, container, false);

        ViewPager viewPager = (ViewPager) view.findViewById(R.id.fragment_onboarding_sense_colors_pager);

        ColorsAdapter adapter = new ColorsAdapter(
            new SenseColor(R.string.title_sense_colors_1, R.string.info_sense_colors_1, R.drawable.onboarding_sense_colors_1),
            new SenseColor(R.string.title_sense_colors_2, R.string.info_sense_colors_2, R.drawable.onboarding_sense_colors_2),
            new SenseColor(R.string.title_sense_colors_3, R.string.info_sense_colors_3, R.drawable.onboarding_sense_colors_3),
            new SenseColor(R.string.title_sense_colors_4, R.string.info_sense_colors_4, R.drawable.onboarding_sense_colors_4),
            new SenseColor(R.string.title_sense_colors_5, R.string.info_sense_colors_5, R.drawable.onboarding_sense_colors_5)
        );
        viewPager.setAdapter(adapter);

        this.finalItem = adapter.getCount() - 1;

        this.bottomContainer = (ViewGroup) view.findViewById(R.id.fragment_onboarding_sense_colors_bottom);
        Views.observeNextLayout(bottomContainer).subscribe(ignored -> {
            this.nextButtonMaxY = bottomContainer.getMeasuredHeight();
            this.nextButtonMinY = nextButton.getY();
            if (viewPager.getCurrentItem() == finalItem) {
                nextButton.setY(nextButtonMinY);
            } else {
                nextButton.setY(nextButtonMaxY);
            }
        });

        PageDots pageDots = (PageDots) view.findViewById(R.id.fragment_onboarding_sense_colors_dots);
        pageDots.setOnPageChangeListener(this);
        pageDots.attach(viewPager);

        this.nextButton = (Button) view.findViewById(R.id.fragment_onboarding_sense_colors_continue);
        Views.setSafeOnClickListener(nextButton, this::next);

        return view;
    }


    public void next(@NonNull View sender) {
        ((OnboardingActivity) getActivity()).showRoomCheckIntro();
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (position == finalItem - 1) {
            float newY = Animations.interpolateFrame(positionOffset, nextButtonMaxY, nextButtonMinY);
            nextButton.setY(newY);
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (position == finalItem && !bottomContainer.isDirty()) {
            nextButton.setY(nextButtonMinY);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }


    private class ColorsAdapter extends PagerAdapter {
        private final LayoutInflater inflater;
        private final SenseColor[] senseColors;

        private ColorsAdapter(@NonNull SenseColor... senseColors) {
            this.inflater = LayoutInflater.from(getActivity());
            this.senseColors = senseColors;
        }

        @Override
        public int getCount() {
            return senseColors.length;
        }

        public SenseColor getColor(int position) {
            return senseColors[position];
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            ViewHolder holder = (ViewHolder) object;
            return (holder.itemView == view);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = inflater.inflate(R.layout.item_onboarding_sense_color, container, false);
            ViewHolder holder = new ViewHolder(view);
            holder.bindSenseColor(getColor(position));
            container.addView(view);
            return holder;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ViewHolder holder = (ViewHolder) object;
            container.removeView(holder.itemView);
        }


        private class ViewHolder {
            private final View itemView;
            private final TextView headingText;
            private final TextView subheadingText;
            private final ImageView diagramImage;

            private ViewHolder(@NonNull View itemView) {
                this.itemView = itemView;

                this.headingText = (TextView) itemView.findViewById(R.id.item_onboarding_sense_color_heading);
                this.subheadingText = (TextView) itemView.findViewById(R.id.item_onboarding_sense_color_subheading);
                this.diagramImage = (ImageView) itemView.findViewById(R.id.item_onboarding_sense_color_diagram);
            }

            private void bindSenseColor(@NonNull SenseColor senseColor) {
                headingText.setText(senseColor.headingRes);
                subheadingText.setText(senseColor.subheadingRes);
                diagramImage.setImageResource(senseColor.diagramRes);
                diagramImage.setContentDescription(subheadingText.getText().toString());
            }
        }
    }

    private static class SenseColor {
        private final @StringRes int headingRes;
        private final @StringRes int subheadingRes;
        private final @DrawableRes int diagramRes;

        private SenseColor(@StringRes int headingRes,
                           @StringRes int subheadingRes,
                           @DrawableRes int diagramRes) {
            this.headingRes = headingRes;
            this.subheadingRes = subheadingRes;
            this.diagramRes = diagramRes;
        }
    }
}
