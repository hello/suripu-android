package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import is.hello.sense.R;
import rx.functions.Action1;

public class StaticItemAdapter extends ArrayAdapter<StaticItemAdapter.Item>
        implements ListView.OnItemClickListener {
    private final LayoutInflater layoutInflater;
    private final Resources resources;

    public StaticItemAdapter(Context context) {
        super(context, R.layout.item_settings_detail);

        this.layoutInflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }


    //region Click Support

    @SuppressWarnings("unchecked")
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Item item = (Item) parent.getItemAtPosition(position);
        if (item != null && item.onClick != null) {
            item.onClick.call(item);
        }
    }

    //endregion

    //region Adding Items

    private @NonNull String getStringSafe(@StringRes int stringRes) {
        if (stringRes == 0) {
            return "";
        } else {
            return resources.getString(stringRes);
        }
    }

    public TextItem addTextItem(@NonNull String title, @Nullable Action1<TextItem> onClick) {
        TextItem item = new TextItem(title, onClick);
        add(item);
        return item;
    }

    public TextItem addTextItem(@NonNull String title) {
        TextItem item = new TextItem(title, null);
        add(item);
        return item;
    }

    public CheckItem addCheckItem(@NonNull String title, boolean checked, @Nullable Action1<CheckItem> onClick) {
        CheckItem item = new CheckItem(title, checked, onClick);
        add(item);
        return item;
    }

    public CheckItem addCheckItem(@StringRes int titleRes, boolean checked, @Nullable Action1<CheckItem> onClick) {
        return addCheckItem(getStringSafe(titleRes), checked, onClick);
    }

    //endregion


    //region Views

    @Override
    public int getViewTypeCount() {
        return ItemType.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        Item item = getItem(position);
        return item.type.ordinal();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        Item item = getItem(position);
        ItemType type = item.type;

        switch (type) {
            case TEXT_ITEM: {
                if (view == null) {
                    view = layoutInflater.inflate(R.layout.item_settings_detail, parent, false);
                    view.setTag(new TextItemViewHolder(view));
                }

                TextItem textItem = (TextItem) item;
                TextItemViewHolder holder = (TextItemViewHolder) view.getTag();
                holder.title.setText(textItem.getTitle());
                holder.detail.setText(null);

                break;
            }

            case CHECK_ITEM: {
                if (view == null) {
                    view = layoutInflater.inflate(R.layout.item_settings_toggle, parent, false);
                    view.setTag(new CheckItemViewHolder(view));
                }

                CheckItem checkItem = (CheckItem) item;
                CheckItemViewHolder holder = (CheckItemViewHolder) view.getTag();
                holder.title.setText(checkItem.getTitle());
                holder.check.setChecked(checkItem.isChecked());

                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown type " + type);
            }
        }

        return view;
    }

    static class TextItemViewHolder {
        final TextView title;
        final TextView detail;

        TextItemViewHolder(@NonNull View view) {
            this.title = (TextView) view.findViewById(R.id.item_settings_detail_title);
            this.detail = (TextView) view.findViewById(R.id.item_settings_detail_detail);
        }
    }

    static class CheckItemViewHolder {
        final TextView title;
        final CheckBox check;

        CheckItemViewHolder(@NonNull View view) {
            this.title = (TextView) view.findViewById(R.id.item_settings_toggle_check_title);
            this.check = (CheckBox) view.findViewById(R.id.item_settings_toggle_check_box);
        }
    }

    //endregion


    public static class Item {
        final @NonNull ItemType type;
        final @NonNull String title;
        @Nullable Action1 onClick;

        protected Item(@NonNull ItemType type,
                       @NonNull String title,
                       @Nullable Action1<? extends Item> onClick) {
            this.type = type;
            this.title = title;
            this.onClick = onClick;
        }


        public @NonNull String getTitle() {
            return title;
        }

        @SuppressWarnings("unchecked")
        public @Nullable <T extends Item> Action1<T> getOnClick() {
            return onClick;
        }

        public void setOnClick(@Nullable Action1<? extends Item> onClick) {
            this.onClick = onClick;
        }
    }

    public class TextItem extends Item {
        public TextItem(@NonNull String title,
                        @Nullable Action1<TextItem> action) {
            super(ItemType.TEXT_ITEM, title, action);
        }
    }

    public class CheckItem extends Item {
        private boolean checked;

        public CheckItem(@NonNull String title,
                         boolean checked,
                         @Nullable Action1<CheckItem> action) {
            super(ItemType.CHECK_ITEM, title, action);

            this.checked = checked;
        }

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
            notifyDataSetChanged();
        }
    }


    public enum ItemType {
        TEXT_ITEM,
        CHECK_ITEM,
    }
}
