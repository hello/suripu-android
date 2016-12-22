package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.ExpansionAlarm;

public class ExpansionAlarmsAdapter extends ArrayRecyclerAdapter<ExpansionAlarm, ExpansionAlarmsAdapter.ExpansionAlarmViewHolder> {

    private boolean wantsAttributionStyle = false;
    private int lastClickedItemIndex = RecyclerView.NO_POSITION;

    public ExpansionAlarmsAdapter(@NonNull final List<ExpansionAlarm> storage) {
        super(storage);
    }

    @Override
    public ExpansionAlarmViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final int layoutRes = wantsAttributionStyle ? R.layout.item_row_expansion_alarm_attribution : R.layout.item_row_expansion_alarm_detail;
        final View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ExpansionAlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ExpansionAlarmViewHolder holder, final int position) {
        holder.bind(position);
    }

    public List<ExpansionAlarm> getAllEnabledWithValueRangeCopy() {
        final List<ExpansionAlarm> copy = new ArrayList<>(getItemCount());
        for (int i = 0; i < getItemCount(); i++) {
            final ExpansionAlarm temp = getItem(i);
            if(temp.isEnabled() && temp.hasExpansionRange()) {
                copy.add(temp);
            }
        }
        return copy;
    }

    /**
     * @param attributionStyle if true use the layout for item_row_expansion_alarm_attribution.
     *                         Default is false. Will refresh entire view.
     */
    public void setWantsAttributionStyle(final boolean attributionStyle) {
        this.wantsAttributionStyle = attributionStyle;
        notifyDataSetChanged();
    }

    public void updateLastClickedItem(@NonNull final ExpansionAlarm expansionAlarm) {
        if(lastClickedItemIndex != RecyclerView.NO_POSITION) {
            set(lastClickedItemIndex, expansionAlarm);
        }
    }

    public class ExpansionAlarmViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        private final TextView categoryNameTextView;
        private final TextView valueTextView;
        private final ImageView errorImageView;

        public ExpansionAlarmViewHolder(final View itemView) {
            super(itemView);
            this.categoryNameTextView = (TextView) itemView.findViewById(R.id.item_row_expansion_alarm_category);
            this.valueTextView = (TextView) itemView.findViewById(R.id.item_row_expansion_alarm_value);
            this.errorImageView = (ImageView) itemView.findViewById(R.id.item_row_expansion_alarm_error);
            this.itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(final View ignored) {
            ExpansionAlarmsAdapter.this.lastClickedItemIndex = getAdapterPosition();
            super.onClick(ignored);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final ExpansionAlarm expansionAlarm = getItem(position);
            if(expansionAlarm != null) {
                this.errorImageView.setVisibility(View.GONE);
                this.valueTextView.setVisibility(View.VISIBLE);
                this.categoryNameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                                               ContextCompat.getDrawable(itemView.getContext(),
                                                                                         expansionAlarm.getDisplayIcon()),
                                                               null,
                                                               null,
                                                               null);
                this.categoryNameTextView.setText(expansionAlarm.getCategory().categoryDisplayString);
                this.valueTextView.setText(expansionAlarm.getDisplayValue());
            } else {
                this.valueTextView.setVisibility(View.GONE);
                this.errorImageView.setVisibility(View.VISIBLE);
            }

        }
    }
}
