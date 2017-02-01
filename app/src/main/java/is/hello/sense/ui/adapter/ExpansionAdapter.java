package is.hello.sense.ui.adapter;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.databinding.ItemRowExpansionBinding;
import is.hello.sense.ui.widget.util.Styles;

public class ExpansionAdapter extends ArrayRecyclerAdapter<Expansion, ExpansionAdapter.ExpansionViewHolder> {

    private final Picasso picasso;

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
    public void onBindViewHolder(final ExpansionViewHolder holder,
                                 final int position) {
        holder.bind(position);
    }

    public class ExpansionViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        private final ItemRowExpansionBinding binding;

        public ExpansionViewHolder(final View itemView) {
            super(itemView);
            this.binding = DataBindingUtil.bind(itemView);
            this.itemView.setOnClickListener(this);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final Expansion expansion = getItem(position);
            if (expansion != null) {
                setEnabled(expansion.isAvailable());
                this.binding.itemIconIv.setExpansion(picasso, expansion);
                this.binding.itemDeviceNameTv.setText(expansion.getDeviceName());
                this.binding.itemStateTv.setText(expansion.getState().displayValue);
            }

        }

        public void setEnabled(final boolean enabled) {
            itemView.setEnabled(enabled);
            this.binding.itemDeviceNameTv.setEnabled(enabled);
            this.binding.itemStateTv.setEnabled(enabled);
            this.binding.itemIconIv.setAlpha(Styles.getViewAlpha(enabled));
        }
    }
}
