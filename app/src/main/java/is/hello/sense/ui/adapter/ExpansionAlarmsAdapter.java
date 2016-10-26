package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
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

    public ExpansionAlarmsAdapter(@NonNull final List<ExpansionAlarm> storage) {
        super(storage);
    }

    @Override
    public ExpansionAlarmViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row_alarm_detail_expansion, parent, false);
        return new ExpansionAlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ExpansionAlarmViewHolder holder, final int position) {
        holder.bind(position);
    }

    public List<ExpansionAlarm> getAllCopy() {
        final List<ExpansionAlarm> copy = new ArrayList<>(getItemCount());
        for (int i = 0; i < getItemCount(); i++) {
            copy.add(getItem(i));
        }
        return copy;
    }

    public class ExpansionAlarmViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        private final TextView categoryNameTextView;
        private final TextView valueTextView;
        private final ImageView errorImageView;

        public ExpansionAlarmViewHolder(final View itemView) {
            super(itemView);
            this.categoryNameTextView = (TextView) itemView.findViewById(R.id.item_row_alarm_detail_expansion_category);
            this.valueTextView = (TextView) itemView.findViewById(R.id.item_row_alarm_detail_expansion_value);
            this.errorImageView = (ImageView) itemView.findViewById(R.id.item_row_alarm_detail_expansion_error);
            this.valueTextView.setOnClickListener(this);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final ExpansionAlarm expansionAlarm = getItem(position);
            if(expansionAlarm != null) {
                this.errorImageView.setVisibility(View.GONE);
                this.valueTextView.setVisibility(View.VISIBLE);
                this.categoryNameTextView.setCompoundDrawables(null, expansionAlarm.getDisplayIcon(), null, null);
                //this.categoryNameTextView.setCompoundDrawablePadding(); //todo see if needed
                this.categoryNameTextView.setText(expansionAlarm.getCategory().categoryDisplayString);
                this.valueTextView.setText(expansionAlarm.getDisplayValue());
            } else {
                this.valueTextView.setVisibility(View.GONE);
                this.errorImageView.setVisibility(View.VISIBLE);
            }

        }
    }
}
