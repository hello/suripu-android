package is.hello.sense.ui.fragments.settings;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;

public abstract class DeviceDetailsFragment<TDevice extends BaseDevice> extends InjectionFragment {
    public static final int RESULT_REPLACED_DEVICE = 0x66;

    public static final String ARG_DEVICE = SenseDetailsFragment.class.getName() + ".ARG_DEVICE";

    private LinearLayout alertContainer;
    private ProgressBar alertBusy;
    private TextView alertText;
    private Button primaryAlertAction;
    private Button secondaryAlertAction;
    private ViewGroup alertTitleContainer;
    private TextView alertTitleText;

    private LinearLayout actionsContainer;

    protected TDevice device;


    //region Lifecycle

    protected static Bundle createArguments(@NonNull BaseDevice device) {
        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_DEVICE, device);
        return arguments;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //noinspection unchecked
        this.device = (TDevice) getArguments().getSerializable(ARG_DEVICE);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_details, container, false);

        this.alertContainer = (LinearLayout) view.findViewById(R.id.fragment_device_details_alert);
        this.alertBusy = (ProgressBar) alertContainer.findViewById(R.id.fragment_device_details_alert_busy);
        this.alertText = (TextView) alertContainer.findViewById(R.id.fragment_device_details_alert_text);
        this.alertTitleContainer = (ViewGroup) alertContainer.findViewById(R.id.fragment_device_details_alert_title_container);
        this.alertTitleText = (TextView) alertContainer.findViewById(R.id.fragment_device_details_alert_title_text);
        this.primaryAlertAction = (Button) alertContainer.findViewById(R.id.fragment_device_details_alert_action);
        this.secondaryAlertAction = (Button) alertContainer.findViewById(R.id.fragment_device_details_alert_action_secondary);

        this.actionsContainer = (LinearLayout) view.findViewById(R.id.fragment_device_details_actions);

        TextView footer = (TextView) view.findViewById(R.id.item_device_name);
        Styles.initializeSupportFooter(getActivity(), footer);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.alertContainer = null;
        this.alertBusy = null;
        this.alertText = null;
        this.alertTitleContainer = null;
        this.alertTitleText = null;
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

    protected void clearAction(final int childIndex) {
        actionsContainer.removeViewAt(childIndex);
    }

    protected void showActions() {
        actionsContainer.setVisibility(View.VISIBLE);
    }

    /**
     * todo refactor to use recyclerview and adapter pattern instead in future release
     */
    protected TextView addDeviceAction(@DrawableRes int iconRes,
                                       @StringRes int titleRes,
                                       @NonNull Runnable onClick) {
        final Context context = getActivity();
        final Resources resources = context.getResources();
        final TextView itemView = new TextView(context);
        itemView.setBackgroundResource(R.drawable.selectable_dark_bounded);
        itemView.setTextColor(ContextCompat.getColorStateList(getActivity(), R.color.primary_text_selector));
        itemView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                Styles.tintDrawable(context, iconRes, R.color.active_icon), null, null, null);
        itemView.setText(titleRes);

        final int itemTextHorizontalPadding = resources.getDimensionPixelSize(R.dimen.x3);
        final int itemTextVerticalPadding = resources.getDimensionPixelSize(R.dimen.x2);
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
        if (getView() != null) {
            alertTitleContainer.setVisibility(View.GONE);
            alertBusy.setVisibility(View.VISIBLE);
            primaryAlertAction.setVisibility(View.GONE);
            secondaryAlertAction.setVisibility(View.GONE);

            alertText.setGravity(Gravity.CENTER);
            alertText.setText(messageRes);

            alertContainer.setVisibility(View.VISIBLE);
        }
        clearActions();
    }

    protected void showTroubleshootingAlert(@NonNull TroubleshootingAlert alert) {
        alertBusy.setVisibility(View.GONE);
        primaryAlertAction.setVisibility(View.VISIBLE);

        if (alert.title != null) {
            alertTitleContainer.setVisibility(View.VISIBLE);
            alertTitleText.setText(alert.title.resolve(getActivity()));
        }
        alertText.setGravity(Gravity.TOP | Gravity.START);
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
        StringRef title;
        StringRef message;
        @StringRes int primaryButtonTitle;
        Runnable primaryButtonOnClick;
        @StringRes int secondaryButtonTitle;
        Runnable secondaryButtonOnClick;

        public TroubleshootingAlert setTitle(@NonNull StringRef title){
            this.title = title;
            return this;
        }
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
