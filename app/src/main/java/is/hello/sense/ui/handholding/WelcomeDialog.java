package is.hello.sense.ui.handholding;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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

import is.hello.sense.R;
import is.hello.sense.ui.adapter.ViewPagerAdapter;
import is.hello.sense.ui.handholding.util.WelcomeDialogParser;
import is.hello.sense.ui.widget.PageDots;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;

public class WelcomeDialog extends DialogFragment {
    public static final String TAG = WelcomeDialog.class.getSimpleName();

    private static final String ARG_ITEMS = WelcomeDialog.class.getName() + ".ARG_ITEMS";

    private Item[] items;
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

        String key = getPreferenceKey(context, welcomeRes);
        SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        return !preferences.getBoolean(key, false);
    }

    public static void markShown(@NonNull Context context, @XmlRes int welcomeRes) {
        String key = getPreferenceKey(context, welcomeRes);
        SharedPreferences preferences = context.getSharedPreferences(Constants.HANDHOLDING_PREFS, 0);
        preferences.edit()
                   .putBoolean(key, true)
                   .apply();
    }

    public static boolean show(@NonNull Activity activity, @XmlRes int welcomeRes) {
        try {
            WelcomeDialogParser parser = new WelcomeDialogParser(activity.getResources(), welcomeRes);
            WelcomeDialog.Item[] items = parser.parse();

            WelcomeDialog welcomeDialog = WelcomeDialog.newInstance(items);
            welcomeDialog.show(activity.getFragmentManager(), WelcomeDialog.TAG);

            markShown(activity, welcomeRes);
        } catch (XmlPullParserException | IOException e) {
            Logger.error(TAG, "Could not parse welcome document", e);
            return false;
        }

        return true;
    }

    public static boolean showIfNeeded(@NonNull Activity activity, @XmlRes int welcomeRes) {
        return (shouldShow(activity, welcomeRes) &&
                show(activity, welcomeRes));
    }

    public static WelcomeDialog newInstance(@NonNull Item[] items) {
        WelcomeDialog fragment = new WelcomeDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelableArray(ARG_ITEMS, items);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.items = (Item[]) getArguments().getParcelableArray(ARG_ITEMS);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_FullScreen);
        dialog.setContentView(R.layout.dialog_welcome);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int tintedStatusBar = getResources().getColor(R.color.status_bar_dimmed);
            dialog.getWindow().setStatusBarColor(tintedStatusBar);
        }

        this.viewPager = (ViewPager) dialog.findViewById(R.id.dialog_welcome_view_pager);

        int pageMargin = getResources().getDimensionPixelSize(R.dimen.gap_outer);
        viewPager.setClipToPadding(false);
        viewPager.setPadding(pageMargin, 0, pageMargin, 0);
        viewPager.setPageMargin(pageMargin);

        this.adapter = new ItemAdapter();
        viewPager.setAdapter(adapter);

        if (items.length > 1) {
            PageDots pageDots = (PageDots) dialog.findViewById(R.id.dialog_welcome_page_dots);
            pageDots.attach(viewPager);
        }

        int maxWidth = getResources().getDimensionPixelSize(R.dimen.dialog_max_width);
        Views.observeNextLayout(dialog.getWindow().getDecorView())
             .subscribe(v -> {
                 int width = viewPager.getMeasuredWidth() - (pageMargin * 2);
                 if (width > maxWidth) {
                     int newPageMargin = (viewPager.getMeasuredWidth() - maxWidth) / 2;
                     viewPager.setPadding(newPageMargin, 0, newPageMargin, 0);
                     viewPager.setPageMargin(newPageMargin);
                 }

             });

        return dialog;
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
            return items.length;
        }

        @Override
        public ViewHolder createViewHolder(ViewGroup container, int position) {
            View itemView = inflater.inflate(R.layout.item_dialog_welcome, container, false);
            return new ViewHolder(itemView, (position == getCount() - 1));
        }

        @Override
        public void bindViewHolder(ViewHolder holder, int position) {
            Item item = items[position];
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


    public static class Item implements Parcelable {
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


        //region Parcelable

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(diagramRes);
            out.writeInt(titleRes);
            out.writeInt(messageRes);
            out.writeInt(scaleDiagram ? 1 : 0);
        }

        private Item(@NonNull Parcel in) {
            this(in.readInt(), in.readInt(), in.readInt(), in.readInt() == 1);
        }

        public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
            @Override
            public Item createFromParcel(Parcel source) {
                return new Item(source);
            }

            @Override
            public Item[] newArray(int size) {
                return new Item[size];
            }
        };

        //endregion
    }
}
