package is.hello.sense.ui.handholding;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import is.hello.sense.ui.handholding.util.DismissTutorialsDialog;
import is.hello.sense.ui.handholding.util.WelcomeDialogParser;
import is.hello.sense.ui.widget.PageDots;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

public class WelcomeDialogFragment extends SenseDialogFragment {
    public static final String TAG = WelcomeDialogFragment.class.getSimpleName();

    private static final int REQUEST_CODE_DISMISS_ALL = 0x99;
    private static final String ARG_WELCOME_RES = WelcomeDialogFragment.class.getName() + ".ARG_WELCOME_RES";

    private List<Item> items;
    private ItemAdapter adapter;
    private ViewPager viewPager;


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
        return (!preferences.getBoolean(Constants.HANDHOLDING_SUPPRESSED, false) &&
                !preferences.getBoolean(getPreferenceKey(context, welcomeRes), false));
    }

    public static void markShown(@NonNull Context context, @XmlRes int welcomeRes) {
        String key = getPreferenceKey(context, welcomeRes);
        SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        preferences.edit()
                   .putBoolean(key, true)
                   .apply();
    }

    public static void show(@NonNull Activity activity, @XmlRes int welcomeRes) {
        WelcomeDialogFragment welcomeDialog = WelcomeDialogFragment.newInstance(welcomeRes);
        welcomeDialog.show(activity.getFragmentManager(), WelcomeDialogFragment.TAG);

        markShown(activity, welcomeRes);
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

        int pageMargin = getResources().getDimensionPixelSize(R.dimen.gap_medium);
        viewPager.setClipToPadding(false);
        viewPager.setPadding(pageMargin, 0, pageMargin, 0);
        viewPager.setPageMargin(pageMargin);

        this.adapter = new ItemAdapter();
        viewPager.setAdapter(adapter);

        PageDots pageDots = (PageDots) dialog.findViewById(R.id.fragment_dialog_welcome_page_dots);
        if (items.size() > 1) {
            pageDots.attach(viewPager);
        } else {
            pageDots.setVisibility(View.GONE);

            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewPager.getLayoutParams();
            layoutParams.bottomMargin = layoutParams.topMargin;
        }

        int maxWidth = getResources().getDimensionPixelSize(R.dimen.dialog_max_width);
        int maxHeight = getResources().getDimensionPixelSize(R.dimen.dialog_max_height);
        viewPager.setVisibility(View.INVISIBLE);
        Views.observeNextLayout(dialog.getWindow().getDecorView())
             .subscribe(v -> {
                 int height = viewPager.getMeasuredHeight();
                 if (height > maxHeight) {
                     viewPager.getLayoutParams().height = maxHeight;
                     viewPager.invalidate();
                 }

                 int width = viewPager.getMeasuredWidth() - (pageMargin * 2);
                 if (width > maxWidth) {
                     int newPageMargin = (viewPager.getMeasuredWidth() - maxWidth) / 2;
                     viewPager.setPadding(newPageMargin, 0, newPageMargin, 0);
                     viewPager.setPageMargin(newPageMargin);
                 }

                 viewPager.post(() -> viewPager.setVisibility(View.VISIBLE));
             });

        return dialog;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_DISMISS_ALL && resultCode == Activity.RESULT_OK) {
            dismiss();
        }
    }

    //endregion


    public void next() {
        if (viewPager.getCurrentItem() < adapter.getCount() - 1) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
        } else {
            dismiss();
        }
    }


    public class ItemAdapter extends ViewPagerAdapter<ItemAdapter.ViewHolder> {
        private final LayoutInflater inflater = LayoutInflater.from(getActivity());

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public ViewHolder createViewHolder(ViewGroup container, int position) {
            View itemView = inflater.inflate(R.layout.item_dialog_welcome, container, false);
            return new ViewHolder(itemView, (position == getCount() - 1));
        }

        @Override
        public void bindViewHolder(ViewHolder holder, int position) {
            Item item = items.get(position);
            holder.bindItem(item);
        }


        class ViewHolder extends ViewPagerAdapter.ViewHolder {
            final ImageView diagramImage;
            final TextView titleText;
            final TextView messageText;

            private ViewHolder(@NonNull View itemView, boolean lastItem) {
                super(itemView);

                this.diagramImage = (ImageView) itemView.findViewById(R.id.fragment_dialog_welcome_item_diagram);
                this.titleText = (TextView) itemView.findViewById(R.id.fragment_dialog_welcome_item_title);
                this.messageText = (TextView) itemView.findViewById(R.id.fragment_dialog_welcome_item_message);

                Button next = (Button) itemView.findViewById(R.id.fragment_dialog_welcome_item_next);
                if (lastItem) {
                    next.setText(android.R.string.ok);
                } else {
                    next.setText(R.string.action_next);
                }
                Views.setSafeOnClickListener(next, ignored -> next());
                next.setOnLongClickListener(ignored -> {
                    DismissTutorialsDialog tutorialsDialog = new DismissTutorialsDialog();
                    tutorialsDialog.setTargetFragment(WelcomeDialogFragment.this, REQUEST_CODE_DISMISS_ALL);
                    tutorialsDialog.show(getFragmentManager(), DismissTutorialsDialog.TAG);
                    return true;
                });
            }

            private void bindItem(@NonNull Item item) {
                if (item.diagramRes != WelcomeDialogParser.MISSING_RES) {
                    diagramImage.setImageResource(item.diagramRes);
                } else {
                    diagramImage.setImageDrawable(null);
                }

                if (item.scaleDiagram) {
                    diagramImage.setAdjustViewBounds(true);
                    diagramImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
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
}
