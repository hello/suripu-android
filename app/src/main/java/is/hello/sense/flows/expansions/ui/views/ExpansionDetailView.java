package is.hello.sense.flows.expansions.ui.views;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Views;

public class ExpansionDetailView extends PresenterView {

    final TextView deviceNameTextView;
    final TextView serviceNameTextView;
    final ImageView expansionIconImageView;
    final TextView expansionDescriptionTextView;
    final Button connectButton;

    final TextView enabledTextView;
    final Switch enabledSwitch;

    final TextView configurationTypeTextView;
    final TextView configurationSelectedTextView;
    final TextView removeAccessTextView;
    final ViewGroup connectedContainer;
    final ViewGroup enabledContainer;

    public ExpansionDetailView(@NonNull final Activity activity) {
        super(activity);
        this.deviceNameTextView = (TextView) findViewById(R.id.view_expansion_detail_device_name);
        this.serviceNameTextView = (TextView) findViewById(R.id.view_expansion_detail_device_service_name);
        this.expansionIconImageView = (ImageView) findViewById(R.id.view_expansion_detail_icon);
        this.expansionDescriptionTextView = (TextView) findViewById(R.id.view_expansion_detail_description);
        //todo show based on expansion state
        // not connected
        this.connectButton = (Button) findViewById(R.id.view_expansion_detail_connect_button);
        // connected
        this.connectedContainer = (ViewGroup) findViewById(R.id.view_expansion_detail_connected_container);
        this.enabledContainer = (ViewGroup) connectedContainer.findViewById(R.id.view_expansion_detail_enabled_container);
        this.enabledTextView = (TextView) enabledContainer.findViewById(R.id.view_expansion_detail_enabled_tv);
        this.enabledSwitch = (Switch) enabledContainer.findViewById(R.id.view_expansion_detail_configuration_selection_switch);
        // connected and configurations found
        this.configurationTypeTextView = (TextView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_type_tv);
        this.configurationSelectedTextView = (TextView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_selection_tv);
        this.removeAccessTextView = (TextView) connectedContainer.findViewById(R.id.view_expansion_detail_remove_access_tv);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_expansion_detail;
    }

    @Override
    public void releaseViews() {
        this.connectButton.setOnClickListener(null);
        this.removeAccessTextView.setOnClickListener(null);
        this.configurationSelectedTextView.setOnClickListener(null);
        this.enabledSwitch.setOnClickListener(null);
        this.enabledTextView.setOnClickListener(null);
    }

    public void loadExpansionIcon(@NonNull final Picasso picasso,
                                  @NonNull final String url){
        picasso.load(url)
               .into(expansionIconImageView);
    }

    public void setConnectButtonClickListener(@NonNull final OnClickListener listener){
        Views.setSafeOnClickListener(this.connectButton, listener);
    }

    public void setRemoveAccessClickListener(@NonNull final OnClickListener listener){
        Views.setSafeOnClickListener(this.removeAccessTextView, listener);
    }

    public void setConfigurationSelectionClickListener(@NonNull final OnClickListener listener){
        Views.setSafeOnClickListener(this.configurationSelectedTextView, listener);
    }

    public void setEnabledSwitchClickListener(@NonNull final CompoundButton.OnCheckedChangeListener listener){
        this.enabledSwitch.setOnCheckedChangeListener(listener); //todo make safe listener
    }

    public void setEnabledIconClickListener(@NonNull final OnClickListener listener){
        Views.setSafeOnClickListener(this.enabledTextView, listener);
    }

    public void enableConfigurations(@Nullable final String configurationName,
                                     @NonNull final OnClickListener configSelectionListener){
        this.configurationSelectedTextView.setText(configurationName);
        setConfigurationSelectionClickListener(configSelectionListener);
        this.connectedContainer.setVisibility(VISIBLE);
    }

    public void enableSwitch(final boolean isOn,
                             @NonNull final CompoundButton.OnCheckedChangeListener onEnableSwitchChanged,
                             @NonNull final OnClickListener onEnableIconClicked) {
        this.enabledContainer.setVisibility(VISIBLE);
        this.enabledSwitch.setChecked(isOn);
        setEnabledSwitchClickListener(onEnableSwitchChanged);
        setEnabledIconClickListener(onEnableIconClicked);
        this.connectedContainer.setVisibility(VISIBLE);
    }

    public void setExpansionInfo(@NonNull final Expansion expansion,
                                 @NonNull final Picasso picasso) {
        this.deviceNameTextView.setText(expansion.getDeviceName());
        this.serviceNameTextView.setText(expansion.getServiceName());
        loadExpansionIcon(picasso, expansion.getIcon()
                                            .getUrl(getResources()));
        this.expansionDescriptionTextView.setText(expansion.getDescription());
        this.configurationTypeTextView.setText(expansion.getConfigurationType());
    }

    public void enableConnectButton(@NonNull final OnClickListener onButtonClicked) {
        setConnectButtonClickListener(onButtonClicked);
        this.connectButton.setVisibility(VISIBLE);
    }
}
