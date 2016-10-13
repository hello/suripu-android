package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Configuration;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

public class ConfigurationAdapter extends ArrayRecyclerAdapter<Configuration, ConfigurationAdapter.BaseViewHolder> {

    public static final int TYPE_CONFIG = 0;
    public static final int TYPE_EMPTY_CONFIG = 1;
    private int selectedItemPosition = NO_POSITION;

    public ConfigurationAdapter(@NonNull final List<Configuration> storage) {
        super(storage);
    }

    @Override
    public int getItemViewType(final int position) {
        return getItem(position) instanceof Configuration.Empty ? TYPE_EMPTY_CONFIG : TYPE_CONFIG;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case TYPE_CONFIG:
                final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row_configuration, parent, false);
                return new ConfigurationViewHolder(view);
            case TYPE_EMPTY_CONFIG:
                final View view2 = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row_no_configuration, parent, false);
                return new EmptyConfigurationViewHolder(view2);
            default:
                throw new IllegalStateException("illegal viewType: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);
    }

    /**
     * @return true if there was a configuration in list which was selected.
     */
    public boolean setSelectedItemFromList(@NonNull final List<? extends Configuration> list) {
        for (int i = 0; i < list.size(); i++) {
            if(list.get(i).isSelected()){
                this.selectedItemPosition = i;
                return true;
            }
        }
        this.selectedItemPosition = NO_POSITION;
        return false;
    }

    public void setSelectedItem(final int position) {
        if (position < 0 || position >= getItemCount()) {
            return;
        }
        final int oldPosition = this.selectedItemPosition;
        if(oldPosition != NO_POSITION) {
            getItem(oldPosition).setSelected(false);
            notifyItemChanged(oldPosition);
        }
        getItem(position).setSelected(true);
        notifyItemChanged(position);
        this.selectedItemPosition = position;
    }

    @Nullable
    public Configuration getSelectedItem() {
        return selectedItemPosition == NO_POSITION ? null : getItem(selectedItemPosition);
    }

    public abstract class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {

        public BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
        }
    }

    public class EmptyConfigurationViewHolder extends BaseViewHolder {
        private final TextView titleTextView;
        private final TextView subtitleTextView;
        private final ImageView alertIconImageView;

        public EmptyConfigurationViewHolder(@NonNull final View itemView) {
            super(itemView);

            this.titleTextView = (TextView) itemView.findViewById(R.id.item_row_no_configuration_title);
            this.subtitleTextView = (TextView) itemView.findViewById(R.id.item_row_no_configuration_subtitle);
            this.alertIconImageView = (ImageView) itemView.findViewById(R.id.item_row_no_configuration_alert_icon);

        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final Configuration.Empty empty = (Configuration.Empty) getItem(position);
            if(empty != null) {
                this.titleTextView.setText(empty.title);
                this.subtitleTextView.setText(empty.subtitle);
                this.alertIconImageView.setImageResource(empty.iconRes);
            }
        }
    }

    public class ConfigurationViewHolder extends BaseViewHolder {
        private final TextView configNameTextView;
        private final CheckBox checkBox;

        public ConfigurationViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.configNameTextView = (TextView) itemView.findViewById(R.id.item_row_configuration_name);
            this.checkBox = (CheckBox) itemView.findViewById(R.id.item_row_configuration_checkbox);
            this.checkBox.setOnClickListener(this);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final Configuration config = getItem(position);
            if(config != null) {
                this.configNameTextView.setText(config.getName());
                this.checkBox.setChecked(config.isSelected());
            }

        }
    }
}
