package is.hello.sense.flows.smartalarm.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.NotTested;

@SuppressLint("ViewConstructor")
public class SmartAlarmDetailView extends PresenterView {

    private final TextView timeTextView;
    private final CompoundButton smartAlarmToggle;
    private final TextView toneNameTextView;
    private final TextView repeatDaysTextView;

    private final TextView lightExpansionValue;
    private final TextView thermoExpansionValue;

    private final LinearLayout expansionsContainer;
    private final LinearLayout lightExpansionContainer;
    private final LinearLayout thermoExpansionContainer;

    private final ProgressBar lightExpansionProgress;
    private final ProgressBar thermoExpansionProgress;

    private final ImageView lightExpansionError;
    private final ImageView thermoExpansionError;

    private final ImageView thermoExpansionIcon;
    private final ImageView lightExpansionIcon;

    private final TextView thermoExpansionLabel;
    private final TextView lightExpansionLabel;
    private final View deleteRowDivider;
    private final View deleteRow;

    public SmartAlarmDetailView(@NonNull final Activity activity,
                                @NonNull final OnClickListener timeClickListener,
                                @NonNull final OnClickListener helpClickListener,
                                @NonNull final OnClickListener toneClickListener,
                                @NonNull final OnClickListener repeatClickListener) {
        super(activity);
        this.expansionsContainer = (LinearLayout) findViewById(R.id.view_smart_alarm_detail_expansions_container);
        this.thermoExpansionIcon = (ImageView) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_temp_icon);
        this.lightExpansionIcon = (ImageView) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_light_icon);
        this.thermoExpansionLabel = (TextView) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_temp_label);
        this.lightExpansionLabel = (TextView) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_light_label);
        this.lightExpansionProgress = (ProgressBar) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_light_progress);
        this.thermoExpansionProgress = (ProgressBar) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_temp_progress);
        this.lightExpansionValue = (TextView) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_light_value);
        this.thermoExpansionValue = (TextView) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_temp_value);
        this.lightExpansionContainer = (LinearLayout) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_light_container);
        this.thermoExpansionContainer = (LinearLayout) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_temp_container);
        this.lightExpansionError = (ImageView) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_light_error);
        this.thermoExpansionError = (ImageView) expansionsContainer.findViewById(R.id.view_smart_alarm_detail_expansions_temp_error);
        this.timeTextView = (TextView) findViewById(R.id.view_smart_alarm_detail_time);
        this.smartAlarmToggle = (CompoundButton) findViewById(R.id.view_smart_alarm_detail_smart_switch);
        this.toneNameTextView = (TextView) findViewById(R.id.view_smart_alarm_detail_tone_name);
        this.repeatDaysTextView = (TextView) findViewById(R.id.view_smart_alarm_detail_repeat_days);
        this.deleteRowDivider = findViewById(R.id.view_smart_alarm_detail_delete_divider);
        this.deleteRow = findViewById(R.id.view_smart_alarm_detail_delete);
        final View smartRow = findViewById(R.id.view_smart_alarm_detail_smart);
        final View timeContainer = findViewById(R.id.view_smart_alarm_detail_time_container);
        final View soundRow = findViewById(R.id.view_smart_alarm_detail_tone);
        final View repeatRow = findViewById(R.id.view_smart_alarm_detail_repeat);
        final View smartHelp = findViewById(R.id.view_smart_alarm_detail_smart_help);
        Views.setSafeOnClickListener(smartRow, v -> this.smartAlarmToggle.toggle());
        Views.setSafeOnClickListener(smartHelp, helpClickListener);
        Views.setSafeOnClickListener(timeContainer, timeClickListener);
        Views.setSafeOnClickListener(soundRow, toneClickListener);
        Views.setSafeOnClickListener(repeatRow, repeatClickListener);
    }
    //region PresenterView

    @NotTested
    @Override
    protected int getLayoutRes() {
        return R.layout.view_smart_alarm_detail;
    }

    @NotTested
    @Override
    public void releaseViews() {

    }

    //endregion
    //region methods

    /**
     * @param deleteClickListener if null will hide the delete row. If not null will show and set.
     */
    @NotTested
    public void showDeleteRow(@Nullable final OnClickListener deleteClickListener) {
        if (deleteClickListener == null) {
            this.deleteRowDivider.setVisibility(View.GONE);
            this.deleteRow.setVisibility(View.GONE);
            this.deleteRow.setOnClickListener(null);
        } else {
            Views.setSafeOnClickListener(this.deleteRow, deleteClickListener);
            this.deleteRowDivider.setVisibility(View.VISIBLE);
            this.deleteRow.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Set the state of the smart alarm toggle button.
     *
     * @param isChecked             set to true or false
     * @param checkedChangeListener listener to call when the user taps this.
     */
    @NotTested
    public void setSmartAlarmToggle(final boolean isChecked,
                                    @NonNull final CompoundButton.OnCheckedChangeListener checkedChangeListener) {
        this.smartAlarmToggle.setOnCheckedChangeListener(null);
        this.smartAlarmToggle.setChecked(isChecked);
        this.smartAlarmToggle.setOnCheckedChangeListener(checkedChangeListener);
    }

    /**
     * Set the text for the current tone.
     *
     * @param toneName if null will default to "None".
     */
    @NotTested
    public void setTone(@Nullable final String toneName) {
        if (toneName == null) {
            this.toneNameTextView.setText(R.string.no_sound_placeholder);
        }
        this.toneNameTextView.setText(toneName);
    }

    /**
     * @param repeatDays set text for repeat days.
     */
    @NotTested
    public void setRepeatDaysTextView(@NonNull final String repeatDays) {
        this.repeatDaysTextView.setText(repeatDays);
    }

    /**
     * Display the two expansions rows.
     */
    @NotTested
    public void showExpansionsContainer() {
        this.expansionsContainer.setVisibility(View.VISIBLE);
    }

    /**
     * @param time set text of current time shown.
     */
    @NotTested
    public void setTime(@NonNull final CharSequence time) {
        this.timeTextView.setText(time);
    }

    /**
     * Set the state of the {@link Category#LIGHT} expansion row.
     *
     * @param isEnabled              if true will be clickable.
     * @param value                  text for this expansion.
     * @param containerClickListener click listener
     */
    @NotTested
    public void setLightExpansion(final boolean isEnabled,
                                  @NonNull final String value,
                                  @NonNull final OnClickListener containerClickListener) {
        this.lightExpansionError.setVisibility(GONE);
        this.lightExpansionProgress.setVisibility(View.GONE);
        this.lightExpansionValue.setVisibility(View.VISIBLE);
        this.lightExpansionIcon.setImageAlpha(Styles.getImageViewAlpha(isEnabled));
        this.lightExpansionLabel.setEnabled(isEnabled);
        this.lightExpansionValue.setEnabled(isEnabled);
        this.lightExpansionContainer.setEnabled(isEnabled);
        this.lightExpansionValue.setText(value);
        Views.setSafeOnClickListener(this.lightExpansionContainer, containerClickListener);
    }

    /**
     * Set the state of the {@link Category#TEMPERATURE} expansion row.
     *
     * @param isEnabled              if true will be clickable.
     * @param value                  text for this expansion.
     * @param containerClickListener click listener
     */
    @NotTested
    public void setThermoExpansion(final boolean isEnabled,
                                   @NonNull final String value,
                                   @NonNull final OnClickListener containerClickListener) {
        this.thermoExpansionError.setVisibility(GONE);
        this.thermoExpansionProgress.setVisibility(View.GONE);
        this.thermoExpansionValue.setVisibility(View.VISIBLE);
        this.thermoExpansionIcon.setImageAlpha(Styles.getImageViewAlpha(isEnabled));
        this.thermoExpansionLabel.setEnabled(isEnabled);
        this.thermoExpansionValue.setEnabled(isEnabled);
        this.thermoExpansionContainer.setEnabled(isEnabled);
        this.thermoExpansionValue.setText(value);
        Views.setSafeOnClickListener(this.thermoExpansionContainer, containerClickListener);
    }


    //endregion
}
