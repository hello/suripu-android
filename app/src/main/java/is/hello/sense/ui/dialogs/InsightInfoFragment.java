package is.hello.sense.ui.dialogs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.api.model.v2.InsightInfo;
import is.hello.sense.api.model.v2.InsightType;
import is.hello.sense.api.model.v2.ShareUrl;
import is.hello.sense.graph.presenters.InsightInfoPresenter;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.common.AnimatedInjectionFragment;
import is.hello.sense.ui.widget.ExtendedScrollView;
import is.hello.sense.ui.widget.ParallaxImageView;
import is.hello.sense.ui.widget.SplitButtonLayout;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.ui.widget.util.Windows;
import is.hello.sense.util.SafeOnClickListener;
import is.hello.sense.util.Share;
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
    private static final String ARG_SUMMARY = InsightInfoFragment.class.getName() + ".ARG_SUMMARY";
    private static final String ARG_IMAGE_URL = InsightInfoFragment.class.getName() + ".ARG_IMAGE_URL";
    private static final String ARG_ID = InsightInfoFragment.class.getName() + ".ARG_ID";

    @Inject
    Picasso picasso;
    @Inject
    InsightInfoPresenter presenter;
    @Inject
    ApiService apiService;

    private String imageUrl;
    private CharSequence summary;
    private String insightId = null;
    private String category = null;

    @UsedInTransition
    private View rootView;
    @UsedInTransition
    private View fillView;

    @UsedInTransition
    private ExtendedScrollView scrollView;
    @UsedInTransition
    private ImageView topShadow, bottomShadow;
    @UsedInTransition
    private ParallaxImageView illustrationImage;
    @UsedInTransition
    private View[] contentViews;
    private TextView titleText;
    private TextView messageText;
    @UsedInTransition
    private SplitButtonLayout bottomContainer;

    private
    @ColorInt
    int defaultStatusBarColor;

    /**
     * The one-off status bar animator used for Picasso image loads.
     */
    private
    @Nullable
    WeakReference<Animator> statusBarAnimator;


    //region Lifecycle

    public static InsightInfoFragment newInstance(@NonNull final Insight insight,
                                                  @NonNull final Resources resources) {
        final InsightInfoFragment fragment = new InsightInfoFragment();

        final Bundle arguments = new Bundle();
        arguments.putString(ARG_CATEGORY, insight.getCategory());
        if (insight.shouldDisplaySummary()) {
            arguments.putParcelable(ARG_SUMMARY, insight.getMessage());
        }
        arguments.putString(ARG_ID, insight.getId());
        arguments.putString(ARG_IMAGE_URL, insight.getImageUrl(resources));
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle arguments = getArguments();
        final String category = arguments.getString(ARG_CATEGORY);
        if (category != null) {
            presenter.setCategory(category);
        }

        this.imageUrl = arguments.getString(ARG_IMAGE_URL);
        this.insightId = arguments.getString(ARG_ID);

        final MarkupString message = arguments.getParcelable(ARG_SUMMARY);
        this.summary = Styles.darkenEmphasis(getResources(), message);

        if (savedInstanceState != null) {
            this.defaultStatusBarColor = savedInstanceState.getInt("defaultStatusBarColor");
        } else {
            this.defaultStatusBarColor = Windows.getStatusBarColor(getActivity().getWindow());
        }

        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.fragment_insight_info, container, false);
        this.fillView = rootView.findViewById(R.id.fragment_insight_info_fill);

        this.illustrationImage =
                (ParallaxImageView) rootView.findViewById(R.id.fragment_insight_info_illustration);
        illustrationImage.setPicassoListener(this);

        this.titleText = (TextView) rootView.findViewById(R.id.fragment_insight_info_title);
        this.bottomContainer = (SplitButtonLayout) rootView.findViewById(R.id.fragment_insight_info_bottom);

        this.messageText = (TextView) rootView.findViewById(R.id.fragment_insight_info_message);
        final TextView summaryHeaderText = (TextView) rootView.findViewById(R.id.fragment_insight_info_summary_header);
        final TextView summaryText = (TextView) rootView.findViewById(R.id.fragment_insight_info_summary);

        if (TextUtils.isEmpty(summary)) {
            this.contentViews = new View[]{titleText, messageText};
            summaryHeaderText.setVisibility(View.GONE);
            summaryText.setVisibility(View.GONE);
        } else {
            summaryText.setText(summary);
            this.contentViews = new View[]{titleText, messageText, summaryHeaderText, summaryText};
        }

        if (insightId == null) {
            //hide share button
            bottomContainer.hideRightButton();
        } else if (getActivity() != null && getActivity() instanceof HomeActivity) {
            bottomContainer.setRightButtonOnClickListener((v) -> {
                ((HomeActivity) getActivity()).showProgressOverlay(true);
                apiService.shareInsight(new InsightType(insightId))
                          .doOnTerminate(() -> shareInsightTerminate((HomeActivity) getActivity()))
                          .subscribe(this::shareInsightSuccess,
                                     this::shareInsightError);
            });
        }

        bottomContainer.setLeftButtonOnClickListener(
                new SafeOnClickListener(stateSafeExecutor, ignored -> getFragmentManager().popBackStack()));

        this.topShadow = (ImageView) this.rootView.findViewById(R.id.fragment_insight_info_top_shadow);
        this.bottomShadow = (ImageView) this.rootView.findViewById(R.id.fragment_insight_info_bottom_shadow);

        this.scrollView = (ExtendedScrollView) this.rootView.findViewById(R.id.fragment_insight_info_scroll);

        final Parent parent = getParent();
        final Drawable existingImage = parent != null
                ? parent.getInsightImage()
                : null;
        if (existingImage != null)

        {
            illustrationImage.setDrawable(existingImage, false);
        } else if (!TextUtils.isEmpty(imageUrl))

        {
            final int maxWidth = getResources().getDisplayMetrics().widthPixels;
            final int maxHeight = Math.round(maxWidth * illustrationImage.getAspectRatioScale());
            picasso.load(imageUrl)
                   .resize(maxWidth, maxHeight)
                   .placeholder(R.drawable.empty_illustration)
                   .into(illustrationImage);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.insightInfo,
                         this::bindInsightInfo,
                         this::insightInfoUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        picasso.cancelRequest(illustrationImage);

        this.titleText = null;
        this.messageText = null;
        if (bottomContainer != null) {
            this.bottomContainer.setLeftButtonOnClickListener(null);
            this.bottomContainer.setRightButtonOnClickListener(null);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("defaultStatusBarColor", defaultStatusBarColor);
    }

//endregion


//region Animation

    @Override
    protected Animator onProvideEnterAnimator() {
        final AnimatorSet scene = new AnimatorSet();
        final Parent parent = getParent();
        final SharedState state = parent != null
                ? parent.provideSharedState(true)
                : null;
        if (state != null) {
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
    protected void onExitAnimatorEnd() {
        this.rootView = null;
        this.fillView = null;
        this.scrollView = null;
        this.topShadow = null;
        this.bottomShadow = null;
        this.illustrationImage = null;
        this.contentViews = null;
        if (bottomContainer != null) {
            this.bottomContainer.setLeftButtonOnClickListener(null);
            this.bottomContainer.setRightButtonOnClickListener(null);
            this.bottomContainer = null;
        }
    }

    @Override
    protected Animator onProvideExitAnimator() {
        final Animator statusBarAnimator = extract(this.statusBarAnimator);
        if (statusBarAnimator != null) {
            statusBarAnimator.cancel();
        }

        final AnimatorSet scene = new AnimatorSet();
        final Parent parent = getParent();
        final SharedState state = parent != null
                ? parent.provideSharedState(false)
                : null;
        if (state != null) {
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
            public void onAnimationStart(final Animator animation) {
                for (final View contentView : contentViews) {
                    contentView.setVisibility(View.VISIBLE);
                    contentView.setAlpha(0f);
                }
            }
        });

        final Animator[] animators = new Animator[contentViews.length];
        for (int i = 0, length = contentViews.length; i < length; i++) {
            final View contentView = contentViews[i];
            contentView.setAlpha(0f);
            animators[i] = animatorFor(contentView).alpha(1f);
        }
        subscene.playTogether(animators);
        return subscene;
    }

    private Animator createFillEnter(@NonNull final Rect insightCardRect) {
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

    private Animator createParallaxEnter(final float targetPercent) {
        illustrationImage.setParallaxPercent(targetPercent);
        return illustrationImage.createParallaxPercentAnimator(0f);
    }

    private Animator createIllustrationEnter(@NonNull final Rect imageRect) {
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
        bottomContainer.setTranslationY(bottomContainer.getHeight());

        return animatorFor(bottomContainer)
                .withStartDelay(MULTI_PHASE_DELAY)
                .translationY(0f);
    }

//endregion

//region Exit Animations

    private Animator createDoneExit() {
        return animatorFor(bottomContainer)
                .translationY(bottomContainer.getHeight());
    }

    private Animator createContentExit() {
        final AnimatorSet subscene = new AnimatorSet();

        final Animator[] animators = new Animator[contentViews.length];
        for (int i = 0, length = contentViews.length; i < length; i++) {
            animators[i] = animatorFor(contentViews[i]).alpha(0f);
        }

        subscene.playTogether(animators);
        return subscene;
    }

    private Animator createParallaxExit(final float targetPercent) {
        illustrationImage.setParallaxPercent(0f);
        return illustrationImage.createParallaxPercentAnimator(targetPercent);
    }

    private Animator createIllustrationExit(@NonNull final Rect imageRect) {
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

    private Animator createFillExit(@NonNull final Rect insightCardRect) {
        final Rect fillRect = Views.copyFrame(fillView);
        return Views.createFrameAnimator(fillView, fillRect, insightCardRect);
    }

//endregion


//region Utilities

    private
    @ColorInt
    int getTargetStatusBarColor() {
        if (illustrationImage.getDrawable() instanceof BitmapDrawable) {
            final Bitmap bitmap = ((BitmapDrawable) illustrationImage.getDrawable()).getBitmap();
            if (bitmap != null) { // @Nullable annotation is missing; nullability in documentation.
                return Drawing.darkenColorBy(bitmap.getPixel(0, 0), 0.2f);
            }
        }

        return Windows.getStatusBarColor(getActivity().getWindow());
    }

    @Nullable
    private Parent getParent() {
        final Activity activity = getActivity();
        if (activity instanceof ParentProvider) {
            return ((ParentProvider) activity).provideInsightInfoParent();
        } else {
            return null;
        }
    }

//endregion


//region Data Bindings

    public void bindInsightInfo(@NonNull final InsightInfo info) {
        titleText.setText(info.getTitle());
        messageText.setText(info.getText());
        category = info.getCategory();
    }

    public void insightInfoUnavailable(final Throwable e) {
        final StringRef errorMessage = Errors.getDisplayMessage(e);
        if (errorMessage != null) {
            messageText.setText(errorMessage.resolve(getActivity()));
        } else {
            messageText.setText(R.string.dialog_error_generic_message);
        }
    }

    @Override
    public void onBitmapLoaded(@NonNull final Bitmap bitmap) {
        final Animator statusBarAnimator = createStatusBarEnter();
        statusBarAnimator.setStartDelay(0L);
        statusBarAnimator.start();

        this.statusBarAnimator = new WeakReference<>(statusBarAnimator);
    }

    @Override
    public void onBitmapFailed() {
    }

//endregion

    private void shareInsightSuccess(@NonNull final ShareUrl shareUrl) {
        final Share.Text text = Share.text(shareUrl.getUrlForSharing(getActivity()));
        if (category != null) {
            text.withProperties(Share.getInsightProperties(category));
        }
        text.send(getActivity());
    }

    private void shareInsightError(@NonNull final Throwable throwable) {
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(throwable, getActivity())
                .withTitle(R.string.error_share_insights_title)
                .withMessage(StringRef.from(R.string.error_share_insights_message))
                .build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    private void shareInsightTerminate(@Nullable final HomeActivity activity) {
        if (activity == null) {
            return;
        }
        activity.showProgressOverlay(false);
    }

//region Scroll handling

    @Override
    public void onScrollChanged(final int scrollX, final int scrollY,
                                final int oldScrollX, final int oldScrollY) {
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
        @Nullable
        SharedState provideSharedState(boolean isEnter);

        @Nullable
        Drawable getInsightImage();
    }

    public interface ParentProvider {
        @Nullable
        Parent provideInsightInfoParent();
    }

    public static class SharedState {
        public final Rect cardRectInWindow = new Rect();
        public final Rect imageRectInWindow = new Rect();
        public float imageParallaxPercent = 0f;

        public
        @NonNull
        Animator parentAnimator = new AnimatorSet();

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