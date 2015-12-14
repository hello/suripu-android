package is.hello.sense.ui.dialogs;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.R;
import is.hello.sense.ui.common.AnimatedInjectionFragment;
import is.hello.sense.ui.widget.ExtendedScrollView;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.ui.widget.util.Windows;
import is.hello.sense.util.markup.text.MarkupString;

public class InsightInfoFragment extends AnimatedInjectionFragment
        implements ExtendedScrollView.OnScrollListener {
    public static final String TAG = InsightInfoFragment.class.getSimpleName();

    private static final String ARG_TITLE = InsightInfoFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_MESSAGE = InsightInfoFragment.class.getName() + ".ARG_MESSAGE";
    private static final String ARG_IMAGE_URL = InsightInfoFragment.class.getName() + ".ARG_IMAGE_URL";
    private static final String ARG_INFO = InsightInfoFragment.class.getName() + ".ARG_INFO";

    @Inject Picasso picasso;

    private View rootView;
    private String title;
    private String imageUrl;
    private CharSequence message;
    private CharSequence info;

    private ExtendedScrollView scrollView;
    private ViewGroup contentView;
    private ImageView topShadow, bottomShadow;
    private ImageView illustrationImage;
    private Button doneButton;

    private @ColorInt int defaultStatusBarColor;

    //region Lifecycle

    public static InsightInfoFragment newInstance(@NonNull String title,
                                                  @NonNull MarkupString message,
                                                  @Nullable String imageUrl,
                                                  @Nullable MarkupString info) {
        final InsightInfoFragment fragment = new InsightInfoFragment();

        final Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putParcelable(ARG_MESSAGE, message);
        arguments.putString(ARG_IMAGE_URL, imageUrl);
        arguments.putParcelable(ARG_INFO, info);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle arguments = getArguments();
        this.title = arguments.getString(ARG_TITLE);
        this.imageUrl = arguments.getString(ARG_IMAGE_URL);

        final MarkupString message = arguments.getParcelable(ARG_MESSAGE);
        this.message = Styles.darkenEmphasis(getResources(), message);

        final MarkupString info = arguments.getParcelable(ARG_INFO);
        this.info = Styles.darkenEmphasis(getResources(), info);

        if (savedInstanceState != null) {
            this.defaultStatusBarColor = savedInstanceState.getInt("defaultStatusBarColor");
        } else {
            this.defaultStatusBarColor = Windows.getStatusBarColor(getActivity().getWindow());
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.fragment_insight_info, container, false);

        this.illustrationImage =
                (ImageView) rootView.findViewById(R.id.fragment_insight_info_illustration);

        this.contentView = (ViewGroup) rootView.findViewById(R.id.fragment_insight_info_content);
        contentView.setVisibility(View.INVISIBLE);

        final TextView titleText =
                (TextView) contentView.findViewById(R.id.fragment_insight_info_title);
        titleText.setText(title);

        final TextView summaryTitleText =
                (TextView) contentView.findViewById(R.id.fragment_insight_info_summary_header);
        final TextView summaryText =
                (TextView) contentView.findViewById(R.id.fragment_insight_info_summary);
        final TextView messageText =
                (TextView) contentView.findViewById(R.id.fragment_insight_info_message);
        if (TextUtils.isEmpty(info)) {
            summaryTitleText.setVisibility(View.GONE);
            summaryText.setVisibility(View.GONE);
            messageText.setText(message);
        } else {
            summaryTitleText.setVisibility(View.VISIBLE);
            summaryText.setVisibility(View.VISIBLE);
            summaryText.setText(message);
            messageText.setText(info);
        }

        this.doneButton = (Button) rootView.findViewById(R.id.fragment_insight_info_done);
        Views.setSafeOnClickListener(doneButton, stateSafeExecutor, ignored -> {
            getFragmentManager().popBackStack();
        });

        this.topShadow = (ImageView) rootView.findViewById(R.id.fragment_insight_info_top_shadow);
        this.bottomShadow = (ImageView) rootView.findViewById(R.id.fragment_insight_info_bottom_shadow);

        this.scrollView = (ExtendedScrollView) rootView.findViewById(R.id.fragment_insight_info_scroll);
        scrollView.setOnScrollListener(this);

        final Drawable existingImage = getSource().getInsightImage();
        if (existingImage != null) {
            illustrationImage.setImageDrawable(existingImage);
        } else if (!TextUtils.isEmpty(imageUrl)) {
            Views.runWhenLaidOut(illustrationImage, () -> {
                final int width = illustrationImage.getMeasuredWidth();
                illustrationImage.getLayoutParams().height = Math.round(width * 0.5f /* 2:1 */);
                illustrationImage.requestLayout();
            });

            picasso.load(imageUrl)
                   .into(illustrationImage);
        }

        return rootView;
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
        if (getSource().isComplexTransitionAvailable() && illustrationImage.getDrawable() != null) {
            scene.play(createIllustrationEnter())
                 .with(createBackgroundEnter())
                 .with(createStatusBarEnter())
                 .before(createDoneEnter());
        } else {
            final ObjectAnimator fadeIn =
                    ObjectAnimator.ofFloat(rootView, "alpha",
                                           0f, 1f);
            scene.play(fadeIn);
        }
        return AnimatorTemplate.DEFAULT.apply(scene);
    }

    @Override
    protected void onSkipEnterAnimator() {
        if (illustrationImage.getDrawable() != null) {
            final Window window = getActivity().getWindow();
            Windows.setStatusBarColor(window, getTargetStatusBarColor());
        }
    }

    @Override
    protected Animator onProvideExitAnimator() {
        final AnimatorSet scene = new AnimatorSet();
        if (getSource().isComplexTransitionAvailable()) {
            scene.play(createIllustrationExit())
                 .with(createStatusBarExit())
                 .with(createBackgroundExit())
                 .with(createDoneExit());
        } else {
            final ObjectAnimator fadeOut =
                    ObjectAnimator.ofFloat(rootView, "alpha",
                                           1f, 0f);
            scene.play(fadeOut)
                 .with(createStatusBarExit());
        }
        return AnimatorTemplate.DEFAULT.apply(scene);
    }

    //endregion


    //region Enter Animations

    private Animator createBackgroundEnter() {
        final @ColorInt int color = getResources().getColor(R.color.background_dark_overlay);
        final ColorDrawable background = new ColorDrawable(color);
        background.setAlpha(0);
        rootView.setBackground(background);
        return ObjectAnimator.ofInt(background, "alpha", 0, Color.alpha(color));
    }

    private Animator createStatusBarEnter() {
        final Window window = getActivity().getWindow();
        final @ColorInt int start = defaultStatusBarColor;
        final @ColorInt int end = getTargetStatusBarColor();
        return Windows.createStatusBarColorAnimator(window, start, end);
    }

    private Animator createIllustrationEnter() {
        final Rect initialRect = new Rect();

        final Rect finalRect = Views.copyFrame(illustrationImage);
        getSource().getInsightImageFrame(initialRect);
        illustrationImage.layout(initialRect.left, initialRect.top,
                                 initialRect.right, initialRect.bottom);

        return Views.createFrameAnimator(illustrationImage,
                                         initialRect,
                                         finalRect);
    }

    private Animator createDoneEnter() {
        doneButton.setTranslationY(doneButton.getHeight());

        return ObjectAnimator.ofFloat(doneButton, "translationY",
                                      doneButton.getHeight(), 0f);
    }

    //endregion

    //region Exit Animations

    private Animator createDoneExit() {
        return ObjectAnimator.ofFloat(doneButton, "translationY",
                                      0f, doneButton.getHeight());
    }

    private Animator createIllustrationExit() {
        final Rect initialRect = Views.copyFrame(illustrationImage);

        final Rect finalRect = new Rect();
        getSource().getInsightImageFrame(finalRect);

        return Views.createFrameAnimator(illustrationImage,
                                         initialRect,
                                         finalRect);
    }

    private Animator createStatusBarExit() {
        final Window window = getActivity().getWindow();
        final @ColorInt int start = Windows.getStatusBarColor(window);
        final @ColorInt int end = defaultStatusBarColor;
        return Windows.createStatusBarColorAnimator(window, start, end);
    }

    private Animator createBackgroundExit() {
        final @ColorInt int color = getResources().getColor(R.color.background_dark_overlay);
        final ColorDrawable background = new ColorDrawable(color);
        rootView.setBackground(background);
        return ObjectAnimator.ofInt(background, "alpha", Color.alpha(color), 0);
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

    private Source getSource() {
        return (Source) getTargetFragment();
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

    public interface Source {
        boolean isComplexTransitionAvailable();
        void getInsightCardFrame(@NonNull Rect outRect);
        void getInsightImageFrame(@NonNull Rect outRect);
        @Nullable Drawable getInsightImage();
    }
}