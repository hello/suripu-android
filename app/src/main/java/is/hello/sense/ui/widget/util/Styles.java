package is.hello.sense.ui.widget.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import is.hello.commonsense.bluetooth.model.SenseConnectToWiFiUpdate;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.api.model.v2.Trends;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Constants;
import is.hello.sense.util.SuperscriptSpanAdjuster;
import is.hello.sense.util.markup.text.MarkupString;
import is.hello.sense.util.markup.text.MarkupStyleSpan;

import static is.hello.sense.ui.common.UserSupport.DeviceIssue;
import static is.hello.sense.ui.common.UserSupport.HelpStep;
import static is.hello.sense.ui.common.UserSupport.showAppSettings;
import static is.hello.sense.ui.common.UserSupport.showContactForm;
import static is.hello.sense.ui.common.UserSupport.showDeviceFeatures;
import static is.hello.sense.ui.common.UserSupport.showForDeviceIssue;
import static is.hello.sense.ui.common.UserSupport.showForHelpStep;
import static is.hello.sense.ui.common.UserSupport.showSupportedDevices;
import static is.hello.sense.ui.common.UserSupport.showUserGuide;

public final class Styles {

    public static final float DISABLED_ALPHA_FLOAT = 0.2f;
    public static final float ENABLED_ALPHA_FLOAT = 1.0f;

    public static final boolean UNDERLINE_LINKS = false;

    public static final int UNIT_STYLE_SUPERSCRIPT = (1 << 1);
    public static final int UNIT_STYLE_SUBSCRIPT = (1 << 2);

    private static final int ENABLED_ALPHA_INT = 255;
    private static final int DISABLED_ALPHA_INT = 50;

    @IntDef({
            UNIT_STYLE_SUPERSCRIPT,
            UNIT_STYLE_SUBSCRIPT,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface UnitStyle {
    }


    public static
    @StyleRes
    @DrawableRes
    int getScoreConditionTintThemeRes(@NonNull ScoreCondition condition) {
        switch (condition) {
            default:
            case UNAVAILABLE:
                return R.style.TintOverride_SleepScore;

            case ALERT:
                return R.style.TintOverride_SleepScore_Alert;

            case WARNING:
                return R.style.TintOverride_SleepScore_Warning;

            case IDEAL:
                return R.style.TintOverride_SleepScore_Ideal;
        }
    }


    public static
    @StringRes
    int getConnectStatusMessage(@NonNull ConnectProgress status) {
        switch (status) {
            case CONNECTING:
                return R.string.title_connecting_with_sense;

            case BONDING:
                return R.string.title_pairing;

            case DISCOVERING_SERVICES:
                return R.string.title_discovering_services;

            default:
            case CONNECTED:
                return R.string.title_connecting_with_sense;
        }
    }

    public static
    @StringRes
    int getWiFiConnectStatusMessage(@NonNull SenseConnectToWiFiUpdate status) {
        switch (status.state) {
            case WLAN_CONNECTED:
                return R.string.title_connecting_network_wlan_connected;
            case IP_RETRIEVED:
                return R.string.title_connecting_network_ip_retrieved;
            case DNS_RESOLVED:
                return R.string.title_connecting_network_dns_resolved;
            case SOCKET_CONNECTED:
                return R.string.title_connecting_network_socket_connected;
            case REQUEST_SENT:
                return R.string.title_connecting_network_request_sent;
            case DNS_FAILED:
            case CONNECT_FAILED:
                return R.string.title_connecting_network_connect_failed;
            case NO_WLAN_CONNECTED:
            case WLAN_CONNECTING:
            default:
                return R.string.title_connecting_network;
        }
    }


    public static
    @NonNull
    CharSequence createUnitSuperscriptSpan(@NonNull final String suffix) {
        final SpannableString spannableSuffix = new SpannableString(suffix);
        if (UnitFormatter.UNIT_SUFFIX_TEMPERATURE.equals(suffix)) {
            spannableSuffix.setSpan(new RelativeSizeSpan(0.6f),
                                    0, spannableSuffix.length(),
                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            spannableSuffix.setSpan(new SuperscriptSpanAdjuster(0.45f),
                                    0, spannableSuffix.length(),
                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        } else {
            spannableSuffix.setSpan(new RelativeSizeSpan(0.4f),
                                    0, spannableSuffix.length(),
                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            spannableSuffix.setSpan(new SuperscriptSpanAdjuster(0.95f),
                                    0, spannableSuffix.length(),
                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
        spannableSuffix.setSpan(new TypefaceSpan("sans-serif-light"),
                                0, spannableSuffix.length(),
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableSuffix;
    }

    public static
    @NonNull
    CharSequence createUnitSubscriptSpan(@NonNull final String suffix) {
        final SpannableString spannableSuffix = new SpannableString(suffix);
        spannableSuffix.setSpan(new RelativeSizeSpan(0.6f),
                                0, spannableSuffix.length(),
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableSuffix.setSpan(new SuperscriptSpanAdjuster(-0.05f),
                                0, spannableSuffix.length(),
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableSuffix.setSpan(new TypefaceSpan("sans-serif-light"),
                                0, spannableSuffix.length(),
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableSuffix;
    }

    public static
    @NonNull
    CharSequence assembleReadingAndUnit(@NonNull final CharSequence value,
                                        @NonNull final String suffix,
                                        @UnitStyle final int unitStyle) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(value);

        switch (unitStyle) {
            case UNIT_STYLE_SUPERSCRIPT:
                builder.append(createUnitSuperscriptSpan(suffix));
                break;

            case UNIT_STYLE_SUBSCRIPT:
                builder.append(createUnitSubscriptSpan(suffix));
                break;
        }

        return builder;
    }

    @NonNull
    public static CharSequence assembleReadingAndUnitWithSpace(@NonNull final CharSequence value,
                                                               @NonNull final String suffix,
                                                               @UnitStyle final int unitStyle) {
        return assembleReadingAndUnit(value, ' ' + suffix, unitStyle);
    }

    public static
    @NonNull
    CharSequence assembleReadingAndUnit(final double value, @NonNull final String suffix) {
        return assembleReadingAndUnit(String.format("%.0f", value), suffix, UNIT_STYLE_SUPERSCRIPT);
    }


    public static void applyRefreshLayoutStyle(@NonNull SwipeRefreshLayout refreshLayout) {
        refreshLayout.setColorSchemeResources(R.color.sensor_alert,
                                              R.color.sensor_warning,
                                              R.color.sensor_ideal);
    }

    public static void applyGraphLineParameters(@NonNull Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public static Spanned darkenEmphasis(@NonNull Resources resources,
                                         @Nullable MarkupString source) {
        if (source == null) {
            return null;
        }

        final @ColorInt int emphasisColor = resources.getColor(R.color.black);
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

    public static SpannableStringBuilder resolveSupportLinks(@NonNull Activity activity,
                                                             @NonNull CharSequence source) {
        final SpannableStringBuilder contents = new SpannableStringBuilder(source);
        final URLSpan[] urlSpans = contents.getSpans(0, contents.length(), URLSpan.class);
        for (final URLSpan urlSpan : urlSpans) {
            final String url = urlSpan.getURL();

            final SimpleClickableSpan clickableSpan;
            switch (url) {
                case "#user-guide": {
                    clickableSpan = new SimpleClickableSpan(v -> showUserGuide(activity));
                    break;
                }

                case "#contact": {
                    clickableSpan = new SimpleClickableSpan(v -> showContactForm(activity));
                    break;
                }

                case "#second-pill": {
                    clickableSpan = new SimpleClickableSpan(v -> showForDeviceIssue(activity, DeviceIssue.PAIRING_2ND_PILL));
                    break;
                }

                case "#sense-with-voice-info": {
                    clickableSpan = new SimpleClickableSpan(v -> showDeviceFeatures(activity));
                    break;
                }

                case "#supported-devices": {
                    clickableSpan = new SimpleClickableSpan(v -> showSupportedDevices(activity));
                    break;
                }

                case "#settings": {
                    clickableSpan = new SimpleClickableSpan(v -> showAppSettings(activity));
                    break;
                }

                case "#facebook-autofill": {
                    clickableSpan = new SimpleClickableSpan(v -> showForHelpStep(activity, HelpStep.AUTO_FILL_FACEBOOK));
                    break;
                }

                default: {
                    throw new IllegalArgumentException("Unknown deep link url " + url);
                }
            }

            final int spanStart = contents.getSpanStart(urlSpan);
            final int spanEnd = contents.getSpanEnd(urlSpan);
            contents.setSpan(clickableSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            contents.removeSpan(urlSpan);
        }
        return contents;
    }

    public static void initializeSupportFooter(@NonNull Activity activity, @NonNull TextView footer) {
        footer.setText(resolveSupportLinks(activity, footer.getText()));
        Views.makeTextViewLinksClickable(footer);
    }


    //region Dividers

    public static View createHorizontalDivider(@NonNull Context context, int width) {
        final View view = new View(context);
        view.setBackgroundResource(R.color.border);
        view.setLayoutParams(new ViewGroup.LayoutParams(width, context.getResources().getDimensionPixelSize(R.dimen.divider_size)));
        return view;
    }

    public static View createVerticalDivider(@NonNull Context context, int height) {
        final View view = new View(context);
        view.setBackgroundResource(R.color.border);
        view.setLayoutParams(new ViewGroup.LayoutParams(context.getResources().getDimensionPixelSize(R.dimen.divider_size), height));
        return view;
    }

    //endregion


    //region Selectables

    /**
     * Creates a new drawable for use as a borderless button background.
     * <p>
     * Intended for use in places where a {@link is.hello.sense.ui.widget.RoundedLinearLayout}
     * or {@link is.hello.sense.ui.widget.RoundedRelativeLayout} cannot be used due to
     * performance or rendering compatibility issues.
     */
    public static Drawable newRoundedBorderlessButtonBackground(float topRadius,
                                                                float bottomRadius,
                                                                @ColorInt int normalColor,
                                                                @ColorInt int selectedColor) {
        final float[] cornerRadii = {
                topRadius, topRadius, topRadius, topRadius,
                bottomRadius, bottomRadius, bottomRadius, bottomRadius,
        };
        final RoundRectShape roundedRect = new RoundRectShape(cornerRadii, null, null);
        final ShapeDrawable normal = new ShapeDrawable(roundedRect);
        normal.getPaint().setColor(normalColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new RippleDrawable(ColorStateList.valueOf(selectedColor), normal, normal);
        } else {
            final ShapeDrawable pressed = new ShapeDrawable(roundedRect);
            pressed.getPaint().setColor(selectedColor);

            final StateListDrawable selector = new StateListDrawable();
            selector.addState(new int[]{android.R.attr.state_pressed}, pressed);
            selector.addState(new int[]{}, normal);
            return selector;
        }
    }

    //endregion


    private static final class SimpleClickableSpan extends ClickableSpan {
        private final View.OnClickListener onClickListener;

        private SimpleClickableSpan(@NonNull View.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
        }

        @Override
        public void onClick(View widget) {
            onClickListener.onClick(widget);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            ds.setColor(ds.linkColor);
            ds.setUnderlineText(UNDERLINE_LINKS);
        }
    }

    public static float getBarWidthPercent(Trends.TimeScale timeScale) {
        if (timeScale == Trends.TimeScale.LAST_WEEK) {
            return .1276f;
        } else if (timeScale == Trends.TimeScale.LAST_MONTH) {
            return .0257f;
        } else {
            return .0111f;
        }
    }

    public static float getBarSpacePercent(Trends.TimeScale timeScale) {
        if (timeScale == Trends.TimeScale.LAST_WEEK) {
            return .0176f;
        } else if (timeScale == Trends.TimeScale.LAST_MONTH) {
            return .007f;
        } else {
            return 0;
        }
    }

    public static String createTextValue(float value, int numberOfDecimalPlaces) {
        return String.format("%." + numberOfDecimalPlaces + "f", value);
    }

    public static String stripTrailingZeros(@NonNull String string) {
        while (string.length() > 0 && string.charAt(string.length() - 1) == '0') {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    public static String stripTrailingPeriods(@NonNull String string) {
        while (string.length() > 0 && string.charAt(string.length() - 1) == '.') {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }


    public static void setTextAppearance(@NonNull final TextView textView,
                                         @StyleRes final int styleRes) {
        if (styleRes == Constants.NONE) {
            return;
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            textView.setTextAppearance(styleRes);
        } else {
            textView.setTextAppearance(textView.getContext(), styleRes);
        }
    }

    public static ColorStateList getColorStateList(@NonNull Resources resources, @ColorRes int id, @Nullable Resources.Theme theme) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            return resources.getColorStateList(id, theme);
        } else {
            return resources.getColorStateList(id);
        }
    }

    public static int dpToPx(final int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(final int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * @return int value between 0 - 255 based on enabled state.
     * Used when setting {@link android.widget.ImageView#setImageAlpha(int)}
     */
    public static int getImageViewAlpha(final boolean isEnabled) {
        return isEnabled ? Styles.ENABLED_ALPHA_INT : Styles.DISABLED_ALPHA_INT;
    }

    /**
     * @return float value between 0f - 1.0f based on enabled state.
     * Used when setting {@link View#setAlpha(float)}
     */
    public static float getViewAlpha(final boolean isEnabled) {
        return isEnabled ? Styles.ENABLED_ALPHA_FLOAT : Styles.DISABLED_ALPHA_FLOAT;
    }

    public static Bitmap drawableToBitmap(@NonNull final Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            final BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static void tintMenuIcon(@NonNull final Context context,
                                    @NonNull final MenuItem item,
                                    @ColorRes final int color) {
        final Drawable normalDrawable = item.getIcon();
        final Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(context, color));
        item.setIcon(wrapDrawable);
    }

    public static Spanned fromHtml(@NonNull final String string) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(string);
        }
    }
}
