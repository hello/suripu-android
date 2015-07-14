package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import is.hello.sense.R;

public class StaticItemAdapter extends ArrayAdapter<StaticItemAdapter.Item> {
    private final LayoutInflater layoutInflater;
    private final Resources resources;

    public StaticItemAdapter(Context context) {
        super(context, R.layout.item_static_text);

        this.layoutInflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }


    //region Adding Items

    private @NonNull String getStringSafe(@StringRes int stringRes) {
        if (stringRes == 0) {
            return "";
        } else {
            return resources.getString(stringRes);
        }
    }

    public Item addSectionTitle(@NonNull String title) {
        Item item = new Item(title, null);
        add(item);
        return item;
    }

    public Item addSectionDivider() {
        return addSectionTitle("");
    }

    public Item addSectionTitle(@StringRes int titleRes) {
        return addSectionTitle(getStringSafe(titleRes));
    }

    public TextItem addTextItem(@NonNull String title, @Nullable String value, @Nullable Runnable action) {
        TextItem item = new TextItem(title, value, action);
        add(item);
        return item;
    }

    public TextItem addTextItem(@StringRes int titleRes, @StringRes int valueRes, @Nullable Runnable action) {
        return addTextItem(getStringSafe(titleRes), getStringSafe(valueRes), action);
    }

    public TextItem addTextItem(@NonNull String title, @Nullable String value) {
        TextItem item = new TextItem(title, value, null);
        add(item);
        return item;
    }

    public TextItem addTextItem(@StringRes int titleRes, @StringRes int valueRes) {
        return addTextItem(getStringSafe(titleRes), getStringSafe(valueRes));
    }

    public CheckItem addCheckItem(@NonNull String title, boolean checked, @Nullable Runnable action) {
        CheckItem item = new CheckItem(title, checked, action);
        add(item);
        return item;
    }

    public CheckItem addCheckItem(@StringRes int titleRes, boolean checked, @Nullable Runnable action) {
        return addCheckItem(getStringSafe(titleRes), checked, action);
    }

    public Item addFooterItem(@NonNull String title) {
        Item item = new Item(ItemType.SECTION_FOOTER, title, null);
        add(item);
        return item;
    }

    public Item addFooterItem(@StringRes int titleRes) {
        return addFooterItem(getStringSafe(titleRes));
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
            case SECTION_TITLE: {
                if (view == null) {
                    view = layoutInflater.inflate(R.layout.item_section_title, parent, false);
                    view.setTag(new SectionItemViewHolder(view));
                }

                SectionItemViewHolder holder = (SectionItemViewHolder) view.getTag();
                String itemTitle = item.getTitle();
                if (TextUtils.isEmpty(itemTitle)) {
                    holder.title.setText(null);
                    holder.title.setVisibility(View.GONE);
                } else {
                    holder.title.setText(itemTitle);
                    holder.title.setVisibility(View.VISIBLE);
                }
                if (position == 0) {
                    holder.divider.setVisibility(View.GONE);
                } else {
                    holder.divider.setVisibility(View.VISIBLE);
                }

                break;
            }

            case TEXT_ITEM: {
                if (view == null) {
                    view = layoutInflater.inflate(R.layout.item_static_text, parent, false);
                    view.setTag(new TextItemViewHolder(view));
                }

                TextItem textItem = (TextItem) item;
                TextItemViewHolder holder = (TextItemViewHolder) view.getTag();
                holder.title.setText(textItem.getTitle());
                holder.detail.setText(textItem.getDetail());

                break;
            }

            case CHECK_ITEM: {
                if (view == null) {
                    view = layoutInflater.inflate(R.layout.item_static_check, parent, false);
                    view.setTag(new CheckItemViewHolder(view));
                }

                CheckItem checkItem = (CheckItem) item;
                CheckItemViewHolder holder = (CheckItemViewHolder) view.getTag();
                holder.title.setText(checkItem.getTitle());
                holder.check.setChecked(checkItem.isChecked());

                break;
            }

            case SECTION_FOOTER: {
                if (view == null) {
                    view = layoutInflater.inflate(R.layout.item_section_footer, parent, false);
                    view.setTag(new SectionFooterViewHolder(view));
                }

                SectionFooterViewHolder holder = (SectionFooterViewHolder) view.getTag();
                holder.text.setText(item.getTitle());

                break;
            }

            default: {
                throw new IllegalArgumentException("Unknown type " + type);
            }
        }

        return view;
    }

    static class SectionItemViewHolder {
        final View divider;
        final TextView title;

        SectionItemViewHolder(@NonNull View view) {
            this.divider = view.findViewById(R.id.item_section_title_divider);
            this.title = (TextView) view.findViewById(R.id.item_section_title_text);
        }
    }

    static class TextItemViewHolder {
        final TextView title;
        final TextView detail;

        TextItemViewHolder(@NonNull View view) {
            this.title = (TextView) view.findViewById(R.id.item_static_text_title);
            this.detail = (TextView) view.findViewById(R.id.item_static_text_detail);
        }
    }

    static class CheckItemViewHolder {
        final TextView title;
        final CheckBox check;

        CheckItemViewHolder(@NonNull View view) {
            this.title = (TextView) view.findViewById(R.id.item_static_check_title);
            this.check = (CheckBox) view.findViewById(R.id.item_static_check_box);
        }
    }

    static class SectionFooterViewHolder {
        final TextView text;

        SectionFooterViewHolder(@NonNull View view) {
            this.text = (TextView) view.findViewById(R.id.item_section_footer);
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

    public class CheckItem extends Item {
        private boolean checked;

        public CheckItem(@NonNull String title, boolean checked, @Nullable Runnable action) {
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
        SECTION_TITLE,
        TEXT_ITEM,
        CHECK_ITEM,
        SECTION_FOOTER,
    }
}
