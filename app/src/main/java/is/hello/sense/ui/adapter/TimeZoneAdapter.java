package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import is.hello.sense.R;

public class TimeZoneAdapter extends RecyclerView.Adapter<TimeZoneAdapter.BaseViewHolder> {
    private final static int VIEW_TYPE_HEADER = 0;
    private final static int VIEW_TYPE_TIME_ZONE = 1;
    private final static ViewGroup.LayoutParams LAYOUT_PARAMS =
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                       ViewGroup.LayoutParams.WRAP_CONTENT);

    private final Context context;

    private final String[] timeZoneNames;
    private final OnClickListener clickListener;

    private final int itemHorizontalPadding;
    private final int itemVerticalPadding;
    private final int drawablePadding;
    private final int primaryTextColor;
    private final int secondaryTextColor;

    public TimeZoneAdapter(@NonNull final Context context,
                           @NonNull final OnClickListener clickListener) {
        this.context = context;

        this.primaryTextColor = ContextCompat.getColor(context, R.color.primary_text_selector);
        this.secondaryTextColor = ContextCompat.getColor(context, R.color.secondary_text_selector);
        final Resources resources = context.getResources();

        final String[] timeZoneArray = resources.getStringArray(R.array.timezone_names);
        this.timeZoneNames = new String[timeZoneArray.length + 1];
        this.timeZoneNames[0] = context.getString(R.string.label_choose_time_zone);
        System.arraycopy(timeZoneArray, 0, this.timeZoneNames, 1, timeZoneArray.length);

        this.itemHorizontalPadding = resources.getDimensionPixelSize(R.dimen.x3);
        this.itemVerticalPadding = resources.getDimensionPixelSize(R.dimen.x2);
        this.drawablePadding = resources.getDimensionPixelSize(R.dimen.x3);

        this.clickListener = clickListener;
    }

    protected String getDisplayName(final int position) {
        return timeZoneNames[position];
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent,
                                             final int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            final TextView headerText = new TextView(context, null,
                                                     R.style.Body1_Secondary);
            headerText.setTextColor(secondaryTextColor);
            return new HeaderViewHolder(headerText);
        } else {
            final TextView timeZoneText = new TextView(context, null, R.style.Body1_Primary);
            timeZoneText.setTextColor(primaryTextColor);
            timeZoneText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_off, 0, 0, 0);
            timeZoneText.setCompoundDrawablePadding(drawablePadding);
            timeZoneText.setBackgroundResource(R.drawable.selectable_dark_bounded);
            return new TimeZoneViewHolder(timeZoneText);
        }
    }


    @Override
    public int getItemViewType(final int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_TIME_ZONE;
        }
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

            itemView.setLayoutParams(LAYOUT_PARAMS);
        }

        abstract void bind(int position);
    }

    class HeaderViewHolder extends BaseViewHolder {
        final TextView header;

        HeaderViewHolder(@NonNull final TextView view) {
            super(view);

            this.header = view;
            header.setPadding(itemHorizontalPadding, itemVerticalPadding * 2,
                              itemHorizontalPadding, itemVerticalPadding);
        }

        void bind(int position) {
            header.setText(getDisplayName(position));
        }
    }

    class TimeZoneViewHolder extends BaseViewHolder {
        final TextView title;

        TimeZoneViewHolder(@NonNull final TextView view) {
            super(view);

            this.title = view;
            title.setPadding(itemHorizontalPadding, itemVerticalPadding,
                             itemHorizontalPadding, itemVerticalPadding);
        }

        void bind(final int position) {
            title.setText(getDisplayName(position));
            title.setOnClickListener((view) -> {
                title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_on, 0, 0, 0);
                clickListener.onTimeZoneItemClicked(position - 1); // This list has 1 more element (the header) than the list of time zone ids.
            });
        }
    }

    public interface OnClickListener {
        void onTimeZoneItemClicked(int position);
    }

}
