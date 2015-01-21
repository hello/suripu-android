package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import is.hello.sense.R;

public class StaticItemAdapter extends ArrayAdapter<StaticItemAdapter.Item> {
    private final LayoutInflater layoutInflater;
    private final Resources resources;

    public StaticItemAdapter(Context context) {
        super(context, R.layout.item_static);

        this.layoutInflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }


    //region Adding Items

    public TextItem addItem(@NonNull String title, @Nullable String value, @Nullable Runnable action) {
        TextItem item = new TextItem(title, value, action);
        add(item);
        return item;
    }

    public TextItem addItem(@StringRes int titleRes, @StringRes int valueRes, @Nullable Runnable action) {
        return addItem(resources.getString(titleRes), resources.getString(valueRes), action);
    }

    public TextItem addItem(@NonNull String title, @Nullable String value) {
        TextItem item = new TextItem(title, value, null);
        add(item);
        return item;
    }

    public TextItem addItem(@StringRes int titleRes, @StringRes int valueRes) {
        return addItem(resources.getString(titleRes), resources.getString(valueRes));
    }

    public Item addTitle(@NonNull String title) {
        Item item = new Item(title, null);
        add(item);
        return item;
    }

    public Item addTitle(@StringRes int titleRes) {
        return addTitle(resources.getString(titleRes));
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
        return item.getType().ordinal();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        Item item = getItem(position);
        ItemType type = item.getType();

        switch (type) {
            case TEXT_ITEM: {
                if (view == null) {
                    view = layoutInflater.inflate(R.layout.item_static, parent, false);
                    view.setTag(new HorizontalItemViewHolder(view));
                }

                TextItem textItem = (TextItem) item;
                HorizontalItemViewHolder holder = (HorizontalItemViewHolder) view.getTag();
                holder.title.setText(textItem.getTitle());
                holder.detail.setText(textItem.getDetail());

                break;
            }

            case SECTION_TITLE: {
                if (view == null) {
                    view = layoutInflater.inflate(R.layout.item_section_title, parent, false);
                    view.setTag(new SectionItemViewHolder(view));
                }

                SectionItemViewHolder holder = (SectionItemViewHolder) view.getTag();
                holder.title.setText(item.getTitle());
                if (position == 0) {
                    holder.divider.setVisibility(View.GONE);
                } else {
                    holder.divider.setVisibility(View.VISIBLE);
                }

                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown type " + type);
            }
        }

        return view;
    }

    private class HorizontalItemViewHolder {
        private final TextView title;
        private final TextView detail;

        private HorizontalItemViewHolder(@NonNull View view) {
            this.title = (TextView) view.findViewById(R.id.list_horizontal_item_title);
            this.detail = (TextView) view.findViewById(R.id.list_horizontal_item_detail);
        }
    }

    private class SectionItemViewHolder {
        private final View divider;
        private final TextView title;

        private SectionItemViewHolder(@NonNull View view) {
            this.divider = view.findViewById(R.id.item_section_title_divider);
            this.title = (TextView) view.findViewById(R.id.item_section_title_text);
        }
    }

    //endregion


    public static class Item {
        private final ItemType type;
        private final String title;
        private Runnable action;

        protected Item(ItemType type, String title, Runnable action) {
            this.type = type;
            this.title = title;
            this.action = action;
        }

        public Item(@NonNull String title, @Nullable Runnable action) {
            this(ItemType.SECTION_TITLE, title, action);
        }


        public ItemType getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public Runnable getAction() {
            return action;
        }

        public void setAction(Runnable action) {
            this.action = action;
        }
    }

    public class TextItem extends Item {
        private String detail;

        public TextItem(@NonNull String title, @Nullable String detail, @Nullable Runnable action) {
            super(ItemType.TEXT_ITEM, title, action);
            this.detail = detail;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
            notifyDataSetChanged();
        }
    }


    public static enum ItemType {
        SECTION_TITLE,
        TEXT_ITEM,
    }
}
