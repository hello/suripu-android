package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.ViewPagerAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.PageDots;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class OnboardingSenseColorsFragment extends InjectionFragment implements ViewPager.OnPageChangeListener {
    @Inject RoomConditionsPresenter presenter;

    private ViewPager viewPager;
    private ViewGroup bottomContainer;
    private Button nextButton;

    private int finalItem;
    private float nextButtonMaxY, nextButtonMinY;

    private boolean hasCurrentConditions = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SENSE_COLORS, null);
        }

        presenter.update();
        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_sense_colors, container, false);

        this.viewPager = (ViewPager) view.findViewById(R.id.fragment_onboarding_sense_colors_pager);
        viewPager.addOnPageChangeListener(this);

        ColorsAdapter adapter = new ColorsAdapter(
            new SenseColor(R.string.title_sense_colors_1, R.string.info_sense_colors_1, R.drawable.onboarding_sense_colors_1),
            new SenseColor(R.string.title_sense_colors_2, R.string.info_sense_colors_2, R.drawable.onboarding_sense_colors_2),
            new SenseColor(R.string.title_sense_colors_3, R.string.info_sense_colors_3, R.drawable.onboarding_sense_colors_3),
            new SenseColor(R.string.title_sense_colors_4, R.string.info_sense_colors_4, R.drawable.onboarding_sense_colors_4),
            new SenseColor(R.string.title_sense_colors_5, R.string.info_sense_colors_5, R.drawable.onboarding_sense_colors_5)
        );
        viewPager.setAdapter(adapter);


        this.finalItem = adapter.getCount() - 1;

        PageDots pageDots = (PageDots) view.findViewById(R.id.fragment_onboarding_sense_colors_dots);
        pageDots.attach(viewPager);

        this.nextButton = (Button) view.findViewById(R.id.fragment_onboarding_sense_colors_continue);
        Views.setSafeOnClickListener(nextButton, this::next);

        this.bottomContainer = (ViewGroup) view.findViewById(R.id.fragment_onboarding_sense_colors_bottom);
        bottomContainer.forceLayout();
        bottomContainer.post(() -> {
            this.nextButtonMaxY = bottomContainer.getMeasuredHeight();
            this.nextButtonMinY = (bottomContainer.getMeasuredHeight() / 2) - (nextButton.getMeasuredHeight() / 2);
            nextButton.setY(nextButtonMaxY);
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.currentConditions,
                         conditions -> {
                             this.hasCurrentConditions = !conditions.conditions.isEmpty();
                         },
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Could not load conditions", e);
                             this.hasCurrentConditions = false;
                         });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        viewPager.clearOnPageChangeListeners();
    }

    public void next(@NonNull View sender) {
        if (hasCurrentConditions) {
            ((OnboardingActivity) getActivity()).showRoomCheckIntro();
        } else {
            ((OnboardingActivity) getActivity()).showSmartAlarmInfo();
        }
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (position == finalItem - 1) {
            float newY = Anime.interpolateFloats(positionOffset, nextButtonMaxY, nextButtonMinY);
            nextButton.setY(newY);
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (position == finalItem) {
            bottomContainer.post(() -> nextButton.setY(nextButtonMinY));
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }


    private class ColorsAdapter extends ViewPagerAdapter<ColorsAdapter.ViewHolder> {
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

        public SenseColor getItem(int position) {
            return senseColors[position];
        }

        @Override
        public ViewHolder createViewHolder(ViewGroup container, int position) {
            View view = inflater.inflate(R.layout.item_onboarding_sense_color, container, false);
            return new ViewHolder(view);
        }

        @Override
        public void bindViewHolder(ViewHolder holder, int position) {
            SenseColor color = getItem(position);
            holder.bindSenseColor(color);
        }


        class ViewHolder extends ViewPagerAdapter.ViewHolder {
            private final TextView headingText;
            private final TextView subheadingText;
            private final ImageView diagramImage;

            private ViewHolder(@NonNull View itemView) {
                super(itemView);

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
