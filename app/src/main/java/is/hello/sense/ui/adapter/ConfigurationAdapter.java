package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Configuration;

public class ConfigurationAdapter extends ArrayRecyclerAdapter<Configuration, ArrayRecyclerAdapter.ViewHolder> {

    public static final int TYPE_CONFIG = 0;
    public static final int TYPE_NO_CONFIG = 1;

    public ConfigurationAdapter(@NonNull final List<Configuration> storage) {
        super(storage);
    }

    @Override
    public int getItemViewType(final int position) {
        return getItem(position) instanceof Configuration.Empty ? TYPE_NO_CONFIG : TYPE_CONFIG;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case TYPE_CONFIG:
                final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row_configuration, parent, false);
                return new ConfigurationViewHolder(view);
            case TYPE_NO_CONFIG:
            default:
                final View view2 = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row_no_configuration, parent, false);
                return new NoConfigurationViewHolder(view2);
        }
    }

    @Override
    public void onBindViewHolder(final ArrayRecyclerAdapter.ViewHolder holder, final int position) {
        holder.bind(position);
    }

    public void setSelectedItem(final int position) {
        final int size = getItemCount();
        for(int i=0; i< size; i++) {
            getItem(i).setSelected(i == position);
        }
        notifyDataSetChanged();
    }

    public class NoConfigurationViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        private final TextView titleTextView;
        private final TextView subtitleTextView;
        private final ImageView alertIconImageView;

        public NoConfigurationViewHolder(@NonNull final View itemView) {
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

    public class ConfigurationViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        private final TextView configNameTextView;
        private final CheckBox checkBox;

        public ConfigurationViewHolder(final View itemView) {
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
