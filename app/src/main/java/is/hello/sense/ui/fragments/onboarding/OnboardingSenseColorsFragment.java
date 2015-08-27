package is.hello.sense.ui.fragments.onboarding;

import android.content.res.Resources;
import android.net.Uri;
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
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.buruberi.util.Either;
import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.ViewPagerAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.PageDots;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class OnboardingSenseColorsFragment extends InjectionFragment
        implements ViewPager.OnPageChangeListener {
    @Inject RoomConditionsPresenter presenter;

    private ViewPager viewPager;
    private ViewGroup bottomContainer;
    private Button nextButton;
    private ColorsAdapter adapter;
    private FinalPageTransformer transformer;

    private int finalItem, transitionItem;
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

        final Uri finalVideo = Uri.parse(getString(R.string.diagram_onboarding_sense_colors));
        final SenseColor[] senseColors = {
                new SenseColor(R.string.title_sense_colors_1, R.string.info_sense_colors_1,
                               R.drawable.onboarding_sense_colors_1, null),
                new SenseColor(R.string.title_sense_colors_2, R.string.info_sense_colors_2,
                               R.drawable.onboarding_sense_colors_2, null),
                new SenseColor(R.string.title_sense_colors_3, R.string.info_sense_colors_3,
                               R.drawable.onboarding_sense_colors_3, null),
                new SenseColor(R.string.title_sense_colors_4, R.string.info_sense_colors_4,
                               R.drawable.onboarding_sense_colors_4, null),
                new SenseColor(R.string.title_sense_colors_5, R.string.info_sense_colors_5,
                               R.drawable.onboarding_sense_colors_5, finalVideo),
        };
        this.adapter = new ColorsAdapter(senseColors);
        viewPager.setAdapter(adapter);

        this.transformer = new FinalPageTransformer(getResources());
        viewPager.setPageTransformer(false, transformer);

        this.finalItem = adapter.getCount() - 1;
        this.transitionItem = finalItem - 1;

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

        adapter.destroyDiagramVideoViews();
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
        if (position == transitionItem) {
            float newY = Anime.interpolateFloats(positionOffset, nextButtonMaxY, nextButtonMinY);
            nextButton.setY(newY);
            transformer.setEnabled(true);
        } else {
            transformer.setEnabled(false);
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


    static class FinalPageTransformer implements ViewPager.PageTransformer {
        private static final float SCALE_MIN = 0.8f;
        private static final float SCALE_MAX = 1.0f;
        private final int translationXMin;

        private boolean enabled = false;

        FinalPageTransformer(@NonNull Resources resources) {
            this.translationXMin = -resources.getDimensionPixelSize(R.dimen.gap_large);
        }

        @Override
        public void transformPage(View page, float position) {
            if (enabled && position < 0f) {
                final float fraction = 1f + position;
                page.setAlpha(fraction);

                final float scale = Anime.interpolateFloats(fraction, SCALE_MIN, SCALE_MAX);
                page.setScaleX(scale);
                page.setScaleY(scale);

                final float translationX = Anime.interpolateFloats(fraction, translationXMin, 0f);
                page.setTranslationX(translationX);
            } else {
                page.setAlpha(1f);
                page.setScaleX(1f);
                page.setScaleY(1f);
                page.setTranslationX(0f);
            }
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    class ColorsAdapter extends ViewPagerAdapter<ColorsAdapter.ViewHolder> {
        private final LayoutInflater inflater;
        private final SenseColor[] senseColors;
        private final List<DiagramVideoView> diagramVideoViews = new ArrayList<>();
        private @Nullable ViewHolder primaryItem;

        ColorsAdapter(@NonNull SenseColor[] senseColors) {
            this.inflater = LayoutInflater.from(getActivity());
            this.senseColors = senseColors;
        }

        public void destroyDiagramVideoViews() {
            for (DiagramVideoView diagramVideoView : diagramVideoViews) {
                diagramVideoView.destroy();
            }
            diagramVideoViews.clear();

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
            final SenseColor senseColor = getItem(position);
            final LinearLayout view = (LinearLayout) inflater.inflate(R.layout.item_onboarding_sense_color, container, false);
            return new ViewHolder(view, (senseColor.diagramVideo != null));
        }

        @Override
        public void bindViewHolder(ViewHolder holder, int position) {
            SenseColor color = getItem(position);
            holder.bindSenseColor(color);
        }

        @Override
        public void unbindViewHolder(ViewHolder holder) {
            holder.unbind();
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            ViewHolder newPrimaryItem = (ViewHolder) object;
            if (primaryItem == newPrimaryItem) {
                return;
            }

            if (primaryItem != null) {
                final Either<ImageView, DiagramVideoView> diagram = primaryItem.diagram;
                if (!diagram.isLeft()) {
                    final DiagramVideoView diagramVideoView = diagram.getRight();
                    diagramVideoView.suspendPlayback();
                }
            }

            if (newPrimaryItem != null) {
                final Either<ImageView, DiagramVideoView> diagram = newPrimaryItem.diagram;
                if (!diagram.isLeft()) {
                    final DiagramVideoView diagramVideoView = diagram.getRight();
                    diagramVideoView.startPlayback();
                }
            }

            this.primaryItem = newPrimaryItem;
        }

        class ViewHolder extends ViewPagerAdapter.ViewHolder {
            private final TextView headingText;
            private final TextView subheadingText;
            private final Either<ImageView, DiagramVideoView> diagram;

            ViewHolder(@NonNull LinearLayout itemView, boolean hasVideo) {
                super(itemView);

                this.headingText = (TextView) itemView.findViewById(R.id.item_onboarding_sense_color_heading);
                this.subheadingText = (TextView) itemView.findViewById(R.id.item_onboarding_sense_color_subheading);

                if (hasVideo) {
                    final DiagramVideoView diagramVideoView = new DiagramVideoView(itemView.getContext());
                    diagramVideoView.setAutoStart(false);
                    itemView.addView(diagramVideoView, new LayoutParams(LayoutParams.MATCH_PARENT,
                                                                        LayoutParams.WRAP_CONTENT));

                    diagramVideoViews.add(diagramVideoView);
                    this.diagram = Either.right(diagramVideoView);
                } else {
                    final ImageView imageView = new ImageView(itemView.getContext());
                    imageView.setAdjustViewBounds(true);
                    itemView.addView(imageView, new LayoutParams(LayoutParams.MATCH_PARENT,
                                                                 LayoutParams.WRAP_CONTENT));

                    this.diagram = Either.left(imageView);
                }
            }

            void bindSenseColor(@NonNull SenseColor senseColor) {
                headingText.setText(senseColor.headingRes);
                subheadingText.setText(senseColor.subheadingRes);
                diagram.match(imageView -> {
                    imageView.setImageResource(senseColor.diagramRes);
                    imageView.setContentDescription(subheadingText.getText().toString());
                }, diagramVideoView -> {
                    diagramVideoView.setPlaceholder(senseColor.diagramRes);
                    diagramVideoView.setContentDescription(subheadingText.getText().toString());
                    if (senseColor.diagramVideo != null) {
                        diagramVideoView.setDataSource(senseColor.diagramVideo);
                    }
                });
            }

            void unbind() {
                if (!diagram.isLeft()) {
                    final DiagramVideoView diagramVideoView = diagram.getRight();
                    diagramVideoView.destroy();

                    diagramVideoViews.remove(diagramVideoView);
                }
            }
        }
    }

    static class SenseColor {
        final @StringRes int headingRes;
        final @StringRes int subheadingRes;
        final @DrawableRes int diagramRes;
        final @Nullable Uri diagramVideo;

        SenseColor(@StringRes int headingRes,
                   @StringRes int subheadingRes,
                   @DrawableRes int diagramRes,
                   @Nullable Uri diagramVideo) {
            this.headingRes = headingRes;
            this.subheadingRes = subheadingRes;
            this.diagramRes = diagramRes;
            this.diagramVideo = diagramVideo;
        }
    }
}
