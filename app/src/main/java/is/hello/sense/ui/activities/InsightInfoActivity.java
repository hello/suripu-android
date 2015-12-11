package is.hello.sense.ui.activities;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.transition.TransitionInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.ui.common.ScopedInjectionActivity;
import is.hello.sense.ui.dialogs.InsightInfoDialogFragment;
import is.hello.sense.ui.widget.ExtendedScrollView;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.markup.text.MarkupString;
import is.hello.sense.util.markup.text.MarkupStyleSpan;

public class InsightInfoActivity extends ScopedInjectionActivity
        implements ExtendedScrollView.OnScrollListener {
    public static final String TAG = InsightInfoDialogFragment.class.getSimpleName();
    private static final String ARG_TITLE = InsightInfoDialogFragment.class.getName() + ".ARG_TITLE";
    private static final String ARG_MESSAGE = InsightInfoDialogFragment.class.getName() + ".ARG_MESSAGE";
    private static final String ARG_TRANSITION_NAME = InsightInfoDialogFragment.class.getName() + ".ARG_TRANSITION_NAME";
    private static final String ARG_INFO = InsightInfoDialogFragment.class.getName() + ".ARG_INFO";
    private static final String ARG_IMAGE_URL = InsightInfoDialogFragment.class.getName() + ".ARG_IMAGE_URL";

    @Inject Picasso picasso;

    private String title;
    private CharSequence message;
    private CharSequence info;

    private ExtendedScrollView scrollView;
    private ImageView topShadow, bottomShadow;
    private ImageView illustrationImage;

    //region Lifecycle

    public static void startActivity(@NonNull Activity sourceActivity,
                                     @NonNull String title,
                                     @NonNull MarkupString message,
                                     @Nullable MarkupString info,
                                     @Nullable String imageUrl,
                                     @Nullable View sharedElement,
                                     @Nullable String transitionName) {

        final ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(sourceActivity,
                                                                   sharedElement,
                                                                   transitionName);

        final Intent intent = new Intent(sourceActivity, InsightInfoActivity.class);
        intent.putExtra(ARG_TITLE, title);
        intent.putExtra(ARG_TRANSITION_NAME, transitionName);
        intent.putExtra(ARG_IMAGE_URL, imageUrl);
        intent.putExtra(ARG_INFO, (Parcelable) info);
        intent.putExtra(ARG_MESSAGE, (Parcelable) message);
        sourceActivity.startActivity(intent, options.toBundle());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_image_transition));
            getWindow().setExitTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_image_transition));


        }
        setContentView(R.layout.fragment_dialog_insight_info);
        this.illustrationImage =
                (ImageView) findViewById(R.id.fragment_dialog_insight_info_illustration);
        Intent intent = getIntent();
        final Bundle arguments = intent.getExtras();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            final String transitionName = arguments.getString(ARG_TRANSITION_NAME);
            if (transitionName != null) {
                illustrationImage.setTransitionName(transitionName);
            }
        }
        this.title = arguments.getString(ARG_TITLE);

        final MarkupString message = arguments.getParcelable(ARG_MESSAGE);
        this.message = addEmphasisFormatting(message);

        final MarkupString info = arguments.getParcelable(ARG_INFO);
        this.info = addEmphasisFormatting(info);


        Views.runWhenLaidOut(illustrationImage, () -> {
            final int width = illustrationImage.getMeasuredWidth();
            illustrationImage.getLayoutParams().height = Math.round(width * 0.5f /* 2:1 */);
            illustrationImage.requestLayout();
        });

        final TextView titleText =
                (TextView) findViewById(R.id.fragment_dialog_insight_info_title);
        titleText.setText(title);

        final TextView summaryText =
                (TextView) findViewById(R.id.fragment_dialog_insight_info_summary);
        final TextView messageText =
                (TextView) findViewById(R.id.fragment_dialog_insight_info_message);
        if (TextUtils.isEmpty(info)) {
            summaryText.setVisibility(View.GONE);
            messageText.setText(message);
        } else {
            summaryText.setVisibility(View.VISIBLE);
            summaryText.setText(message);
            messageText.setText(info);
        }

        final Button doneButton =
                (Button) findViewById(R.id.fragment_dialog_insight_info_done);
        Views.setSafeOnClickListener(doneButton, ignored -> {
            finish();
        });

        this.topShadow = (ImageView) findViewById(R.id.fragment_dialog_insight_info_top_shadow);
        this.bottomShadow = (ImageView) findViewById(R.id.fragment_dialog_insight_info_bottom_shadow);

        this.scrollView = (ExtendedScrollView) findViewById(R.id.fragment_dialog_insight_info_scroll);
        scrollView.setOnScrollListener(this);

        final String imageUrl = intent.getStringExtra(ARG_IMAGE_URL);
        if (!TextUtils.isEmpty(imageUrl)) {
            picasso.load(imageUrl)
                   .noFade()
                   .into(illustrationImage, new Callback() {
                       @Override
                       public void onSuccess() {
                           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                               startPostponedEnterTransition();
                           }
                       }

                       @Override
                       public void onError() {
                           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                               startPostponedEnterTransition();
                           }
                       }
                   });
        }
    }


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