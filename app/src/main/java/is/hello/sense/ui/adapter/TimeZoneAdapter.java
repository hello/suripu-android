package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import is.hello.sense.R;

public class TimeZoneAdapter extends RecyclerView.Adapter<TimeZoneAdapter.BaseViewHolder> {
    private final static int VIEW_TYPE_HEADER = 0;
    private final static int VIEW_TYPE_RADIO = 1;
    private final static ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private final String[] timeZoneNames;
    private final Context context;
    private final int itemVerticalPadding;
    private final int imagePadding;
    private final OnRadioClickListener clickListener;

    public TimeZoneAdapter(@NonNull Context context, @NonNull OnRadioClickListener clickListener) {
        this.context = context;
        final String[] timeZoneArray = context.getResources().getStringArray(R.array.timezone_names);
        this.timeZoneNames = new String[timeZoneArray.length + 1];
        this.timeZoneNames[0] = context.getString(R.string.label_choose_time_zone);
        System.arraycopy(timeZoneArray, 0, this.timeZoneNames, 1, timeZoneArray.length);
        this.clickListener = clickListener;
        this.itemVerticalPadding = context.getResources().getDimensionPixelSize(R.dimen.gap_outer_half);
        this.imagePadding = context.getResources().getDimensionPixelSize(R.dimen.gap_large);


    }

    protected String getDisplayName(int position) {
        return timeZoneNames[position];
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderHolder(new TextView(context, null, R.attr.senseTextAppearanceFieldLabel));
        }
        TextView textView = new TextView(context, null, R.style.AppTheme_Text_Body);
        textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_off, 0, 0, 0);
        textView.setCompoundDrawablePadding(imagePadding);
        textView.setBackgroundResource(R.drawable.selectable_dark_bounded);
        textView.setLayoutParams(layoutParams);
        return new TimeZoneHolder(textView);
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
        }

        void bind(int position) {
            header.setText(getDisplayName(position));
        }
    }

    class TimeZoneHolder extends BaseViewHolder {

        final TextView title;

        public TimeZoneHolder(@NonNull View view) {
            super(view);
            view.setPadding(0, itemVerticalPadding, 0, itemVerticalPadding);
            this.title = (TextView) view;
        }

        void bind(int position) {
            title.setText(getDisplayName(position));
            title.setOnClickListener((view) -> {
                title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_on, 0, 0, 0);
                clickListener.onRadioValueChanged(position - 1); // This list has 1 more element (the header) than the list of time zone ids.
            });
        }

    }

    public interface OnRadioClickListener {
        void onRadioValueChanged(int position);
    }

}
