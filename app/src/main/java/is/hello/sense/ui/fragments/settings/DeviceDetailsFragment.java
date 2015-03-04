package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.ui.animation.AnimatorConfig;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.widget.util.Styles;

public abstract class DeviceDetailsFragment extends HardwareFragment {
    public static final int RESULT_REPLACED_DEVICE = 0x66;

    public static final String ARG_DEVICE = SenseDetailsFragment.class.getName() + ".ARG_DEVICE";

    private LinearLayout alertContainer;
    private ImageView alertIcon;
    private ProgressBar alertBusy;
    private TextView alertText;
    private Button alertAction;

    private LinearLayout actionsContainer;

    protected Device device;


    //region Lifecycle

    protected static Bundle createArguments(@NonNull Device device) {
        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DEVICE, device);
        return arguments;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.device = (Device) getArguments().getSerializable(ARG_DEVICE);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_details, container, false);

        LinearLayout fragmentContainer = (LinearLayout) view.findViewById(R.id.fragment_device_details_container);
        AnimatorConfig.DEFAULT.apply(fragmentContainer.getLayoutTransition());

        this.alertContainer = (LinearLayout) view.findViewById(R.id.fragment_device_details_alert);
        this.alertIcon = (ImageView) alertContainer.findViewById(R.id.fragment_device_details_alert_icon);
        this.alertBusy = (ProgressBar) alertContainer.findViewById(R.id.fragment_device_details_alert_busy);
        this.alertText = (TextView) alertContainer.findViewById(R.id.fragment_device_details_alert_text);
        this.alertAction = (Button) alertContainer.findViewById(R.id.fragment_device_details_alert_action);

        this.actionsContainer = (LinearLayout) view.findViewById(R.id.fragment_device_details_actions);

        TextView footer = (TextView) view.findViewById(R.id.footer_help);
        Styles.initializeSupportFooter(getActivity(), footer);

        return view;
    }

    protected void finishDeviceReplaced() {
        finishWithResult(RESULT_REPLACED_DEVICE, null);
    }

    //endregion


    //region Actions

    protected void clearActions() {
        actionsContainer.removeViews(0, actionsContainer.getChildCount());
        actionsContainer.setVisibility(View.GONE);
    }

    protected void showActions() {
        actionsContainer.setVisibility(View.VISIBLE);
    }

    protected void addDeviceAction(@StringRes int titleRes, boolean wantsDivider, @NonNull Runnable onClick) {
        View itemView = Styles.createItemView(getActivity(), titleRes, R.style.AppTheme_Text_Actionable, ignored -> onClick.run());
        actionsContainer.addView(itemView);

        if (wantsDivider) {
            View dividerView = Styles.createHorizontalDivider(getActivity(), ViewGroup.LayoutParams.MATCH_PARENT);
            actionsContainer.addView(dividerView);
        }
    }

    protected void showSupportFor(@NonNull UserSupport.DeviceIssue deviceIssue) {
        UserSupport.showForDeviceIssue(getActivity(), deviceIssue);
    }

    //endregion


    //region Displaying Alerts

    protected void hideAlert() {
        alertContainer.setVisibility(View.GONE);
        alertBusy.setVisibility(View.GONE);
    }

    protected void showBlockingAlert(@StringRes int messageRes) {
        alertIcon.setVisibility(View.GONE);
        alertBusy.setVisibility(View.VISIBLE);
        alertAction.setVisibility(View.GONE);

        alertText.setText(messageRes);

        alertContainer.setVisibility(View.VISIBLE);
        clearActions();
    }

    protected void showTroubleshootingAlert(@NonNull String message,
                                            @StringRes int buttonTitleRes,
                                            @NonNull Runnable onClick) {
        alertIcon.setVisibility(View.VISIBLE);
        alertBusy.setVisibility(View.GONE);
        alertAction.setVisibility(View.VISIBLE);

        alertText.setText(message);
        alertAction.setText(buttonTitleRes);
        alertAction.setOnClickListener(ignored -> onClick.run());

        alertContainer.setVisibility(View.VISIBLE);
    }

    protected void showTroubleshootingAlert(@StringRes int messageRes,
                                            @StringRes int buttonTitleRes,
                                            @NonNull Runnable onClick) {
        showTroubleshootingAlert(getString(messageRes), buttonTitleRes, onClick);
    }

    //endregion
}
