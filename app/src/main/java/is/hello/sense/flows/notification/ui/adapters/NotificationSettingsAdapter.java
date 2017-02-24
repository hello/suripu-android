package is.hello.sense.flows.notification.ui.adapters;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.NotificationSetting;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.widget.util.Views;

public class NotificationSettingsAdapter extends ArrayRecyclerAdapter<NotificationSetting, ArrayRecyclerAdapter.ViewHolder> {
    private static final int ERROR = 0;
    private static final int HEADER = 1;
    private static final int SETTING = 2;

    private boolean showHeader = false;
    private boolean hasError = false;
    private Listener listener = null;

    public NotificationSettingsAdapter() {
        super(new ArrayList<>(2));
    }

    //region ArrayRecyclerAdapter
    @Override
    public int getItemViewType(final int position) {
        if (showHeader && position == 0) {
            return HEADER;
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
            return new NoConnectionErrorViewHolder(parent);
        } else if (viewType == HEADER) {
            return new HeaderViewHolder(parent);
        } else if (viewType == SETTING) {
            return new SettingsViewHolder(inflate(R.layout.item_notification_settings, parent));
        }
        throw new IllegalStateException("unknown type");
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

    //endregion

    //region methods
    public void bindSettings(@NonNull final List<NotificationSetting> settings) {
        this.hasError = false;
        replaceAll(settings);
    }


    public void showNotificationHeader(final boolean showHeader) {
        this.showHeader = showHeader;
        notifyDataSetChanged();
    }


    public void setHasError(final boolean hasError) {
        this.hasError = hasError;
        notifyDataSetChanged();
    }

    private void showSettings(final View ignored) {
        if (this.listener != null) {
            this.listener.showSettings();
        }
    }

    public void setListener(@Nullable final Listener listener) {
        this.listener = listener;
    }
    //endregion

    //region ViewHolders
    private class HeaderViewHolder extends ItemMessageCardViewHolder {
        public HeaderViewHolder(@NonNull final ViewGroup parent) {
            super(parent,
                  R.drawable.icon_warning,
                  R.string.notification_settings_enabled_title,
                  R.string.notification_settings_enabled_body,
                  R.string.notification_settings_enabled_link,
                  NotificationSettingsAdapter.this::showSettings);
        }
    }

    private class SettingsViewHolder extends ViewHolder {
        private final TextView name;
        private final CompoundButton toggleButton;

        public SettingsViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.name = (TextView) itemView.findViewById(R.id.item_notification_settings_text);
            this.toggleButton = (CompoundButton) itemView.findViewById(R.id.item_notification_settings_switch);
        }

        @SuppressWarnings("RedundantCast")
        @Override
        public void bind(final int position) {
            final NotificationSetting setting = getItem(position);
            if (setting == null) {
                return;
            }
            this.name.setText(setting.getName());
            this.toggleButton.setChecked(setting.isEnabled());
            Views.setSafeOnSwitchClickListener(this.toggleButton, (buttonView, isChecked) -> setting.setEnabled(isChecked));
        }
    }
    //endregion

    public interface Listener {
        void showSettings();
    }

}
