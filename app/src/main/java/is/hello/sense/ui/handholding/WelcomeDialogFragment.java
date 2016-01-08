package is.hello.sense.ui.handholding;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.ui.adapter.ViewPagerAdapter;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.handholding.util.WelcomeDialogParser;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.PageDots;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.ui.widget.util.Windows;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

public class WelcomeDialogFragment extends SenseDialogFragment {
    public static final String TAG = WelcomeDialogFragment.class.getSimpleName();

    private static final String ARG_WELCOME_RES = WelcomeDialogFragment.class.getName() + ".ARG_WELCOME_RES";

    private @XmlRes int welcomeRes;
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

        final SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        preferences.edit()
                   .clear()
                   .apply();
    }

    public static boolean shouldShow(@NonNull Context context, @XmlRes int welcomeRes) {
        if (welcomeRes == WelcomeDialogParser.MISSING_RES) {
            return false;
        }

        final SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        return !preferences.getBoolean(getPreferenceKey(context, welcomeRes), false);
    }

    public static void markShown(@NonNull Context context, @XmlRes int welcomeRes) {
        final String key = getPreferenceKey(context, welcomeRes);
        final SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        preferences.edit()
                   .putBoolean(key, true)
                   .apply();
    }

    public static boolean isAnyVisible(@NonNull Activity activity) {
        return (activity.getFragmentManager().findFragmentByTag(WelcomeDialogFragment.TAG) != null);
    }

    public static void show(@NonNull Activity activity, @XmlRes int welcomeRes) {
        final WelcomeDialogFragment welcomeDialog = WelcomeDialogFragment.newInstance(welcomeRes);
        welcomeDialog.showAllowingStateLoss(activity.getFragmentManager(), WelcomeDialogFragment.TAG);
    }

    public static boolean showIfNeeded(@NonNull Activity activity, @XmlRes int welcomeRes) {
        if (shouldShow(activity, welcomeRes)) {
            show(activity, welcomeRes);
            return true;
        } else {
            return false;
        }
    }

    public static WelcomeDialogFragment newInstance(@XmlRes int welcomeRes) {
        final WelcomeDialogFragment fragment = new WelcomeDialogFragment();

        final Bundle arguments = new Bundle();
        arguments.putInt(ARG_WELCOME_RES, welcomeRes);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            this.welcomeRes = getArguments().getInt(ARG_WELCOME_RES);

            final WelcomeDialogParser parser = new WelcomeDialogParser(getResources(), welcomeRes);
            this.items = parser.parse();
        } catch (XmlPullParserException | IOException e) {
            this.items = Collections.emptyList();
        }

        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_FullScreen);
        dialog.setContentView(R.layout.fragment_dialog_welcome);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final @ColorInt int activityStatusBarColor =
                    Windows.getStatusBarColor(getActivity().getWindow());
            final @ColorInt int myStatusBarColor =
                    Drawing.darkenColorBy(activityStatusBarColor, 0.5f);
            Windows.setStatusBarColor(dialog.getWindow(), myStatusBarColor);
        }

        this.viewPager = (ViewPager) dialog.findViewById(R.id.fragment_dialog_welcome_view_pager);

        this.pageDots = (PageDots) dialog.findViewById(R.id.fragment_dialog_welcome_page_dots);
        if (items.size() < 2) {
            pageDots.setVisibility(View.GONE);

            final MarginLayoutParams layoutParams = (MarginLayoutParams) viewPager.getLayoutParams();
            layoutParams.bottomMargin = layoutParams.topMargin;
        }

        this.adapter = new ItemAdapter();

        final int pageMargin = getResources().getDimensionPixelSize(R.dimen.gap_medium);
        viewPager.setClipToPadding(false);
        viewPager.setPadding(pageMargin, 0, pageMargin, 0);
        viewPager.setPageMargin(pageMargin);

        final int maxWidth = getResources().getDimensionPixelSize(R.dimen.dialog_max_width);
        final int maxHeight = getResources().getDimensionPixelSize(R.dimen.dialog_max_height);
        Views.runWhenLaidOut(dialog.getWindow().getDecorView(), () -> {
            boolean isFloating = false;

            final int height = viewPager.getMeasuredHeight();
            if (height > maxHeight) {
                viewPager.getLayoutParams().height = maxHeight;
                viewPager.invalidate();

                isFloating = true;
            }

            final int width = viewPager.getMeasuredWidth() - (pageMargin * 2);
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

    @Override
    public void onResume() {
        super.onResume();

        markShown(getActivity(), welcomeRes);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        pageDots.detach();
        adapter.destroyDiagramVideoViews();
    }

    //endregion


    public static class ParallaxTransformer implements ViewPager.PageTransformer {
        private static final float SMALL_SCALE = 0.9f;
        private static final float BIG_SCALE = 1f;

        private static final float SMALL_ALPHA = 0.7f;
        private static final float BIG_ALPHA = 1f;

        private final ViewPager parent;

        public ParallaxTransformer(@NonNull ViewPager parent) {
            this.parent = parent;
        }

        @Override
        public void transformPage(View page, float position) {
            // https://code.google.com/p/android/issues/detail?id=64046
            final float fixedUpPosition = (position - parent.getPaddingRight() / (float) page.getWidth());
            final float lockedPosition = Math.max(0f, fixedUpPosition);

            final float scale = Anime.interpolateFloats(lockedPosition, BIG_SCALE, SMALL_SCALE);
            final float alpha = Anime.interpolateFloats(lockedPosition, BIG_ALPHA, SMALL_ALPHA);
            final float translationX = Anime.interpolateFloats(lockedPosition, 0f, -(parent.getWidth() / 5f));

            page.setScaleX(scale);
            page.setScaleY(scale);
            page.setTranslationX(translationX);
            page.setAlpha(alpha);
        }
    }


    //region Items

    public class ItemAdapter extends ViewPagerAdapter<ItemAdapter.BaseViewHolder> {
        private final LayoutInflater inflater = LayoutInflater.from(getActivity());
        private final List<DiagramVideoView> diagramVideoViews = new ArrayList<>(1);

        public void destroyDiagramVideoViews() {
            for (DiagramVideoView diagramVideoView : diagramVideoViews) {
                diagramVideoView.destroy();
            }
            diagramVideoViews.clear();

        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public BaseViewHolder createViewHolder(ViewGroup container, int position) {
            final Item item = items.get(position);
            final View itemView = inflater.inflate(R.layout.item_dialog_welcome, container, false);
            if (item.diagramVideo != null) {
                return new VideoHeaderViewHolder(itemView);
            } else {
                return new ImageHeaderViewHolder(itemView);
            }
        }

        @Override
        public void bindViewHolder(BaseViewHolder holder, int position) {
            final Item item = items.get(position);
            holder.bindItem(item);
            holder.setLastItem(position == getCount() - 1);
        }

        @Override
        public void unbindViewHolder(BaseViewHolder holder) {
            holder.unbind();
        }

        abstract class BaseViewHolder<THeader extends View> extends ViewPagerAdapter.ViewHolder {
            final THeader header;
            final TextView titleText;
            final TextView messageText;
            final Button dismissButton;
            final View dismissButtonDivider;

            private BaseViewHolder(@NonNull View itemView) {
                super(itemView);

                final LinearLayout contents = (LinearLayout) itemView.findViewById(R.id.item_dialog_welcome_contents);
                this.header = onProvideHeader(contents);
                contents.addView(header, 0);

                this.titleText = (TextView) itemView.findViewById(R.id.fragment_dialog_welcome_item_title);
                this.messageText = (TextView) itemView.findViewById(R.id.fragment_dialog_welcome_item_message);

                this.dismissButtonDivider = itemView.findViewById(R.id.fragment_dialog_welcome_item_dismiss_border);
                this.dismissButton = (Button) itemView.findViewById(R.id.fragment_dialog_welcome_item_dismiss);
                Views.setSafeOnClickListener(dismissButton, ignored -> dismissAllowingStateLoss());

                final Resources resources = getResources();
                final float bottomCornerRadius = resources.getDimension(R.dimen.button_corner_radius);
                final int normal = resources.getColor(R.color.background_light);
                final int pressed = resources.getColor(R.color.light_accent_extra_dimmed);
                dismissButton.setBackground(Styles.newRoundedBorderlessButtonBackground(0f, bottomCornerRadius,
                                                                                        normal, pressed));
            }

            protected abstract THeader onProvideHeader(@NonNull LinearLayout parent);

            private void setLastItem(boolean lastItem) {
                if (lastItem) {
                    dismissButtonDivider.setVisibility(View.VISIBLE);
                    dismissButton.setVisibility(View.VISIBLE);
                } else {
                    dismissButtonDivider.setVisibility(View.GONE);
                    dismissButton.setVisibility(View.GONE);
                }
            }

            protected void bindItem(@NonNull Item item) {
                if (item.titleRes != WelcomeDialogParser.MISSING_RES) {
                    titleText.setText(item.titleRes);
                    titleText.setVisibility(View.VISIBLE);
                } else {
                    titleText.setText("");
                    titleText.setVisibility(View.GONE);
                }

                String message = getString(item.messageRes);
                messageText.setText(message);
            }

            protected void unbind() {
            }
        }

        class ImageHeaderViewHolder extends BaseViewHolder<ImageView> {
            ImageHeaderViewHolder(@NonNull View itemView) {
                super(itemView);
            }

            @Override
            protected ImageView onProvideHeader(@NonNull LinearLayout parent) {
                final ImageView imageView = new ImageView(parent.getContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                     ViewGroup.LayoutParams.WRAP_CONTENT));
                return imageView;
            }

            protected void bindItem(@NonNull Item item) {
                super.bindItem(item);

                if (item.diagramRes != WelcomeDialogParser.MISSING_RES) {
                    header.setImageResource(item.diagramRes);
                } else {
                    header.setImageDrawable(null);
                }

                if (item.scaleDiagram) {
                    header.setAdjustViewBounds(true);
                    header.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else {
                    header.setAdjustViewBounds(false);
                    header.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }

                if (item.titleRes != WelcomeDialogParser.MISSING_RES) {
                    titleText.setText(item.titleRes);
                    titleText.setVisibility(View.VISIBLE);
                } else {
                    titleText.setText("");
                    titleText.setVisibility(View.GONE);
                }

                header.setContentDescription(getString(item.messageRes));
            }
        }

        class VideoHeaderViewHolder extends BaseViewHolder<DiagramVideoView> {
            VideoHeaderViewHolder(@NonNull View itemView) {
                super(itemView);
            }

            @Override
            protected DiagramVideoView onProvideHeader(@NonNull LinearLayout parent) {
                final DiagramVideoView diagramVideoView = new DiagramVideoView(parent.getContext());
                diagramVideoView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                            ViewGroup.LayoutParams.WRAP_CONTENT));
                diagramVideoViews.add(diagramVideoView);
                return diagramVideoView;
            }

            @Override
            protected void bindItem(@NonNull Item item) {
                super.bindItem(item);

                if (item.diagramRes != WelcomeDialogParser.MISSING_RES) {
                    header.setPlaceholder(item.diagramRes);
                } else {
                    header.setPlaceholder(null);
                }

               final  Uri diagramUri = Uri.parse(item.diagramVideo);
                header.setDataSource(diagramUri);

                final int radius = getResources().getDimensionPixelSize(R.dimen.raised_item_corner_radius);
                final float[] cornerRadii = {
                        radius, radius, radius, radius,
                        0f, 0f, 0f, 0f,
                };
                final ShapeDrawable background = new ShapeDrawable(new RoundRectShape(cornerRadii,
                                                                                      null, null));
                background.getPaint().setColor(item.diagramFillColor);
                header.setPadding(radius, 0, radius, 0);
                header.setBackground(background);
            }

            @Override
            protected void unbind() {
                diagramVideoViews.remove(header);
                header.destroy();
            }
        }
    }


    public static class Item {
        public final @DrawableRes int diagramRes;
        public final @StringRes int titleRes;
        public final @StringRes int messageRes;
        public final boolean scaleDiagram;
        public final @Nullable String diagramVideo;
        public final @ColorInt int diagramFillColor;

        public Item(@DrawableRes int diagramRes,
                    @StringRes int titleRes,
                    @StringRes int messageRes,
                    boolean scaleDiagram,
                    @Nullable String diagramVideo,
                    @ColorInt int diagramFillColor) {
            this.diagramRes = diagramRes;
            this.titleRes = titleRes;
            this.messageRes = messageRes;
            this.scaleDiagram = scaleDiagram;
            this.diagramVideo = diagramVideo;
            this.diagramFillColor = diagramFillColor;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "diagramRes=" + diagramRes +
                    ", titleRes=" + titleRes +
                    ", messageRes=" + messageRes +
                    ", scaleDiagram=" + scaleDiagram +
                    ", diagramVideo='" + diagramVideo + '\'' +
                    '}';
        }
    }

    //endregion
}
