package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import is.hello.sense.R;

public class SettingsRecyclerAdapter extends ArrayRecyclerAdapter<SettingsRecyclerAdapter.Item,
        SettingsRecyclerAdapter.ViewHolder> {
    private final Resources resources;
    protected final LayoutInflater inflater;
    private boolean wantsDividers = true;

    public SettingsRecyclerAdapter(@NonNull Context context) {
        super(new ArrayList<>());

        this.resources = context.getResources();
        this.inflater = LayoutInflater.from(context);
    }

    public void setWantsDividers(boolean wantsDividers) {
        this.wantsDividers = wantsDividers;
        notifyDataSetChanged();
    }

    //region Binding

    @Override
    public boolean add(Item item) {
        if (super.add(item)) {
            item.bind(this, getItemCount() - 1);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getItemViewType(int position) {
        final Item item = getItem(position);
        if (item instanceof DetailItem) {
            return DetailItem.ID;
        } else if (item instanceof ToggleItem) {
            return ToggleItem.ID;
        } else if (item instanceof TextItem){
            return TextItem.ID;
        } else{
            return Item.ID;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case DetailItem.ID: {
                final View view = inflater.inflate(R.layout.item_settings_detail, parent, false);
                return new DetailViewHolder(view);
            }
            case ToggleItem.ID: {
                final View view = inflater.inflate(R.layout.item_settings_toggle, parent, false);
                return new ToggleViewHolder(view);
            }
            case TextItem.ID: {
                final View view = inflater.inflate(R.layout.item_settings_text, parent, false);
                return new TextViewHolder(view);
            }
            default: {
                throw new IllegalArgumentException("Unknown view type " + viewType);
            }
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Item item = getItem(position);
        //noinspection unchecked
        holder.bind(item);
    }

    //endregion


    //region View Holders

    abstract class ViewHolder<T extends Item> extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        ViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setBackgroundResource(R.drawable.selectable_dark_bounded);
            itemView.setOnClickListener(this);
        }

        abstract void bind(T item);

        @Override
        public void onClick(View ignored) {
            // View dispatches OnClickListener#onClick(View) calls on
            // the next looper cycle. It's possible for the adapter's
            // containing recycler view to update and invalidate a
            // view holder before the callback fires.
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                final Item item = getItem(adapterPosition);
                final Runnable onClick = item.onClick;
                if (onClick != null) {
                    onClick.run();
                }
            }
        }
    }

    class TextViewHolder extends ViewHolder<TextItem> {
        final TextView text;

        TextViewHolder(@NonNull View itemView) {
            super(itemView);

            this.text = (TextView) itemView.findViewById(R.id.item_text);
        }

        @Override
        void bind(TextItem item) {
            text.setText(item.text);
        }
    }

    class DetailViewHolder extends ViewHolder<DetailItem> {
        final ImageView icon;
        final TextView title;
        final TextView detail;
        final View divider;

        DetailViewHolder(@NonNull View itemView) {
            super(itemView);

            this.icon = (ImageView) itemView.findViewById(R.id.item_settings_detail_icon);
            this.title = (TextView) itemView.findViewById(R.id.item_settings_detail_title);
            this.detail = (TextView) itemView.findViewById(R.id.item_settings_detail_detail);
            this.divider = itemView.findViewById(R.id.item_settings_detail_divider);
        }

        @Override
        void bind(DetailItem item) {
            if (item.icon != 0) {
                icon.setImageResource(item.icon);
                icon.setContentDescription(resources.getString(item.iconContentDescription));
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setImageDrawable(null);
                icon.setVisibility(View.GONE);
            }
            title.setText(item.text);
            detail.setText(item.value);

            if (!wantsDividers || item.position == getItemCount() - 1) {
                divider.setVisibility(View.GONE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }
        }
    }

    class ToggleViewHolder extends ViewHolder<ToggleItem> {
        final TextView title;
        final CompoundButton toggle;

        ToggleViewHolder(@NonNull View itemView) {
            super(itemView);

            this.title = (TextView) itemView.findViewById(R.id.item_settings_toggle_check_title);
            this.toggle = (CompoundButton) itemView.findViewById(R.id.item_settings_toggle_check_box);
        }

        @Override
        void bind(ToggleItem item) {
            title.setText(item.text);
            toggle.setChecked(item.value);
        }
    }

    //endregion


    //region Items

    public static class Item<T> {
        static final int ID = 0;

        T value;
        final @Nullable Runnable onClick;

        @Nullable SettingsRecyclerAdapter adapter;
        int position = RecyclerView.NO_POSITION;

        public Item(@Nullable Runnable onClick) {
            this.onClick = onClick;
        }

        void bind(@NonNull SettingsRecyclerAdapter adapter, int position) {
            this.position = position;
            this.adapter = adapter;
        }

        public void setValue(T value) {
            this.value = value;
            notifyChanged();
        }

        public T getValue() {
            return value;
        }

        protected void notifyChanged(){
            if (position != RecyclerView.NO_POSITION && adapter != null) {
                adapter.notifyItemChanged(position);
            }
        }
    }

    public static class TextItem<T> extends Item<T>{
        static final int ID = 1;

        String text;

        public TextItem(@NonNull String text,
                        @Nullable Runnable onClick) {
            super(onClick);
            this.text = text;
        }

        public void setText(String text) {
            this.text = text;
            notifyChanged();
        }
    }

    public static class DetailItem extends TextItem<String> {
        static final int ID = 2;

        @DrawableRes int icon;
        @StringRes int iconContentDescription;

        public DetailItem(@NonNull String title, @Nullable Runnable onClick) {
            super(title, onClick);
        }

        public void setIcon(@DrawableRes int icon, @StringRes int iconContentDescription) {
            this.icon = icon;
            this.iconContentDescription = iconContentDescription;
        }
    }

    public static class ToggleItem extends TextItem<Boolean> {
        static final int ID = 3;

        public ToggleItem(@NonNull String title, @Nullable Runnable onClick) {
            super(title, onClick);

            setValue(false);
        }
    }

    //endregion
}
