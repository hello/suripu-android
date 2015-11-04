package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import is.hello.sense.R;

public class TimeZoneAdapter extends RecyclerView.Adapter<TimeZoneAdapter.TimeZoneHolder> {
    private final String[] names;
    private final Context context;
    private final int dp;
    private final OnRadioClickListener clickListener;

    public TimeZoneAdapter(@NonNull Context context, @NonNull OnRadioClickListener clickListener) {
        this.context = context;
        this.names = context.getResources().getStringArray(R.array.timezone_names);
        this.clickListener = clickListener;
        dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.gap_medium), context.getResources().getDisplayMetrics());
    }


    protected String getDisplayName(int position) {
        return names[position];
    }

    @Override
    public TimeZoneHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TimeZoneHolder(new RadioButton(context));
    }


    @Override
    public int getItemCount() {
        return names.length;
    }

    @Override
    public void onBindViewHolder(TimeZoneHolder holder, int position) {
        holder.bind(position);
    }

    class TimeZoneHolder extends RecyclerView.ViewHolder {

        final RadioButton radioButton;

        public TimeZoneHolder(@NonNull View view) {
            super(view);
            this.radioButton = (RadioButton) view;
            this.radioButton.setPadding(0, dp, 0, dp);
        }

        void bind(int position) {
            radioButton.setText(getDisplayName(position));
            radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    clickListener.onRadioValueChanged(position);
                }
            });
        }
    }

    public interface OnRadioClickListener {
        void onRadioValueChanged(int position);
    }
}
