package is.hello.sense.ui.handholding;

import android.animation.FloatEvaluator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.ViewPagerAdapter;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.handholding.util.WelcomeDialogParser;
import is.hello.sense.ui.widget.PageDots;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

public class WelcomeDialogFragment extends SenseDialogFragment {
    public static final String TAG = WelcomeDialogFragment.class.getSimpleName();

    private static final String ARG_WELCOME_RES = WelcomeDialogFragment.class.getName() + ".ARG_WELCOME_RES";

    private List<Item> items;
    private ItemAdapter adapter;
    private ViewPager viewPager;
    private PageDots pageDots;


    //region Lifecycle

    private static String getPreferenceKey(@NonNull Context context, @XmlRes int welcomeRes) {
        String resourceName = context.getResources().getResourceEntryName(welcomeRes);
        return "dialog_" + resourceName + "_shown";
    }

    public static void clearShownStates(@NonNull Context context) {
        Logger.info(TAG, "Clearing already shown dialog states");

        SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        preferences.edit()
                   .clear()
                   .apply();
    }

    public static boolean shouldShow(@NonNull Context context, @XmlRes int welcomeRes) {
        if (welcomeRes == WelcomeDialogParser.MISSING_RES) {
            return false;
        }

        SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        return !preferences.getBoolean(getPreferenceKey(context, welcomeRes), false);
    }

    public static void markShown(@NonNull Context context, @XmlRes int welcomeRes) {
        String key = getPreferenceKey(context, welcomeRes);
        SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        preferences.edit()
                   .putBoolean(key, true)
                   .apply();
    }

    public static boolean isAnyVisible(@NonNull Activity activity) {
        return (activity.getFragmentManager().findFragmentByTag(WelcomeDialogFragment.TAG) != null);
    }

    public static void show(@NonNull Activity activity, @XmlRes int welcomeRes) {
        WelcomeDialogFragment welcomeDialog = WelcomeDialogFragment.newInstance(welcomeRes);
        welcomeDialog.show(activity.getFragmentManager(), WelcomeDialogFragment.TAG);

        markShown(activity, welcomeRes);
    }

    public static void showIfNeeded(@NonNull Activity activity, @XmlRes int welcomeRes) {
        if (shouldShow(activity, welcomeRes)) {
            show(activity, welcomeRes);
        }
    }

    public static WelcomeDialogFragment newInstance(@XmlRes int welcomeRes) {
        WelcomeDialogFragment fragment = new WelcomeDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(ARG_WELCOME_RES, welcomeRes);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            int welcomeRes = getArguments().getInt(ARG_WELCOME_RES);
            WelcomeDialogParser parser = new WelcomeDialogParser(getResources(), welcomeRes);
            this.items = parser.parse();
        } catch (XmlPullParserException | IOException e) {
            this.items = Collections.emptyList();
        }

        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_FullScreen);
        dialog.setContentView(R.layout.fragment_dialog_welcome);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int tintedStatusBar = getResources().getColor(R.color.status_bar_dimmed);
            dialog.getWindow().setStatusBarColor(tintedStatusBar);
        }

        this.viewPager = (ViewPager) dialog.findViewById(R.id.fragment_dialog_welcome_view_pager);

        this.pageDots = (PageDots) dialog.findViewById(R.id.fragment_dialog_welcome_page_dots);
        if (items.size() < 2) {
            pageDots.setVisibility(View.GONE);

            MarginLayoutParams layoutParams = (MarginLayoutParams) viewPager.getLayoutParams();
            layoutParams.bottomMargin = layoutParams.topMargin;
        }

        this.adapter = new ItemAdapter();

        int pageMargin = getResources().getDimensionPixelSize(R.dimen.gap_medium);
        viewPager.setClipToPadding(false);
        viewPager.setPadding(pageMargin, 0, pageMargin, 0);
        viewPager.setPageMargin(pageMargin);

        int maxWidth = getResources().getDimensionPixelSize(R.dimen.dialog_max_width);
        int maxHeight = getResources().getDimensionPixelSize(R.dimen.dialog_max_height);
        Views.observeNextLayout(dialog.getWindow().getDecorView())
                .subscribe(v -> {
                    boolean isFloating = false;

                    int height = viewPager.getMeasuredHeight();
                    if (height > maxHeight) {
                        viewPager.getLayoutParams().height = maxHeight;
                        viewPager.invalidate();

                        isFloating = true;
                    }

                    int width = viewPager.getMeasuredWidth() - (pageMargin * 2);
                    if (width > maxWidth) {
                        int newPageMargin = (viewPager.getMeasuredWidth() - maxWidth) / 2;
                        viewPager.setPadding(newPageMargin, 0, newPageMargin, 0);
                        viewPager.setPageMargin(newPageMargin);

                        isFloating = true;
                    }

                    if (!isFloating) {
                        viewPager.setPageTransformer(true, new ParallaxTransformer(viewPager));
                    }

                    viewPager.setAdapter(adapter);
                    if (items.size() > 1) {
                        pageDots.attach(viewPager);
                    }
                });

        return dialog;
    }

    //endregion


    public static class ParallaxTransformer implements ViewPager.PageTransformer {
        private static final float SMALL_SCALE = 0.9f;
        private static final float BIG_SCALE = 1f;

        private static final float SMALL_ALPHA = 0.7f;
        private static final float BIG_ALPHA = 1f;

        private final FloatEvaluator evaluator = new FloatEvaluator();
        private final ViewPager parent;

        public ParallaxTransformer(@NonNull ViewPager parent) {
            this.parent = parent;
        }

        @Override
        public void transformPage(View page, float position) {
            // https://code.google.com/p/android/issues/detail?id=64046
            float fixedUpPosition = (position - parent.getPaddingRight() / (float) page.getWidth());
            float lockedPosition = Math.max(0f, fixedUpPosition);

            float scale = evaluator.evaluate(lockedPosition, BIG_SCALE, SMALL_SCALE);
            float alpha = evaluator.evaluate(lockedPosition, BIG_ALPHA, SMALL_ALPHA);
            float translationX = evaluator.evaluate(lockedPosition, 0f, -(parent.getWidth() / 5f));

            page.setScaleX(scale);
            page.setScaleY(scale);
            page.setTranslationX(translationX);
            page.setAlpha(alpha);
        }
    }


    //region Items

    public class ItemAdapter extends ViewPagerAdapter<ItemAdapter.ViewHolder> {
        private final LayoutInflater inflater = LayoutInflater.from(getActivity());

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public ViewHolder createViewHolder(ViewGroup container, int position) {
            View itemView = inflater.inflate(R.layout.item_dialog_welcome, container, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void bindViewHolder(ViewHolder holder, int position) {
            Item item = items.get(position);
            holder.bindItem(item);
            holder.setLastItem(position == getCount() - 1);
        }


        class ViewHolder extends ViewPagerAdapter.ViewHolder {
            final ImageView diagramImage;
            final TextView titleText;
            final TextView messageText;
            final Button dismissButton;
            final View dismissButtonDivider;

            private ViewHolder(@NonNull View itemView) {
                super(itemView);

                this.diagramImage = (ImageView) itemView.findViewById(R.id.fragment_dialog_welcome_item_diagram);
                this.titleText = (TextView) itemView.findViewById(R.id.fragment_dialog_welcome_item_title);
                this.messageText = (TextView) itemView.findViewById(R.id.fragment_dialog_welcome_item_message);

                this.dismissButtonDivider = itemView.findViewById(R.id.fragment_dialog_welcome_item_dismiss_border);
                this.dismissButton = (Button) itemView.findViewById(R.id.fragment_dialog_welcome_item_dismiss);
                Views.setSafeOnClickListener(dismissButton, ignored -> dismissAllowingStateLoss());

                Resources resources = getResources();
                float bottomCornerRadius = resources.getDimension(R.dimen.button_corner_radius);
                int normal = resources.getColor(R.color.background_light);
                int pressed = resources.getColor(R.color.light_accent_extra_dimmed);
                dismissButton.setBackground(Styles.newRoundedBorderlessButtonBackground(0f, bottomCornerRadius, normal, pressed));
            }

            private void setLastItem(boolean lastItem) {
                if (lastItem) {
                    dismissButtonDivider.setVisibility(View.VISIBLE);
                    dismissButton.setVisibility(View.VISIBLE);
                } else {
                    dismissButtonDivider.setVisibility(View.GONE);
                    dismissButton.setVisibility(View.GONE);
                }
            }

            private void bindItem(@NonNull Item item) {
                if (item.diagramRes != WelcomeDialogParser.MISSING_RES) {
                    diagramImage.setImageResource(item.diagramRes);
                } else {
                    diagramImage.setImageDrawable(null);
                }

                if (item.scaleDiagram) {
                    diagramImage.setAdjustViewBounds(true);
                    diagramImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else {
                    diagramImage.setAdjustViewBounds(false);
                    diagramImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }

                if (item.titleRes != WelcomeDialogParser.MISSING_RES) {
                    titleText.setText(item.titleRes);
                    titleText.setVisibility(View.VISIBLE);
                } else {
                    titleText.setText("");
                    titleText.setVisibility(View.GONE);
                }

                String message = getString(item.messageRes);
                diagramImage.setContentDescription(message);
                messageText.setText(message);
            }
        }
    }


    public static class Item {
        public final @DrawableRes int diagramRes;
        public final @StringRes int titleRes;
        public final @StringRes int messageRes;
        public final boolean scaleDiagram;

        public Item(@DrawableRes int diagramRes,
                    @StringRes int titleRes,
                    @StringRes int messageRes,
                    boolean scaleDiagram) {
            this.diagramRes = diagramRes;
            this.titleRes = titleRes;
            this.messageRes = messageRes;
            this.scaleDiagram = scaleDiagram;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "diagramRes=" + diagramRes +
                    ", titleRes=" + titleRes +
                    ", messageRes=" + messageRes +
                    ", scaleDiagram=" + scaleDiagram +
                    '}';
        }
    }

    //endregion
}
