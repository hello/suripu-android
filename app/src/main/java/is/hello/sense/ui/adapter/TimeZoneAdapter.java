package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;


import is.hello.sense.R;

public class TimeZoneAdapter extends RecyclerView.Adapter<TimeZoneAdapter.BaseViewHolder> {
    private final String[] timeZoneNames;
    private final Context context;
    private final int itemVerticalPadding;
    private final OnRadioClickListener clickListener;
    private final int VIEW_TYPE_HEADER = 0;
    private final int VIEW_TYPE_RADIO = 1;

    public TimeZoneAdapter(@NonNull Context context, @NonNull OnRadioClickListener clickListener) {
        this.context = context;
        final String[] timeZoneArray = context.getResources().getStringArray(R.array.timezone_names);
        this.timeZoneNames = new String[timeZoneArray.length + 1];
        this.timeZoneNames[0] = context.getString(R.string.label_choose_time_zone);
        System.arraycopy(timeZoneArray, 0, this.timeZoneNames, 1, timeZoneArray.length);
        this.clickListener = clickListener;
        itemVerticalPadding = context.getResources().getDimensionPixelSize(R.dimen.gap_outer_half);
    }


    protected String getDisplayName(int position) {
        return timeZoneNames[position];
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderHolder(new TextView(context));
        }
        return new TimeZoneHolder(new RadioButton(context));
    }


    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_RADIO;
    }

    @Override
    public int getItemCount() {
        return timeZoneNames.length;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bind(position);
    }

    abstract class BaseViewHolder extends RecyclerView.ViewHolder {
        BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        abstract void bind(int position);
    }

    class HeaderHolder extends BaseViewHolder {

        final TextView header;

        public HeaderHolder(@NonNull View view) {
            super(view);
            this.header = (TextView) view;
            this.header.setPadding(0, itemVerticalPadding * 2, 0, itemVerticalPadding);
            this.header.setTextAppearance(context, R.style.AppTheme_Text_SectionHeading_Large);
        }

        void bind(int position) {
            header.setText(getDisplayName(position));
        }
    }

    class TimeZoneHolder extends BaseViewHolder {

        final RadioButton radioButton;

        public TimeZoneHolder(@NonNull View view) {
            super(view);
            this.radioButton = (RadioButton) view;
            this.radioButton.setPadding(0, itemVerticalPadding, 0, itemVerticalPadding);
        }

        void bind(int position) {
            radioButton.setText(getDisplayName(position));
            radioButton.setOnCheckedChangeListener((button, isChecked) -> {
                clickListener.onRadioValueChanged(position - 1); // This list has 1 more element (the header) than the list of time zone ids.
            });
        }
    }

    public interface OnRadioClickListener {
        void onRadioValueChanged(int position);
    }
}
