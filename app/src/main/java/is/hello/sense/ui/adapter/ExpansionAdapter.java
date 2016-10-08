package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Expansion;

public class ExpansionAdapter extends ArrayRecyclerAdapter<Expansion, ExpansionAdapter.ExpansionViewHolder> {

    private Picasso picasso;

    public ExpansionAdapter(@NonNull final List<Expansion> storage,
                            @NonNull final Picasso picasso) {
        super(storage);
        this.picasso = picasso;
    }

    @Override
    public ExpansionViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row_expansion, parent, false);
        return new ExpansionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ExpansionViewHolder holder, int position) {
        holder.bind(position);
    }

    public class ExpansionViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        private final ImageView iconImageView;
        private final TextView deviceNameTextView;
        private final TextView stateTextView;

        public ExpansionViewHolder(final View itemView) {
            super(itemView);
            this.iconImageView = (ImageView) itemView.findViewById(R.id.item_icon_iv);
            this.deviceNameTextView = (TextView) itemView.findViewById(R.id.item_device_name_tv);
            this.stateTextView = (TextView) itemView.findViewById(R.id.item_state_tv);
        }

        @Override
        public void bind(int position) {
            super.bind(position);
            final Expansion expansion = getItem(position);
            if(expansion != null) {
                picasso.cancelRequest(iconImageView);
                picasso.load(expansion.getIcon().getUrl(itemView.getResources()))
                       .into(iconImageView);
                this.deviceNameTextView.setText(expansion.getDeviceName());
                this.stateTextView.setText(expansion.getState().displayValue);
            }

        }
    }
}
