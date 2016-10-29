package is.hello.sense.flows.expansions.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.flows.expansions.ui.widget.ExpansionRangePicker;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Views;

@SuppressLint("ViewConstructor")
public class ExpansionDetailView extends PresenterView {

    final ViewGroup expansionInfoContainer;
    final TextView deviceNameTextView;
    final TextView serviceNameTextView;
    final ImageView expansionIconImageView;
    final TextView expansionDescriptionTextView;
    final Button connectButton;

    final TextView enabledTextView;
    final CompoundButton enabledSwitch;

    final TextView configurationTypeTextView;
    final TextView configurationSelectedTextView;
    final ImageView configurationErrorImageView;
    final ViewGroup removeAccessContainer;
    final ViewGroup connectedContainer;
    final ViewGroup enabledContainer;

    final ProgressBar configurationLoading;

    final ExpansionRangePicker expansionValuePickerView;

    public ExpansionDetailView(@NonNull final Activity activity,
                               @NonNull final OnClickListener enabledTextViewClickListener,
                               @NonNull final OnClickListener removeAccessTextViewClickListener) {
        super(activity);
        this.expansionInfoContainer = (ViewGroup) findViewById(R.id.view_expansion_detail_info_container);
        this.deviceNameTextView = (TextView) expansionInfoContainer.findViewById(R.id.view_expansion_detail_device_name);
        this.serviceNameTextView = (TextView) expansionInfoContainer.findViewById(R.id.view_expansion_detail_device_service_name);
        this.expansionIconImageView = (ImageView) expansionInfoContainer.findViewById(R.id.view_expansion_detail_icon);
        this.expansionDescriptionTextView = (TextView) expansionInfoContainer.findViewById(R.id.view_expansion_detail_description);

        // not connected
        this.connectButton = (Button) findViewById(R.id.view_expansion_detail_connect_button);
        // connected
        this.connectedContainer = (ViewGroup) findViewById(R.id.view_expansion_detail_connected_container);
        this.enabledContainer = (ViewGroup) connectedContainer.findViewById(R.id.view_expansion_detail_enabled_container);
        this.enabledTextView = (TextView) enabledContainer.findViewById(R.id.view_expansion_detail_enabled_tv);
        this.enabledSwitch = (CompoundButton) enabledContainer.findViewById(R.id.view_expansion_detail_configuration_selection_switch);
        // connected and configurations found
        this.configurationErrorImageView = (ImageView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_error);
        this.configurationTypeTextView = (TextView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_type_tv);
        this.configurationSelectedTextView = (TextView) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_selection_tv);
        this.removeAccessContainer = (ViewGroup) connectedContainer.findViewById(R.id.view_expansion_detail_remove_access_container);
        this.configurationLoading = (ProgressBar) connectedContainer.findViewById(R.id.view_expansion_detail_configuration_loading);

        this.expansionValuePickerView = (ExpansionRangePicker) findViewById(R.id.view_expansion_detail_range_picker);
        //hook up listeners
        Views.setSafeOnClickListener(this.enabledTextView, enabledTextViewClickListener);
        Views.setSafeOnClickListener(this.removeAccessContainer, removeAccessTextViewClickListener);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_expansion_detail;
    }

    @Override
    public void releaseViews() {
        this.connectButton.setOnClickListener(null);
        this.removeAccessContainer.setOnClickListener(null);
        this.configurationSelectedTextView.setOnClickListener(null);
        this.enabledSwitch.setOnClickListener(null);
        this.enabledTextView.setOnClickListener(null);
    }

    public void showConfigurationSuccess(@Nullable final String configurationName,
                                         @NonNull final OnClickListener configurationSelectedTextViewClickListener) {
        Views.setSafeOnClickListener(this.configurationSelectedTextView, configurationSelectedTextViewClickListener);
        this.configurationLoading.setVisibility(GONE);
        this.configurationSelectedTextView.setText(configurationName);
        this.configurationSelectedTextView.setVisibility(VISIBLE);
        this.connectedContainer.setVisibility(VISIBLE);
        this.removeAccessContainer.setEnabled(true);
    }

    public void showConfigurationsError(@NonNull final OnClickListener configurationErrorImageViewClickListener) {
        Views.setSafeOnClickListener(this.configurationErrorImageView, configurationErrorImageViewClickListener);
        this.configurationLoading.setVisibility(GONE);
        this.configurationSelectedTextView.setVisibility(GONE);
        this.configurationErrorImageView.setVisibility(VISIBLE);
        this.connectedContainer.setVisibility(VISIBLE);
        this.removeAccessContainer.setEnabled(true);
    }

    public void showConfigurationSpinner() {
        this.configurationSelectedTextView.setVisibility(GONE);
        this.configurationErrorImageView.setVisibility(GONE);
        this.configurationLoading.setVisibility(VISIBLE);
    }

    public void showConnectButton(@NonNull final OnClickListener connectButtonClickListener) {
        this.connectButton.setVisibility(VISIBLE);
        Views.setSafeOnClickListener(this.connectButton, connectButtonClickListener);
    }


    public void showExpansionInfo(@NonNull final Expansion expansion,
                                  @NonNull final Picasso picasso) {
        this.expansionInfoContainer.setVisibility(VISIBLE);
        this.deviceNameTextView.setText(expansion.getDeviceName());
        this.serviceNameTextView.setText(expansion.getServiceName());
        picasso.load(expansion.getIcon().getUrl(getResources()))
               .into(expansionIconImageView);
        this.expansionDescriptionTextView.setText(expansion.getDescription());
        this.configurationTypeTextView.setText(expansion.getConfigurationType());
    }

    public void showExpansionRangePicker(@NonNull final Expansion expansion,
                                         @NonNull final int[] initialValues,
                                         @NonNull final String suffix){
        post( () -> {
            this.expansionValuePickerView.setVisibility(VISIBLE);
            this.expansionValuePickerView.initPickers(expansion.getValueRange(),
                                                      suffix,
                                                      initialValues);

            this.configurationTypeTextView.setText(expansion.getConfigurationType());
        });
    }


    //region switch

    /**
     * Call to revert the switch after failing to update its state on the server.
     *
     * @param enabledSwitchClickListener we need to remove its callback while changing its checked
     *                                   value and then add it back.
     */
    public void showUpdateSwitchError(@NonNull final CompoundButton.OnCheckedChangeListener enabledSwitchClickListener) {
        setEnableSwitch(!enabledSwitch.isChecked(), enabledSwitchClickListener);
    }

    /**
     * Call to re-enable the switch after successfully updating its state on the server.
     */
    public void showUpdateSwitchSuccess() {
        this.enabledSwitch.setEnabled(true);
    }

    /**
     * Call once for expansions that need to display an on/off switch.
     *
     * @param isOn                       starting value of switch.
     * @param enabledSwitchClickListener callback when switch is pressed.
     */
    public void showEnableSwitch(final boolean isOn,
                                 @NonNull final CompoundButton.OnCheckedChangeListener enabledSwitchClickListener) {
        this.connectedContainer.setVisibility(VISIBLE);
        this.enabledContainer.setVisibility(VISIBLE);
        this.setEnableSwitch(isOn, enabledSwitchClickListener);
    }

    public void showRemoveAccess(final boolean isOn){
        this.removeAccessContainer.setVisibility(isOn ? VISIBLE : GONE);
        this.removeAccessContainer.setEnabled(isOn);
    }

    private void setEnableSwitch(final boolean isOn,
                                 @NonNull final CompoundButton.OnCheckedChangeListener enabledSwitchClickListener) {
        this.enabledSwitch.setOnCheckedChangeListener(null);
        this.enabledSwitch.setChecked(isOn);
        this.enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonView.setEnabled(false);
            enabledSwitchClickListener.onCheckedChanged(buttonView, isChecked);
        });
        this.enabledSwitch.setEnabled(true);
    }

    public int getSelectedMin() {
        return this.expansionValuePickerView.getSelectedMinValue();
    }

    public int getSelectedMax() {
        return this.expansionValuePickerView.getSelectedMaxValue();
    }

    //endregion


}
