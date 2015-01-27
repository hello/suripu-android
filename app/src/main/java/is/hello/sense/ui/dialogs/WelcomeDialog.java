package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v4.view.PagerAdapter;
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
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Logger;
import is.hello.sense.util.WelcomeDialogParser;

public class WelcomeDialog extends DialogFragment {
    public static final String TAG = WelcomeDialog.class.getSimpleName();

    private static final String ARG_ITEMS = WelcomeDialog.class.getName() + ".ARG_ITEMS";

    private Item[] items;
    private ItemAdapter adapter;
    private ViewPager viewPager;


    //region Lifecycle

    public static boolean show(@NonNull Activity activity, @XmlRes int welcomeRes) {
        try {
            WelcomeDialogParser parser = new WelcomeDialogParser(activity.getResources(), welcomeRes);
            WelcomeDialog.Item[] items = parser.parse();

            WelcomeDialog welcomeDialog = WelcomeDialog.newInstance(items);
            welcomeDialog.show(activity.getFragmentManager(), WelcomeDialog.TAG);
        } catch (XmlPullParserException | IOException e) {
            Logger.error(TAG, "Could not parse welcome document", e);
            return false;
        }

        return true;
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

        this.viewPager = (ViewPager) dialog.findViewById(R.id.dialog_welcome_view_pager);
        this.adapter = new ItemAdapter();
        viewPager.setAdapter(adapter);

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


    public class ItemAdapter extends PagerAdapter {
        private final LayoutInflater inflater = LayoutInflater.from(getActivity());

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            ViewHolder holder = (ViewHolder) object;
            return (holder.itemView == view);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = inflater.inflate(R.layout.fragment_dialog_welcome_item, container, false);
            ViewHolder holder = new ViewHolder(itemView);
            holder.bindItem(items[position]);
            container.addView(itemView);
            return holder;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ViewHolder holder = (ViewHolder) object;
            container.removeView(holder.itemView);
        }


        private class ViewHolder {
            final View itemView;
            private final ImageView diagram;
            private final TextView title;
            private final TextView message;

            private ViewHolder(@NonNull View itemView) {
                this.itemView = itemView;

                this.diagram = (ImageView) itemView.findViewById(R.id.fragment_dialog_welcome_item_diagram);
                this.title = (TextView) itemView.findViewById(R.id.fragment_dialog_welcome_item_title);
                this.message = (TextView) itemView.findViewById(R.id.fragment_dialog_welcome_item_message);

                Button next = (Button) itemView.findViewById(R.id.fragment_dialog_welcome_item_next);
                Views.setSafeOnClickListener(next, ignored -> next());
            }

            private void bindItem(@NonNull Item item) {
                if (item.diagramRes != WelcomeDialogParser.MISSING_RES) {
                    diagram.setImageResource(item.diagramRes);
                } else {
                    diagram.setImageDrawable(null);
                }

                if (item.titleRes != WelcomeDialogParser.MISSING_RES) {
                    title.setText(item.titleRes);
                } else {
                    title.setText("");
                }

                message.setText(item.messageRes);
            }
        }
    }


    public static class Item implements Parcelable {
        public final @DrawableRes int diagramRes;
        public final @StringRes int titleRes;
        public final @StringRes int messageRes;

        public Item(@DrawableRes int diagramRes,
                    @StringRes int titleRes,
                    @StringRes int messageRes) {
            this.diagramRes = diagramRes;
            this.titleRes = titleRes;
            this.messageRes = messageRes;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "diagramRes=" + diagramRes +
                    ", titleRes=" + titleRes +
                    ", messageRes=" + messageRes +
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
        }

        private Item(@NonNull Parcel in) {
            this(in.readInt(), in.readInt(), in.readInt());
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
