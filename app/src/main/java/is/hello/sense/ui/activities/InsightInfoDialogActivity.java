package is.hello.sense.ui.activities;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import javax.inject.Inject;

import is.hello.go99.Anime;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.R;
import is.hello.sense.ui.common.ScopedInjectionActivity;
import is.hello.sense.ui.dialogs.InsightInfoDialogFragment;
import is.hello.sense.ui.widget.ExtendedScrollView;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.ui.widget.util.Windows;
import is.hello.sense.util.markup.text.MarkupString;
import is.hello.sense.util.markup.text.MarkupStyleSpan;

public class InsightInfoDialogActivity extends ScopedInjectionActivity
        implements Target, ExtendedScrollView.OnScrollListener {
    public static final String TAG = InsightInfoDialogFragment.class.getSimpleName();

    private static final String ARG_TITLE = InsightInfoDialogFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_MESSAGE = InsightInfoDialogFragment.class.getName() + ".ARG_MESSAGE";
    private static final String ARG_IMAGE_URL = InsightInfoDialogFragment.class.getName() + ".ARG_IMAGE_URL";
    private static final String ARG_INFO = InsightInfoDialogFragment.class.getName() + ".ARG_INFO";

    @Inject
    Picasso picasso;

    private String title;
    private String imageUrl;
    private CharSequence message;
    private CharSequence info;

    private ExtendedScrollView scrollView;
    private ImageView topShadow, bottomShadow;
    private ImageView illustrationImage;
    private ValueAnimator loadedAnimator;

    //region Lifecycle

    public static Intent newInstance(@NonNull String title,
                                                        @NonNull MarkupString message,
                                                        @Nullable String imageUrl,
                                                        @Nullable MarkupString info) {
        final Intent intent = new Intent();

        intent.putExtra(ARG_TITLE, title);
        intent.(ARG_MESSAGE, message);
        intent.putExtra(ARG_IMAGE_URL, imageUrl);
        intent.putExtra(ARG_INFO, info);

        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.title = intent.getStringExtra(ARG_TITLE);
        this.imageUrl = intent.getStringExtra(ARG_IMAGE_URL);

        final MarkupString message = intent.getParcelableExtra(ARG_MESSAGE);
        this.message = addEmphasisFormatting(message);

        final MarkupString info = intent.getParcelableExtra(ARG_INFO);
        this.info = addEmphasisFormatting(info);
    }

    /*@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bitmap imageBitmap = null;
        final Dialog dialog = new Dialog(this, R.style.AppTheme_Dialog_FullScreen_Insight);
        dialog.setContentView(R.layout.fragment_dialog_insight_info);

        this.illustrationImage =
                (ImageView) dialog.findViewById(R.id.fragment_dialog_insight_info_illustration);
        if (savedInstanceState != null) {
            imageBitmap = savedInstanceState.getParcelable("IMAGE");
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.illustrationImage.setTransitionName("transitionnimage");
        }
        Views.runWhenLaidOut(illustrationImage, () -> {
            final int width = illustrationImage.getMeasuredWidth();
            illustrationImage.getLayoutParams().height = Math.round(width * 0.5f /* 2:1 */ /*);
            illustrationImage.requestLayout();
        });

        final TextView titleText =
                (TextView) dialog.findViewById(R.id.fragment_dialog_insight_info_title);
        titleText.setText(title);

        final TextView summaryText =
                (TextView) dialog.findViewById(R.id.fragment_dialog_insight_info_summary);
        final TextView messageText =
                (TextView) dialog.findViewById(R.id.fragment_dialog_insight_info_message);
        if (TextUtils.isEmpty(info)) {
            summaryText.setVisibility(View.GONE);
            messageText.setText(message);
        } else {
            summaryText.setVisibility(View.VISIBLE);
            summaryText.setText(message);
            messageText.setText(info);
        }

        final Button doneButton =
                (Button) dialog.findViewById(R.id.fragment_dialog_insight_info_done);
        Views.setSafeOnClickListener(doneButton, ignored -> {
            finish();
        });

        this.topShadow = (ImageView) dialog.findViewById(R.id.fragment_dialog_insight_info_top_shadow);
        this.bottomShadow = (ImageView) dialog.findViewById(R.id.fragment_dialog_insight_info_bottom_shadow);

        this.scrollView = (ExtendedScrollView) dialog.findViewById(R.id.fragment_dialog_insight_info_scroll);
        scrollView.setOnScrollListener(this);

        if (!TextUtils.isEmpty(imageUrl)) {
            picasso.load(imageUrl)
                   .into(this);
        }
        this.illustrationImage.setImageBitmap(imageBitmap);

        return dialog;
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (loadedAnimator != null) {
            loadedAnimator.cancel();
        }

        picasso.cancelRequest(this);
    }

    */

    //endregion


    //region Utilities

    private Spanned addEmphasisFormatting(@Nullable MarkupString source) {
        if (source == null) {
            return null;
        }

        final @ColorInt int emphasisColor = getResources().getColor(R.color.text_dark);
        final SpannableStringBuilder toFormat = new SpannableStringBuilder(source);
        final MarkupStyleSpan[] spans = toFormat.getSpans(0, toFormat.length(),
                                                          MarkupStyleSpan.class);
        for (final MarkupStyleSpan span : spans) {
            if (span.getStyle() == Typeface.NORMAL) {
                continue;
            }

            final int start = toFormat.getSpanStart(span);
            final int end = toFormat.getSpanEnd(span);
            final int flags = toFormat.getSpanFlags(span);

            toFormat.setSpan(new ForegroundColorSpan(emphasisColor), start, end, flags);

            if (span.getStyle() == Typeface.BOLD) {
                toFormat.removeSpan(span);
            }
        }

        return toFormat;
    }

    private static @ColorInt int getStatusBarColor(@NonNull Bitmap bitmap) {
        return Drawing.darkenColorBy(bitmap.getPixel(0, 0), 0.2f);
    }

    //endregion


    //region Bitmap Loading


    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        /*
        if (!isAdded()) {
            return;
        }

        if (getDialog() == null) {
            stateSafeExecutor.execute(() -> {
                final Window window = getDialog().getWindow();
                final @ColorInt int newStatusBar = getStatusBarColor(bitmap);
                Windows.setStatusBarColor(window, newStatusBar);
                illustrationImage.setImageBitmap(bitmap);
            });

            return;
        }

        final Window window = getDialog().getWindow();
        final BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);
        drawable.setAlpha(0);
        illustrationImage.setImageDrawable(drawable);

        final @ColorInt int oldStatusBar = Windows.getStatusBarColor(window);
        final @ColorInt int newStatusBar = getStatusBarColor(bitmap);
        this.loadedAnimator = AnimatorTemplate.DEFAULT.apply(ValueAnimator.ofFloat(0f, 1f));
        loadedAnimator.addUpdateListener(a -> {
            final float fraction = a.getAnimatedFraction();
            drawable.setAlpha(Math.round(255f * fraction));
            Windows.setStatusBarColor(window, Anime.interpolateColors(fraction,
                                                                      oldStatusBar,
                                                                      newStatusBar));
        });
        loadedAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (loadedAnimator == animation) {
                    InsightInfoDialogActivity.this.loadedAnimator = null;
                }
            }
        });
        loadedAnimator.start();
        */
    }


    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
    /*
        if (!isAdded()) {
            return;
        }

        final Window window = getDialog().getWindow();
        final @ColorInt int statusBar = getResources().getColor(R.color.status_bar_illustration);
        Windows.setStatusBarColor(window, statusBar);
        illustrationImage.setImageDrawable(errorDrawable);
        */
    }


    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
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
}