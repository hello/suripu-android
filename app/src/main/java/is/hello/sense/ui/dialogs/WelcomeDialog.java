package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;

public class WelcomeDialog extends DialogFragment {
    public static final String TAG = WelcomeDialog.class.getSimpleName();

    private static final String ARG_ITEMS = WelcomeDialog.class.getName() + ".ARG_ITEMS";

    private Item[] items;
    private ItemAdapter adapter;
    private ViewPager viewPager;


    //region Lifecycle

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
        this.adapter = new ItemAdapter(getChildFragmentManager());
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


    public class ItemAdapter extends FragmentStatePagerAdapter {
        public ItemAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Fragment getItem(int position) {
            return ItemFragment.newInstance(WelcomeDialog.this, items[position]);
        }
    }

    public static class ItemFragment extends Fragment {
        private static final String ARG_ITEM = ItemFragment.class.getName() + ".ARG_ITEM";

        private Item item;
        private WelcomeDialog parentFragment;


        //region Lifecycle

        public static ItemFragment newInstance(@NonNull WelcomeDialog targetFragment, @NonNull Item item) {
            ItemFragment fragment = new ItemFragment();

            Bundle arguments = new Bundle();
            arguments.putParcelable(ARG_ITEM, item);
            fragment.setArguments(arguments);
            fragment.setTargetFragment(targetFragment, 0x00);

            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            this.item = getArguments().getParcelable(ARG_ITEMS);
            this.parentFragment = (WelcomeDialog) getTargetFragment();
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_dialog_welcome_item, container, false);

            ImageView diagram = (ImageView) view.findViewById(R.id.fragment_dialog_welcome_item_diagram);
            diagram.setImageResource(item.diagramRes);

            TextView title = (TextView) view.findViewById(R.id.fragment_dialog_welcome_item_title);
            title.setText(item.titleRes);

            TextView message = (TextView) view.findViewById(R.id.fragment_dialog_welcome_item_message);
            message.setText(item.messageRes);

            Button next = (Button) view.findViewById(R.id.fragment_dialog_welcome_item_next);
            Views.setSafeOnClickListener(next, ignored -> parentFragment.next());

            return view;
        }

        //endregion
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
