package is.hello.sense.ui.dialogs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import is.hello.buruberi.util.Errors;
import is.hello.buruberi.util.StringRef;
import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.api.model.v2.InsightInfo;
import is.hello.sense.graph.presenters.InsightInfoPresenter;
import is.hello.sense.ui.common.AnimatedInjectionFragment;
import is.hello.sense.ui.widget.ExtendedScrollView;
import is.hello.sense.ui.widget.ParallaxImageView;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.ui.widget.util.Windows;
import is.hello.sense.util.markup.text.MarkupString;

import static is.hello.go99.animators.MultiAnimator.animatorFor;
import static is.hello.sense.functional.Functions.extract;

public class InsightInfoFragment extends AnimatedInjectionFragment
        implements ExtendedScrollView.OnScrollListener, ParallaxImageView.PicassoListener {
    public static final String TAG = InsightInfoFragment.class.getSimpleName();

    private static final long TRANSITION_DURATION = Anime.DURATION_NORMAL;
    /**
     * The status bar animation needs to fire when the image is near the status bar,
     * but not touching it. This creates the effect of the image moving away pulling
     * the color out of the status bar.
     */
    private static final long MULTI_PHASE_DELAY = TRANSITION_DURATION - 100L;

    private static final String ARG_CATEGORY = InsightInfoFragment.class.getName() + ".ARG_CATEGORY";
    private static final String ARG_TITLE = InsightInfoFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_SUMMARY = InsightInfoFragment.class.getName() + ".ARG_SUMMARY";
    private static final String ARG_IMAGE_URL = InsightInfoFragment.class.getName() + ".ARG_IMAGE_URL";

    @Inject Picasso picasso;
    @Inject InsightInfoPresenter presenter;

    private View rootView;
    private View fillView;
    private String title;
    private String imageUrl;
    private CharSequence summary;

    private ExtendedScrollView scrollView;
    private ImageView topShadow, bottomShadow;
    private ParallaxImageView illustrationImage;
    private View[] contentViews;
    private TextView messageText;
    private Button doneButton;

    private @ColorInt int defaultStatusBarColor;

    /**
     * The one-off status bar animator used for Picasso image loads.
     */
    private @Nullable WeakReference<Animator> statusBarAnimator;


    //region Lifecycle

    public static InsightInfoFragment newInstance(@NonNull Insight insight,
                                                  @NonNull Resources resources) {
        final InsightInfoFragment fragment = new InsightInfoFragment();

        final Bundle arguments = new Bundle();
        arguments.putString(ARG_CATEGORY, insight.getCategory());
        arguments.putString(ARG_TITLE, insight.getTitle());
        arguments.putParcelable(ARG_SUMMARY, insight.getMessage());
        arguments.putString(ARG_IMAGE_URL, insight.getImageUrl(resources));
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle arguments = getArguments();
        final String category = arguments.getString(ARG_CATEGORY);
        if (category != null) {
            presenter.setCategory(category);
        }

        this.title = arguments.getString(ARG_TITLE);
        this.imageUrl = arguments.getString(ARG_IMAGE_URL);

        final MarkupString message = arguments.getParcelable(ARG_SUMMARY);
        this.summary = Styles.darkenEmphasis(getResources(), message);

        if (savedInstanceState != null) {
            this.defaultStatusBarColor = savedInstanceState.getInt("defaultStatusBarColor");
        } else {
            this.defaultStatusBarColor = Windows.getStatusBarColor(getActivity().getWindow());
        }

        addPresenter(presenter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.fragment_insight_info, container, false);
        this.fillView = rootView.findViewById(R.id.fragment_insight_info_fill);

        this.illustrationImage =
                (ParallaxImageView) rootView.findViewById(R.id.fragment_insight_info_illustration);
        illustrationImage.setPicassoListener(this);

        final TextView titleText = (TextView) rootView.findViewById(R.id.fragment_insight_info_title);
        titleText.setText(title);

        this.messageText = (TextView) rootView.findViewById(R.id.fragment_insight_info_message);
        final TextView summaryHeaderText = (TextView) rootView.findViewById(R.id.fragment_insight_info_summary_header);
        final TextView summaryText = (TextView) rootView.findViewById(R.id.fragment_insight_info_summary);
        summaryText.setText(summary);

        this.contentViews = new View[] { titleText, messageText, summaryHeaderText, summaryText };

        this.doneButton = (Button) this.rootView.findViewById(R.id.fragment_insight_info_done);
        Views.setSafeOnClickListener(doneButton, stateSafeExecutor, ignored -> {
            getFragmentManager().popBackStack();
        });

        this.topShadow = (ImageView) this.rootView.findViewById(R.id.fragment_insight_info_top_shadow);
        this.bottomShadow = (ImageView) this.rootView.findViewById(R.id.fragment_insight_info_bottom_shadow);

        this.scrollView = (ExtendedScrollView) this.rootView.findViewById(R.id.fragment_insight_info_scroll);

        final Parent parent = getSource();
        final Drawable existingImage = parent != null
                ? parent.getInsightImage()
                : null;
        if (existingImage != null) {
            illustrationImage.setDrawable(existingImage, false);
        } else if (!TextUtils.isEmpty(imageUrl)) {
            picasso.load(imageUrl)
                   .placeholder(R.drawable.empty_illustration)
                   .into(illustrationImage);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.insightInfo,
                         this::bindInsightInfo,
                         this::insightInfoUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        picasso.cancelRequest(illustrationImage);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("defaultStatusBarColor", defaultStatusBarColor);
    }

    //endregion


    //region Animation

    @Override
    protected Animator onProvideEnterAnimator() {
        final AnimatorSet scene = new AnimatorSet();
        final Parent parent = getSource();
        final SharedState state = new SharedState();
        if (parent != null && parent.provideSharedState(/*out*/state, true)) {
            topShadow.setVisibility(View.GONE);
            bottomShadow.setVisibility(View.GONE);

            scene.play(createIllustrationEnter(state.imageRectInWindow))
                 .with(createFillEnter(state.cardRectInWindow))
                 .with(createParallaxEnter(state.imageParallaxPercent))
                 .with(state.parentAnimator)
                 .with(createStatusBarEnter())
                 .with(createDoneEnter())
                 .before(createContentEnter());
        } else {
            final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(rootView, "alpha", 0f, 1f);
            scene.play(fadeIn);
        }

        scene.setInterpolator(new DecelerateInterpolator());
        scene.setDuration(TRANSITION_DURATION);

        return scene;
    }

    @Override
    protected void onSkipEnterAnimator() {
        if (illustrationImage.getDrawable() != null) {
            final Window window = getActivity().getWindow();
            Windows.setStatusBarColor(window, getTargetStatusBarColor());
        }

        for (final View contentView : contentViews) {
            contentView.setVisibility(View.VISIBLE);
            contentView.setAlpha(1f);
        }

        scrollView.setOnScrollListener(this);
    }

    @Override
    protected void onEnterAnimatorEnd() {
        scrollView.setOnScrollListener(this);
    }

    @Override
    protected Animator onProvideExitAnimator() {
        final Animator statusBarAnimator = extract(this.statusBarAnimator);
        if (statusBarAnimator != null) {
            statusBarAnimator.cancel();
        }

        final AnimatorSet scene = new AnimatorSet();
        final Parent parent = getSource();
        final SharedState state = new SharedState();
        if (parent != null && parent.provideSharedState(/*out*/state, false)) {
            topShadow.setVisibility(View.GONE);
            bottomShadow.setVisibility(View.GONE);

            scrollView.setOnScrollListener(null);

            scene.play(createIllustrationExit(state.imageRectInWindow))
                 .with(createFillExit(state.cardRectInWindow))
                 .with(createParallaxExit(state.imageParallaxPercent))
                 .with(state.parentAnimator)
                 .after(MULTI_PHASE_DELAY)
                 .with(createStatusBarExit())
                 .with(createDoneExit())
                 .after(createContentExit());
        } else {
            final ObjectAnimator fadeOut = ObjectAnimator.ofFloat(rootView, "alpha", 1f, 0f);
            scene.play(fadeOut)
                 .with(createStatusBarExit());
        }

        scene.setInterpolator(new DecelerateInterpolator());
        scene.setDuration(TRANSITION_DURATION);

        return scene;
    }

    //endregion


    //region Enter Animations

    private Animator createContentEnter() {
        final AnimatorSet subscene = new AnimatorSet();
        subscene.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                for (final View contentView : contentViews) {
                    contentView.setVisibility(View.VISIBLE);
                    contentView.setAlpha(0f);
                }
            }
        });

        final int initialTranslationY = getResources().getDimensionPixelSize(R.dimen.gap_medium);
        final Animator[] animators = new Animator[contentViews.length];
        for (int i = 0, length = contentViews.length; i < length; i++) {
            final View contentView = contentViews[i];
            contentView.setAlpha(0f);
            contentView.setTranslationY(initialTranslationY);

            animators[i] = animatorFor(contentView)
                    .alpha(1f)
                    .translationY(0f);
        }
        subscene.playTogether(animators);
        return subscene;
    }

    private Animator createFillEnter(@NonNull Rect insightCardRect) {
        final Rect fillRect = Views.copyFrame(fillView);
        fillView.layout(insightCardRect.left, insightCardRect.top,
                        insightCardRect.right, insightCardRect.bottom);

        return Views.createFrameAnimator(fillView, insightCardRect, fillRect);
    }

    private Animator createStatusBarEnter() {
        final Window window = getActivity().getWindow();
        final @ColorInt int start = defaultStatusBarColor;
        final @ColorInt int end = getTargetStatusBarColor();
        final Animator animator = Windows.createStatusBarColorAnimator(window, start, end);
        animator.setStartDelay(MULTI_PHASE_DELAY);
        return animator;
    }

    private Animator createParallaxEnter(float targetPercent) {
        illustrationImage.setParallaxPercent(targetPercent);
        return illustrationImage.createParallaxPercentAnimator(0f);
    }

    private Animator createIllustrationEnter(@NonNull Rect imageRect) {
        illustrationImage.setPivotX(0f);
        illustrationImage.setPivotY(0f);

        final float scaleX = imageRect.width() / (float) illustrationImage.getMeasuredWidth();
        illustrationImage.setScaleX(scaleX);

        final float scaleY = imageRect.height() / (float) illustrationImage.getMeasuredHeight();
        illustrationImage.setScaleY(scaleY);

        final float translationX = imageRect.left;
        illustrationImage.setTranslationX(translationX);

        final float translationY = imageRect.top;
        illustrationImage.setTranslationY(translationY);

        return animatorFor(illustrationImage)
                .scale(1f)
                .translationX(0f)
                .translationY(0f);
    }

    private Animator createDoneEnter() {
        doneButton.setTranslationY(doneButton.getHeight());

        return animatorFor(doneButton)
                .withStartDelay(MULTI_PHASE_DELAY)
                .translationY(0f);
    }

    //endregion

    //region Exit Animations

    private Animator createDoneExit() {
        return animatorFor(doneButton)
                .translationY(doneButton.getHeight());
    }

    private Animator createContentExit() {
        final AnimatorSet subscene = new AnimatorSet();

        final int finalTranslationY = getResources().getDimensionPixelSize(R.dimen.gap_medium);
        final Animator[] animators = new Animator[contentViews.length];
        for (int i = 0, length = contentViews.length; i < length; i++) {
            animators[i] = animatorFor(contentViews[i])
                    .alpha(0f)
                    .translationY(finalTranslationY);
        }

        subscene.playTogether(animators);
        return subscene;
    }

    private Animator createParallaxExit(float targetPercent) {
        illustrationImage.setParallaxPercent(0f);
        return illustrationImage.createParallaxPercentAnimator(targetPercent);
    }

    private Animator createIllustrationExit(@NonNull Rect imageRect) {
        illustrationImage.setPivotX(0f);
        illustrationImage.setPivotY(0f);

        final float scaleX = imageRect.width() / (float) illustrationImage.getMeasuredWidth();
        final float scaleY = imageRect.height() / (float) illustrationImage.getMeasuredHeight();

        final float translationX = imageRect.left + scrollView.getScrollX();
        final float translationY = imageRect.top + scrollView.getScrollY();

        return animatorFor(illustrationImage)
                .scaleX(scaleX)
                .scaleY(scaleY)
                .translationX(translationX)
                .translationY(translationY);
    }

    private Animator createStatusBarExit() {
        final Window window = getActivity().getWindow();
        final @ColorInt int start = Windows.getStatusBarColor(window);
        final @ColorInt int end = defaultStatusBarColor;
        return Windows.createStatusBarColorAnimator(window, start, end);
    }

    private Animator createFillExit(@NonNull Rect insightCardRect) {
        final Rect fillRect = Views.copyFrame(fillView);
        return Views.createFrameAnimator(fillView, fillRect, insightCardRect);
    }

    //endregion


    //region Utilities

    private @ColorInt int getTargetStatusBarColor() {
        if (illustrationImage.getDrawable() instanceof BitmapDrawable) {
            final Bitmap bitmap = ((BitmapDrawable) illustrationImage.getDrawable()).getBitmap();
            return Drawing.darkenColorBy(bitmap.getPixel(0, 0), 0.2f);
        } else {
            return Windows.getStatusBarColor(getActivity().getWindow());
        }
    }

    @Nullable
    private Parent getSource() {
        // The target fragment does not correctly survive state changes because the
        // target fragment and this fragment do not share the same fragment manager.
        final Fragment targetFragment = getTargetFragment();
        if (targetFragment instanceof Parent) {
            return (Parent) targetFragment;
        } else {
            return null;
        }
    }

    //endregion


    //region Data Bindings

    public void bindInsightInfo(@NonNull InsightInfo info) {
        messageText.setText(info.getText());
    }

    public void insightInfoUnavailable(Throwable e) {
        final StringRef errorMessage = Errors.getDisplayMessage(e);
        if (errorMessage != null) {
            messageText.setText(errorMessage.resolve(getActivity()));
        } else {
            messageText.setText(R.string.dialog_error_generic_message);
        }
    }

    @Override
    public void onBitmapLoaded(@NonNull Bitmap bitmap) {
        final Animator statusBarAnimator = createStatusBarEnter();
        statusBarAnimator.setStartDelay(0L);
        statusBarAnimator.start();

        this.statusBarAnimator = new WeakReference<>(statusBarAnimator);
    }

    @Override
    public void onBitmapFailed() {
    }

    //endregion


    //region Scroll handling

    @Override
    public void onScrollChanged(int scrollX, int scrollY,
                                int oldScrollX, int oldScrollY) {
        if (scrollY >= illustrationImage.getMeasuredHeight()) {
            topShadow.setVisibility(View.VISIBLE);
        } else {
            topShadow.setVisibility(View.GONE);
        }

        if (scrollView.canScrollVertically(1)) {
            bottomShadow.setVisibility(View.VISIBLE);
        } else {
            bottomShadow.setVisibility(View.GONE);
        }
    }

    //endregion

    public interface Parent {
        boolean provideSharedState(@NonNull SharedState outState, boolean isEnter);
        @Nullable Drawable getInsightImage();
    }

    public static class SharedState {
        public final Rect cardRectInWindow = new Rect();
        public final Rect imageRectInWindow = new Rect();
        public float imageParallaxPercent = 0f;

        public @NonNull Animator parentAnimator = new AnimatorSet();

        @Override
        public String toString() {
            return "SharedState{" +
                    "cardRectInWindow=" + cardRectInWindow +
                    ", imageRectInWindow=" + imageRectInWindow +
                    ", imageParallaxPercent=" + imageParallaxPercent +
                    ", parentAnimator=" + parentAnimator +
                    '}';
        }
    }
}