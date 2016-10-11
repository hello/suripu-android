package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Configuration;

public class ConfigurationAdapter extends ArrayRecyclerAdapter<Configuration, ConfigurationAdapter.ConfigurationViewHolder> {

    public ConfigurationAdapter(@NonNull final List<Configuration> storage) {
        super(storage);
    }

    @Override
    public ConfigurationViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row_configuration, parent, false);
        return new ConfigurationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ConfigurationViewHolder holder, final int position) {
        holder.bind(position);
    }

    public void setSelectedItem(final int position) {
        final int size = getItemCount();
        for(int i=0; i< size; i++) {
            getItem(i).setSelected(i == position);
        }
        notifyDataSetChanged();
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
