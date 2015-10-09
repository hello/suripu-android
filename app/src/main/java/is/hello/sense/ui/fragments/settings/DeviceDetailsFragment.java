package is.hello.sense.ui.fragments.settings;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public abstract class DeviceDetailsFragment extends InjectionFragment {
    public static final int RESULT_REPLACED_DEVICE = 0x66;

    public static final String ARG_DEVICE = SenseDetailsFragment.class.getName() + ".ARG_DEVICE";

    private LinearLayout alertContainer;
    private ImageView alertIcon;
    private ProgressBar alertBusy;
    private TextView alertText;
    private Button primaryAlertAction;
    private Button secondaryAlertAction;

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

        this.alertContainer = (LinearLayout) view.findViewById(R.id.fragment_device_details_alert);
        this.alertIcon = (ImageView) alertContainer.findViewById(R.id.fragment_device_details_alert_icon);
        this.alertBusy = (ProgressBar) alertContainer.findViewById(R.id.fragment_device_details_alert_busy);
        this.alertText = (TextView) alertContainer.findViewById(R.id.fragment_device_details_alert_text);
        this.primaryAlertAction = (Button) alertContainer.findViewById(R.id.fragment_device_details_alert_action);
        this.secondaryAlertAction = (Button) alertContainer.findViewById(R.id.fragment_device_details_alert_action_secondary);

        this.actionsContainer = (LinearLayout) view.findViewById(R.id.fragment_device_details_actions);

        TextView footer = (TextView) view.findViewById(R.id.item_device_support_footer);
        Styles.initializeSupportFooter(getActivity(), footer);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.alertContainer = null;
        this.alertIcon = null;
        this.alertBusy = null;
        this.alertText = null;
        this.primaryAlertAction = null;

        this.actionsContainer = null;
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

    protected TextView addDeviceAction(@DrawableRes int iconRes,
                                       @StringRes int titleRes,
                                       @NonNull Runnable onClick) {
        final Context context = getActivity();
        final Resources resources = context.getResources();

        final TextView itemView = new TextView(context);
        itemView.setBackgroundResource(R.drawable.selectable_dark_bounded);
        itemView.setTextAppearance(context, R.style.AppTheme_Text_Body);
        itemView.setTextColor(resources.getColorStateList(R.color.text_color_selector_dark));
        itemView.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0);
        itemView.setText(titleRes);

        final int itemTextHorizontalPadding = resources.getDimensionPixelSize(R.dimen.gap_outer);
        final int itemTextVerticalPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);
        itemView.setPadding(itemTextHorizontalPadding, itemTextVerticalPadding,
                            itemTextHorizontalPadding, itemTextVerticalPadding);
        itemView.setCompoundDrawablePadding(itemTextHorizontalPadding);

        Views.setSafeOnClickListener(itemView, ignored -> onClick.run());

        actionsContainer.addView(itemView);

        return itemView;
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
        primaryAlertAction.setVisibility(View.GONE);
        secondaryAlertAction.setVisibility(View.GONE);

        alertText.setGravity(Gravity.CENTER);
        alertText.setTextAppearance(getActivity(), R.style.AppTheme_Text_Body_New);
        alertText.setText(messageRes);

        alertContainer.setVisibility(View.VISIBLE);
        clearActions();
    }

    protected void showTroubleshootingAlert(@NonNull TroubleshootingAlert alert) {
        alertIcon.setVisibility(View.VISIBLE);
        alertBusy.setVisibility(View.GONE);
        primaryAlertAction.setVisibility(View.VISIBLE);

        alertText.setGravity(Gravity.TOP | Gravity.START);
        alertText.setTextAppearance(getActivity(), R.style.AppTheme_Text_Body_MidSized);
        alertText.setText(alert.message.resolve(getActivity()));

        if (alert.primaryButtonTitle != 0 && alert.primaryButtonOnClick != null) {
            primaryAlertAction.setText(alert.primaryButtonTitle);
            primaryAlertAction.setOnClickListener(ignored -> alert.primaryButtonOnClick.run());
            primaryAlertAction.setVisibility(View.VISIBLE);
        } else {
            primaryAlertAction.setVisibility(View.GONE);
        }

        if (alert.secondaryButtonTitle != 0 && alert.secondaryButtonOnClick != null) {
            secondaryAlertAction.setText(alert.secondaryButtonTitle);
            secondaryAlertAction.setOnClickListener(ignored -> alert.secondaryButtonOnClick.run());
            secondaryAlertAction.setVisibility(View.VISIBLE);
        } else {
            secondaryAlertAction.setVisibility(View.GONE);
        }

        alertContainer.setVisibility(View.VISIBLE);
    }

    protected class TroubleshootingAlert {
        StringRef message;
        @StringRes int primaryButtonTitle;
        Runnable primaryButtonOnClick;
        @StringRes int secondaryButtonTitle;
        Runnable secondaryButtonOnClick;

        public TroubleshootingAlert setMessage(@NonNull StringRef message) {
            this.message = message;
            return this;
        }

        public TroubleshootingAlert setPrimaryButtonTitle(@StringRes int primaryButtonTitle) {
            this.primaryButtonTitle = primaryButtonTitle;
            return this;
        }

        public TroubleshootingAlert setPrimaryButtonOnClick(@NonNull Runnable primaryButtonOnClick) {
            this.primaryButtonOnClick = primaryButtonOnClick;
            return this;
        }

        public TroubleshootingAlert setSecondaryButtonTitle(@StringRes int secondaryButtonTitle) {
            this.secondaryButtonTitle = secondaryButtonTitle;
            return this;
        }

        public TroubleshootingAlert setSecondaryButtonOnClick(@NonNull Runnable secondaryButtonOnClick) {
            this.secondaryButtonOnClick = secondaryButtonOnClick;
            return this;
        }
    }

    //endregion
}
