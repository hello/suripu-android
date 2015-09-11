package is.hello.sense.ui.widget.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import is.hello.buruberi.bluetooth.stacks.util.Operation;
import is.hello.commonsense.bluetooth.model.SenseConnectToWiFiUpdate;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.ScoreCondition;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.graphing.ColorDrawableCompat;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.SuperscriptSpanAdjuster;

public final class Styles {
    public static final float LETTER_SPACING_SECTION_HEADING_LARGE = 0.2f;

    public static final boolean UNDERLINE_LINKS = false;

    public static final int UNIT_STYLE_SUPERSCRIPT = (1 << 1);
    public static final int UNIT_STYLE_SUBSCRIPT = (1 << 2);
    @IntDef({
        UNIT_STYLE_SUPERSCRIPT,
        UNIT_STYLE_SUBSCRIPT,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface UnitStyle {}


    public static @ColorRes @DrawableRes int getSleepScoreColorRes(int sleepScore) {
        if (sleepScore >= 80) {
            return R.color.sensor_ideal;
        } else if (sleepScore >= 50) {
            return R.color.sensor_warning;
        } else {
            return R.color.sensor_alert;
        }
    }

    public static @StyleRes @DrawableRes int getScoreConditionTintThemeRes(@NonNull ScoreCondition condition) {
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


    public static @StringRes int getWiFiConnectStatusMessage(@NonNull Operation status) {
        switch (status) {
            case CONNECTING:
                return R.string.title_connecting;

            case BONDING:
                return R.string.title_pairing;

            case DISCOVERING_SERVICES:
                return R.string.title_discovering_services;

            default:
            case CONNECTED:
                return R.string.title_connecting;
        }
    }

    public static @StringRes int getWiFiConnectStatusMessage(@NonNull SenseConnectToWiFiUpdate status) {
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


    public static @NonNull CharSequence createUnitSuperscriptSpan(@NonNull String suffix) {
        SpannableString spannableSuffix = new SpannableString(' ' + suffix);
        if (UnitFormatter.UNIT_SUFFIX_TEMPERATURE.equals(suffix)) {
            spannableSuffix.setSpan(new RelativeSizeSpan(0.6f), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            spannableSuffix.setSpan(new SuperscriptSpanAdjuster(0.45f), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        } else {
            spannableSuffix.setSpan(new RelativeSizeSpan(0.4f), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            spannableSuffix.setSpan(new SuperscriptSpanAdjuster(0.95f), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
        spannableSuffix.setSpan(new TypefaceSpan("sans-serif-light"), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableSuffix;
    }

    public static @NonNull CharSequence createUnitSubscriptSpan(@NonNull String suffix) {
        SpannableString spannableSuffix = new SpannableString(' ' + suffix);
        spannableSuffix.setSpan(new RelativeSizeSpan(0.6f), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableSuffix.setSpan(new SuperscriptSpanAdjuster(-0.05f), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableSuffix.setSpan(new TypefaceSpan("sans-serif-light"), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableSuffix;
    }

    public static @NonNull CharSequence assembleReadingAndUnit(@NonNull CharSequence value,
                                                               @NonNull String suffix,
                                                               @UnitStyle int unitStyle) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
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

    public static @NonNull CharSequence assembleReadingAndUnit(double value, @NonNull String suffix) {
        return assembleReadingAndUnit(String.format("%.0f", value), suffix, UNIT_STYLE_SUPERSCRIPT);
    }

    public static @NonNull GradientDrawable createGraphFillGradientDrawable(@NonNull Resources resources) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
                resources.getColor(R.color.graph_fill_gradient_top),
                resources.getColor(R.color.graph_fill_gradient_bottom),
        });
    }

    public static @NonNull ColorDrawableCompat createGraphFillSolidDrawable(@NonNull Resources resources) {
        return new ColorDrawableCompat(resources.getColor(R.color.graph_fill_solid));
    }

    public static @NonNull TextView createItemView(@NonNull Context context,
                                                   @StringRes int titleRes,
                                                   @StyleRes int textAppearanceRes,
                                                   @NonNull View.OnClickListener onClick) {
        TextView itemView = new TextView(context);
        itemView.setBackgroundResource(R.drawable.selectable_dark_bounded);
        itemView.setTextAppearance(context, textAppearanceRes);
        itemView.setText(titleRes);

        Resources resources = context.getResources();
        int itemTextHorizontalPadding = resources.getDimensionPixelSize(R.dimen.gap_outer);
        int itemTextVerticalPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);
        itemView.setPadding(itemTextHorizontalPadding, itemTextVerticalPadding, itemTextHorizontalPadding, itemTextVerticalPadding);

        Views.setSafeOnClickListener(itemView, onClick);

        return itemView;
    }

    public static void applyRefreshLayoutStyle(@NonNull SwipeRefreshLayout refreshLayout) {
        refreshLayout.setColorSchemeResources(R.color.sensor_alert, R.color.sensor_warning, R.color.sensor_ideal);
    }

    public static void applyGraphLineParameters(@NonNull Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public static SpannableStringBuilder resolveSupportLinks(@NonNull Activity activity,
                                                             @NonNull CharSequence source) {
        SpannableStringBuilder contents = new SpannableStringBuilder(source);
        URLSpan[] urlSpans = contents.getSpans(0, contents.length(), URLSpan.class);
        for (URLSpan urlSpan : urlSpans) {
            String url = urlSpan.getURL();

            SimpleClickableSpan clickableSpan;
            switch (url) {
                case "#user-guide": {
                    clickableSpan = new SimpleClickableSpan(v -> UserSupport.showUserGuide(activity));
                    break;
                }

                case "#contact": {
                    clickableSpan = new SimpleClickableSpan(v -> UserSupport.showContactForm(activity));
                    break;
                }

                case "#second-pill": {
                    clickableSpan = new SimpleClickableSpan(v -> UserSupport.showForDeviceIssue(activity, UserSupport.DeviceIssue.PAIRING_2ND_PILL));
                    break;
                }

                case "#supported-devices": {
                    clickableSpan = new SimpleClickableSpan(v -> UserSupport.showSupportedDevices(activity));
                    break;
                }

                default: {
                    throw new IllegalArgumentException("Unknown deep link url " + url);
                }
            }

            int spanStart = contents.getSpanStart(urlSpan);
            int spanEnd = contents.getSpanEnd(urlSpan);
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
        View view = new View(context);
        view.setBackgroundResource(R.color.border);
        view.setLayoutParams(new ViewGroup.LayoutParams(width, context.getResources().getDimensionPixelSize(R.dimen.divider_size)));
        return view;
    }

    public static View createVerticalDivider(@NonNull Context context, int height) {
        View view = new View(context);
        view.setBackgroundResource(R.color.border);
        view.setLayoutParams(new ViewGroup.LayoutParams(context.getResources().getDimensionPixelSize(R.dimen.divider_size), height));
        return view;
    }

    //endregion


    //region Selectables

    /**
     * Creates a new drawable for use as a borderless button background.
     * <p />
     * Intended for use in places where a {@link is.hello.sense.ui.widget.RoundedLinearLayout}
     * or {@link is.hello.sense.ui.widget.RoundedRelativeLayout} cannot be used due to
     * performance or rendering compatibility issues.
     */
    public static Drawable newRoundedBorderlessButtonBackground(float topRadius,
                                                                float bottomRadius,
                                                                int normalColor,
                                                                int selectedColor) {
        float[] cornerRadii = {
            topRadius, topRadius, topRadius, topRadius,
            bottomRadius, bottomRadius, bottomRadius, bottomRadius,
        };
        RoundRectShape roundedRect = new RoundRectShape(cornerRadii, null, null);
        ShapeDrawable normal = new ShapeDrawable(roundedRect);
        normal.getPaint().setColor(normalColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new RippleDrawable(ColorStateList.valueOf(selectedColor), normal, normal);
        } else {
            ShapeDrawable pressed = new ShapeDrawable(roundedRect);
            pressed.getPaint().setColor(selectedColor);

            StateListDrawable selector = new StateListDrawable();
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
}
