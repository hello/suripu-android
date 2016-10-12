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
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Views;

public class ExpansionDetailView extends PresenterView {

    final TextView deviceNameTextView;
    final TextView serviceNameTextView;
    final ImageView expansionIconImageView;
    final TextView expansionDescriptionTextView;
    final Button actionButton;

    final TextView enabledTextView;
    final Switch enabledSwitch;

    final TextView configurationTypeTextView;
    final TextView configurationSelectedTextView;
    final ViewGroup removeAccessViewGroup;

    public ExpansionDetailView(@NonNull final Activity activity) {
        super(activity);
        this.deviceNameTextView = (TextView) findViewById(R.id.view_expansion_detail_device_name);
        this.serviceNameTextView = (TextView) findViewById(R.id.view_expansion_detail_device_service_name);
        this.expansionIconImageView = (ImageView) findViewById(R.id.view_expansion_detail_icon);
        this.expansionDescriptionTextView = (TextView) findViewById(R.id.view_expansion_detail_description);
        //todo hide based on expansion state
        // not connected
        this.actionButton = (Button) findViewById(R.id.view_expansion_detail_action_button);
        // connected
        this.enabledTextView = (TextView) findViewById(R.id.view_expansion_detail_enabled_tv);
        this.enabledSwitch = (Switch) findViewById(R.id.view_expansion_detail_configuration_selection_switch);
        // connected and configurations found
        this.configurationTypeTextView = (TextView) findViewById(R.id.view_expansion_detail_configuration_type_tv);
        this.configurationSelectedTextView = (TextView) findViewById(R.id.view_expansion_detail_configuration_selection_tv);
        this.removeAccessViewGroup = (ViewGroup) findViewById(R.id.view_expansion_detail_remove_access_container);

    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_expansion_detail;
    }

    @Override
    public void releaseViews() {
        this.actionButton.setOnClickListener(null);
        this.removeAccessViewGroup.setOnClickListener(null);
        this.configurationSelectedTextView.setOnClickListener(null);
        this.enabledSwitch.setOnClickListener(null);
        this.enabledTextView.setOnClickListener(null);
    }

    public void loadExpansionIcon(@NonNull final Picasso picasso,
                                  @NonNull final String url){
        picasso.load(url)
               .into(expansionIconImageView);
    }

    public void setActionButtonClickListener(@NonNull final OnClickListener listener){
        Views.setSafeOnClickListener(this.actionButton, listener);
    }

    public void setRemoveAccessClickListener(@NonNull final OnClickListener listener){
        Views.setSafeOnClickListener(this.removeAccessViewGroup, listener);
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

    public void setTitle(@Nullable final String deviceName) {
        this.deviceNameTextView.setText(deviceName);
    }

    public void setSubtitle(@Nullable final String serviceName) {
        this.serviceNameTextView.setText(serviceName);
    }

    public void setDescription(@Nullable final String description) {
        this.expansionDescriptionTextView.setText(description);
    }

    public void setConfigurationType(@Nullable final String type) {
        this.configurationTypeTextView.setText(type);
    }
}
