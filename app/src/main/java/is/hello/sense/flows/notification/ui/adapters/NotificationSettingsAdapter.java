package is.hello.sense.flows.notification.ui.adapters;


import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.NotificationSetting;
import is.hello.sense.databinding.ItemNotificationSettingsBinding;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.widget.util.Views;

public class NotificationSettingsAdapter extends ArrayRecyclerAdapter<NotificationSetting, ArrayRecyclerAdapter.ViewHolder> {
    private static final int ERROR = 0;
    private static final int HEADER = 1;
    private static final int SETTING = 2;

    private boolean showHeader = false;
    private boolean hasError = false;

    public NotificationSettingsAdapter() {
        super(new ArrayList<>());
    }

    @Override
    public int getItemViewType(final int position) {
        if (showHeader) {
            return position == 0 ? HEADER : hasError ? ERROR : SETTING;
        }
        if (hasError) {
            return ERROR;
        }
        return SETTING;
    }

    @Override
    public ArrayRecyclerAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent,
                                                              final int viewType) {
        if (viewType == ERROR) {
            return new ErrorViewHolder(parent);
        } else if (viewType == HEADER) {
            return new HeaderViewHolder(inflate(R.layout.item_enable_notifications, parent));
        }

        return new SettingsViewHolder(DataBindingUtil.bind(inflate(R.layout.item_notification_settings, parent)));
    }

    @Override
    public void onBindViewHolder(final ArrayRecyclerAdapter.ViewHolder holder,
                                 final int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        final int offset = showHeader ? 1 : 0;
        if (hasError) {

            return 1 + offset;
        }
        return super.getItemCount() + offset;
    }

    @Override
    public NotificationSetting getItem(final int position) {
        return super.getItem(showHeader ? position - 1 : position);
    }

    public void bindSettings(@NonNull final List<NotificationSetting> settings) {
        clear();
        this.hasError = false;
        addAll(settings);
        notifyDataSetChanged();
    }


    public void showNotificationHeader(final boolean showHeader) {
        this.showHeader = showHeader;
        notifyDataSetChanged();
    }


    public void setHasError(final boolean hasError) {
        this.hasError = hasError;
        notifyDataSetChanged();
    }

    private class HeaderViewHolder extends ArrayRecyclerAdapter.ViewHolder {

        public HeaderViewHolder(@NonNull final View itemView) {
            super(itemView);

        }
    }

    private class SettingsViewHolder extends ArrayRecyclerAdapter.ViewHolder {
        private final ItemNotificationSettingsBinding binding;

        public SettingsViewHolder(@NonNull final ItemNotificationSettingsBinding itemView) {
            super(itemView.getRoot());
            this.binding = itemView;
        }

        @SuppressWarnings("RedundantCast")
        @Override
        public void bind(final int position) {
            final NotificationSetting setting = getItem(position);
            if (setting == null) {
                return;
            }
            this.binding.itemNotificationSettingsText.setText(setting.getName());
            ((CompoundButton) this.binding.itemNotificationSettingsSwitch.widgetSwitch).setChecked(setting.isEnabled());
            Views.setSafeOnSwitchClickListener(((CompoundButton) this.binding.itemNotificationSettingsSwitch.widgetSwitch), (buttonView, isChecked) -> setting.setEnabled(isChecked));
        }
    }

    private class ErrorViewHolder extends ArrayRecyclerAdapter.ErrorViewHolder {

        public ErrorViewHolder(@NonNull final ViewGroup parent) {
            super(parent);
        }
    }

}
