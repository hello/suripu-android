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
import android.support.annotation.Nullable;
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
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import is.hello.sense.R;
import is.hello.sense.api.model.TimelineSegment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.graphing.ColorDrawableCompat;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.SuperscriptSpanAdjuster;

public final class Styles {
    public static final int TIMELINE_HOURS_ON_SCREEN = 10;
    public static final boolean UNDERLINE_LINKS = false;

    public static final int CARD_SPACING_HEADER = (1 << 1);
    public static final int CARD_SPACING_FOOTER = (1 << 2);
    public static final int CARD_SPACING_USE_COMPACT = (1 << 3);
    public static final int CARD_SPACING_HEADER_AND_FOOTER = CARD_SPACING_HEADER | CARD_SPACING_FOOTER;
    public static final int CARD_SPACING_OUT_COUNT = 2;

    @IntDef(
            value = {
                    CARD_SPACING_HEADER,
                    CARD_SPACING_FOOTER,
                    CARD_SPACING_USE_COMPACT,
                    CARD_SPACING_HEADER_AND_FOOTER
            },
            flag = true
    )
    @Retention(RetentionPolicy.SOURCE)
    public @interface CardSpacing {}


    public static @ColorRes @DrawableRes int getSleepDepthColorRes(int sleepDepth, boolean dimmed) {
        if (dimmed) {
            if (sleepDepth == 0) {
                return R.color.sleep_awake_dimmed;
            } else if (sleepDepth == 100) {
                return R.color.sleep_deep_dimmed;
            } else if (sleepDepth < 60) {
                return R.color.sleep_light_dimmed;
            } else {
                return R.color.sleep_intermediate_dimmed;
            }
        } else {
            if (sleepDepth == 0) {
                return R.color.sleep_awake;
            } else if (sleepDepth == 100) {
                return R.color.sleep_deep;
            } else if (sleepDepth < 60) {
                return R.color.sleep_light;
            } else {
                return R.color.sleep_intermediate;
            }
        }
    }

    public static @StringRes int getSleepDepthStringRes(int sleepDepth) {
        if (sleepDepth == 0) {
            return R.string.sleep_depth_awake;
        } else if (sleepDepth == 100) {
            return R.string.sleep_depth_deep;
        } else if (sleepDepth < 60) {
            return R.string.sleep_depth_light;
        } else {
            return R.string.sleep_depth_intermediate;
        }
    }

    public static @StringRes int getWakingDepthStringRes(int sleepDepth) {
        if (sleepDepth == 0) {
            return R.string.waking_depth_awake;
        } else if (sleepDepth == 100) {
            return R.string.waking_depth_deep;
        } else if (sleepDepth < 60) {
            return R.string.waking_depth_light;
        } else {
            return R.string.waking_depth_intermediate;
        }
    }

    public static @ColorRes @DrawableRes int getSleepScoreColorRes(int sleepScore) {
        if (sleepScore >= 80) {
            return R.color.sensor_ideal;
        } else if (sleepScore >= 50) {
            return R.color.sensor_warning;
        } else {
            return R.color.sensor_alert;
        }
    }

    public static int getSleepScoreColor(@NonNull Context context, int sleepScore) {
        return context.getResources().getColor(getSleepScoreColorRes(sleepScore));
    }


    public static @DrawableRes int getTimelineSegmentIconRes(@NonNull TimelineSegment segment) {
        if (!segment.hasEventInfo()) {
            return 0;
        }

        switch (segment.getEventType()) {
            case MOTION: {
                return R.drawable.timeline_movement;
            }

            case NOISE: {
                return R.drawable.timeline_sound;
            }

            case SNORING: {
                return R.drawable.timeline_sound;
            }

            case SLEEP_TALK: {
                return R.drawable.timeline_sound;
            }

            case LIGHT: {
                return R.drawable.timeline_light;
            }

            case LIGHTS_OUT: {
                return R.drawable.timeline_lights_out;
            }

            case SLEEP_MOTION: {
                return R.drawable.timeline_movement;
            }

            case IN_BED: {
                return R.drawable.timeline_in_bed;
            }

            case SLEEP: {
                return R.drawable.timeline_asleep;
            }

            case SUNSET: {
                return R.drawable.timeline_sunset;
            }

            case SUNRISE: {
                return R.drawable.timeline_sunrise;
            }

            case PARTNER_MOTION: {
                return R.drawable.timeline_partner;
            }

            case OUT_OF_BED: {
                return R.drawable.timeline_out_of_bed;
            }

            case WAKE_UP: {
                return R.drawable.timeline_wakeup;
            }

            case ALARM: {
                return R.drawable.timeline_alarm;
            }

            default:
            case UNKNOWN: {
                return R.drawable.timeline_unknown;
            }
        }
    }


    public static CharSequence createUnitSuffixSpan(@NonNull String suffix) {
        SpannableString spannableSuffix = new SpannableString(' ' + suffix);
        if (UnitSystem.TEMP_SUFFIX.equals(suffix)) {
            spannableSuffix.setSpan(new RelativeSizeSpan(0.6f), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            spannableSuffix.setSpan(new SuperscriptSpanAdjuster(0.45f), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        } else {
            spannableSuffix.setSpan(new RelativeSizeSpan(0.4f), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            spannableSuffix.setSpan(new SuperscriptSpanAdjuster(0.95f), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
        spannableSuffix.setSpan(new TypefaceSpan("sans-serif-light"), 0, spannableSuffix.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableSuffix;
    }

    public static CharSequence assembleReadingAndUnit(long value, @NonNull String suffix) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(Long.toString(value));
        builder.append(createUnitSuffixSpan(suffix));
        return builder;
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

    public static void addCardSpacing(@NonNull ListView listView,
                                      @CardSpacing int spacing,
                                      @Nullable View[] outSpacers) {
        if (outSpacers != null && outSpacers.length != CARD_SPACING_OUT_COUNT) {
            throw new IllegalArgumentException("outSpacers must have length of " + CARD_SPACING_OUT_COUNT);
        }

        Context context = listView.getContext();
        Resources resources = listView.getResources();

        boolean isCompact = ((spacing & CARD_SPACING_USE_COMPACT) == CARD_SPACING_USE_COMPACT);
        int spacingHeight;
        if (isCompact) {
            spacingHeight = resources.getDimensionPixelSize(R.dimen.gap_card_header_footer_compact);
        } else {
            spacingHeight = resources.getDimensionPixelSize(R.dimen.gap_card_header_footer);
        }
        if ((spacing & CARD_SPACING_HEADER) == CARD_SPACING_HEADER) {
            View topSpacing = new View(context);
            topSpacing.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, spacingHeight));
            ListViews.addHeaderView(listView, topSpacing, null, false);

            if (outSpacers != null) {
                outSpacers[0] = topSpacing;
            }
        }

        if ((spacing & CARD_SPACING_FOOTER) == CARD_SPACING_FOOTER) {
            View bottomSpacing = new View(context);
            bottomSpacing.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, spacingHeight));
            ListViews.addFooterView(listView, bottomSpacing, null, false);

            if (outSpacers != null) {
                outSpacers[1] = bottomSpacing;
            }
        }
    }

    public static SpannableStringBuilder resolveSupportLinks(@NonNull Activity activity,
                                                             @NonNull CharSequence source) {
        SpannableStringBuilder contents = new SpannableStringBuilder(source);
        URLSpan[] urlSpans = contents.getSpans(0, contents.length(), URLSpan.class);
        for (URLSpan urlSpan : urlSpans) {
            String url = urlSpan.getURL();

            SimpleClickableSpan clickableSpan;
            switch (url) {
                case "#support": {
                    clickableSpan = new SimpleClickableSpan(v -> UserSupport.showSupport(activity));
                    break;
                }

                case "#email": {
                    clickableSpan = new SimpleClickableSpan(v -> UserSupport.showEmailSupport(activity));
                    break;
                }

                case "#second-pill": {
                    clickableSpan = new SimpleClickableSpan(v -> UserSupport.showForDeviceIssue(activity, UserSupport.DeviceIssue.PAIRING_2ND_PILL));
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
