package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.os.Bundle;
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
import is.hello.sense.interactors.RoomConditionsInteractor;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.ViewPagerAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.PageDots;
import is.hello.sense.ui.widget.util.OnViewPagerChangeAdapter;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class OnboardingSenseColorsFragment extends InjectionFragment {
    private static final float BACKGROUND_SENSE_SCALE = 0.7f;

    private static final int POSITION_INTRO = 0;
    private static final int POSITION_GOOD = 1;
    private static final int POSITION_WARNING = 2;
    private static final int POSITION_ALERT = 3;
    private static final int POSITION_WAVE = 4;

    @Inject
    RoomConditionsInteractor presenter;

    private ImageView senseBackground, senseGreen, senseYellow, senseRed;
    private DiagramVideoView senseWave;

    private ViewPager viewPager;
    private PageDots pageDots;
    private OnViewPagerChangeAdapter pagerChangeAdapter;
    private ViewGroup bottomContainer;
    private Button nextButton;
    private ColorsAdapter adapter;

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
        final View view = inflater.inflate(R.layout.fragment_onboarding_sense_colors, container, false);

        this.senseBackground = (ImageView) view.findViewById(R.id.fragment_onboarding_sense_colors_background);
        this.senseGreen = (ImageView) view.findViewById(R.id.fragment_onboarding_sense_colors_green);
        this.senseYellow = (ImageView) view.findViewById(R.id.fragment_onboarding_sense_colors_yellow);
        senseYellow.setScaleX(BACKGROUND_SENSE_SCALE);
        senseYellow.setScaleY(BACKGROUND_SENSE_SCALE);
        this.senseRed = (ImageView) view.findViewById(R.id.fragment_onboarding_sense_colors_red);
        senseRed.setScaleX(BACKGROUND_SENSE_SCALE);
        senseRed.setScaleY(BACKGROUND_SENSE_SCALE);

        this.senseWave = (DiagramVideoView) view.findViewById(R.id.fragment_onboarding_sense_colors_final);
        senseWave.setAlpha(0f);

        this.viewPager = (ViewPager) view.findViewById(R.id.fragment_onboarding_sense_colors_pager);

        this.pagerChangeAdapter = new OnViewPagerChangeAdapter(viewPager, new PageChangeListener());
        viewPager.addOnPageChangeListener(pagerChangeAdapter);

        final SenseColor[] senseColors = {
                new SenseColor(R.string.title_sense_colors_intro, R.string.info_sense_colors_intro),
                new SenseColor(R.string.title_sense_colors_good, R.string.info_sense_colors_good),
                new SenseColor(R.string.title_sense_colors_warning, R.string.info_sense_colors_warning),
                new SenseColor(R.string.title_sense_colors_bad, R.string.info_sense_colors_bad),
                new SenseColor(R.string.title_sense_colors_end, R.string.info_sense_colors_end),
        };
        this.adapter = new ColorsAdapter(senseColors);
        viewPager.setAdapter(adapter);

        this.pageDots = (PageDots) view.findViewById(R.id.fragment_onboarding_sense_colors_dots);
        pageDots.attach(viewPager);

        this.nextButton = (Button) view.findViewById(R.id.fragment_onboarding_sense_colors_continue);
        Views.setSafeOnClickListener(nextButton, this::next);

        this.bottomContainer = (ViewGroup) view.findViewById(R.id.fragment_onboarding_sense_colors_bottom);
        bottomContainer.forceLayout();
        bottomContainer.post(() -> {
            this.nextButtonMaxY = bottomContainer.getMeasuredHeight();
            this.nextButtonMinY = (bottomContainer.getMeasuredHeight() / 2) - (nextButton.getMeasuredHeight() / 2);
            if (viewPager.getCurrentItem() == POSITION_WAVE) {
                nextButton.setY(nextButtonMinY);
            } else {
                nextButton.setY(nextButtonMaxY);
            }
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
    public void onPause() {
        super.onPause();

        senseWave.suspendPlayback(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (senseWave.getAlpha() > 0f) {
            senseWave.startPlayback();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        senseWave.destroy();
        pagerChangeAdapter.destroy();
        pageDots.detach();
        viewPager.clearOnPageChangeListeners();

        this.senseBackground = null;
        this.senseGreen = null;
        this.senseYellow = null;
        this.senseRed = null;
        this.senseWave = null;

        this.pageDots = null;
        this.pagerChangeAdapter = null;
        this.adapter = null;
    }

    public void next(@NonNull View sender) {
        if (nextButton.getY() != nextButtonMinY) {
            return;
        }

        if (hasCurrentConditions) {
            ((OnboardingActivity) getActivity()).showRoomCheckIntro();
        } else {
            getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, null);
        }
    }


    class PageChangeListener implements OnViewPagerChangeAdapter.Listener {
        @Override
        public void onPageChangeScrolled(int position, float offset) {
            if (position == POSITION_INTRO) {
                final int maxTranslation = Views.getCenterX(senseGreen) - Views.getCenterX(senseYellow);
                final float translation = Anime.interpolateFloats(offset, 0f, maxTranslation);

                senseYellow.setTranslationX(translation);
                senseYellow.setScaleX(BACKGROUND_SENSE_SCALE);
                senseYellow.setScaleY(BACKGROUND_SENSE_SCALE);
                senseYellow.setAlpha(1f);

                senseRed.setTranslationX(-translation);
                senseRed.setScaleX(BACKGROUND_SENSE_SCALE);
                senseRed.setScaleY(BACKGROUND_SENSE_SCALE);
                senseRed.setAlpha(1f);
            } else if (position == POSITION_GOOD) {
                senseGreen.setAlpha(1f - offset);
                senseYellow.setAlpha(offset);
            } else if (position == POSITION_WARNING) {
                senseYellow.setAlpha(1f - offset);
                senseRed.setAlpha(offset);
            } else if (position == POSITION_ALERT) {
                senseBackground.setAlpha(1f - offset);
                senseRed.setAlpha(1f - offset);

                senseWave.setAlpha(offset);

                final float newY = Anime.interpolateFloats(offset, nextButtonMaxY, nextButtonMinY);
                nextButton.setY(newY);
            }
        }

        @Override
        public void onPageChangeCompleted(int position) {
            if (position > POSITION_INTRO) {
                final int translation = Views.getCenterX(senseGreen) - Views.getCenterX(senseYellow);
                senseYellow.setTranslationX(translation);
                senseYellow.setScaleX(1f);
                senseYellow.setScaleY(1f);

                senseRed.setTranslationX(-translation);
                senseRed.setScaleX(1f);
                senseRed.setScaleY(1f);
            }

            if (position == POSITION_GOOD) {
                senseYellow.setAlpha(0f);
                senseRed.setAlpha(0f);
            } else if (position == POSITION_WARNING) {
                senseGreen.setAlpha(0f);
                senseYellow.setAlpha(1f);
                senseRed.setAlpha(0f);
            } else if (position == POSITION_ALERT) {
                senseWave.suspendPlayback(false);

                senseGreen.setAlpha(0f);
                senseYellow.setAlpha(0f);
                senseRed.setAlpha(1f);
            } else if (position == POSITION_WAVE) {
                senseBackground.setAlpha(0f);
                senseGreen.setAlpha(0f);

                senseYellow.setScaleX(1f);
                senseYellow.setScaleY(1f);
                senseYellow.setAlpha(0f);

                senseRed.setScaleX(1f);
                senseRed.setScaleY(1f);
                senseRed.setAlpha(0f);

                senseWave.setAlpha(1f);
                senseWave.startPlayback();

                nextButton.setY(nextButtonMinY);
            }
        }
    }


    class ColorsAdapter extends ViewPagerAdapter<ColorsAdapter.ViewHolder> {
        private final LayoutInflater inflater;
        private final SenseColor[] senseColors;

        ColorsAdapter(@NonNull SenseColor[] senseColors) {
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
            final View view = inflater.inflate(R.layout.item_onboarding_sense_color, container, false);
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

            ViewHolder(@NonNull View itemView) {
                super(itemView);

                this.headingText = (TextView) itemView.findViewById(R.id.item_onboarding_sense_color_heading);
                this.subheadingText = (TextView) itemView.findViewById(R.id.item_onboarding_sense_color_subheading);
            }

            void bindSenseColor(@NonNull SenseColor senseColor) {
                headingText.setText(senseColor.headingRes);
                subheadingText.setText(senseColor.subheadingRes);
            }
        }
    }

    static class SenseColor {
        final @StringRes int headingRes;
        final @StringRes int subheadingRes;

        SenseColor(@StringRes int headingRes,
                   @StringRes int subheadingRes) {
            this.headingRes = headingRes;
            this.subheadingRes = subheadingRes;
        }
    }
}
