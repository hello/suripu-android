package is.hello.sense.flows.generic.ui.adapters;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.widget.ImageTextView;
import is.hello.sense.util.Constants;

public class SimpleListAdapter extends ArrayRecyclerAdapter<String, ArrayRecyclerAdapter.ViewHolder> {
    private String searchParameters = Constants.EMPTY_STRING;
    private final ArrayList<String> entireList = new ArrayList<>();
    private String initialSelection = null;
    private int selectedItemPosition = Constants.NONE;
    private Listener listener = null;

    public SimpleListAdapter() {
        super(new ArrayList<>());
    }

    //region ArrayRecyclerAdapter
    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent,
                                         final int viewType) {
        return new SimpleViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row_basic_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ArrayRecyclerAdapter.ViewHolder holder,
                                 final int position) {
        holder.bind(position);
    }

    @Override
    public boolean addAll(@NonNull final Collection<? extends String> collection) {
        this.entireList.clear();
        this.entireList.addAll(collection);
        applySearchParams();
        return true;
    }
    //endregion

    //region methods

    public void setListener(@Nullable final Listener listener) {
        this.listener = listener;
    }

    public void setInitialSelection(@Nullable final String initialSelection) {
        this.initialSelection = initialSelection;
        notifyDataSetChanged();
    }

    public void setSearchParameters(@NonNull final String searchParameters) {
        this.searchParameters = searchParameters;
        applySearchParams();
    }

    public void applySearchParams() {
        clear();
        if (searchParameters.isEmpty()) {
            super.addAll(this.entireList);
            notifyDataSetChanged();
            return;
        }
        final ArrayList<String> tempList = new ArrayList<>();
        for (final String item : this.entireList) {
            if (item.toLowerCase().contains(searchParameters.toLowerCase())) {
                tempList.add(item);
            }
        }
        super.addAll(tempList);
        notifyDataSetChanged();
    }

    private void onSelected(@NonNull final String item) {
        if (listener == null) {
            return;
        }
        listener.onSelected(item);
    }
    //endregion

    private class SimpleViewHolder extends ViewHolder {
        private final ImageTextView imageTextView;

        private SimpleViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.imageTextView = (ImageTextView) itemView.findViewById(R.id.item_row_basic_list_item_text);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final String item = getItem(position);
            this.imageTextView.setText(item);
            if (item.equals(initialSelection)) {
                SimpleListAdapter.this.selectedItemPosition = position;
                this.imageTextView.setImageResource(R.drawable.radio_on);
            } else {
                this.imageTextView.setImageResource(R.drawable.radio_off);
            }
            this.itemView.setOnClickListener(v -> {
                initialSelection = item;
                notifyItemChanged(selectedItemPosition);
                notifyItemChanged(position);
                onSelected(item);
            });
        }

    }

    public interface Listener {
        void onSelected(@NonNull String selection);
    }
}
