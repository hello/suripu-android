package is.hello.sense.flows.smartalarm.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Drawing;
import is.hello.sense.ui.widget.util.Views;

@SuppressLint("ViewConstructor")
public class SmartAlarmDetailView extends PresenterView {

    private final TextView time;
    private final TextView toneName;
    private final TextView repeatDays;

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

    public SmartAlarmDetailView(@NonNull final Activity activity,
                                @NonNull final OnClickListener timeClickListener,
                                @NonNull final OnClickListener helpClickListener,
                                @NonNull final OnClickListener toneClickListener,
                                @NonNull final OnClickListener repeatClickListener,
                                @NonNull final OnClickListener deleteClickListener) {
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
        this.time = (TextView) findViewById(R.id.view_smart_alarm_detail_time);
        //  setVoiceState(false);
        //  updateTime();
        final View timeContainer = findViewById(R.id.view_smart_alarm_detail_time_container);
        Views.setSafeOnClickListener(timeContainer, timeClickListener);
        final CompoundButton smartToggle = (CompoundButton) findViewById(R.id.view_smart_alarm_detail_smart_switch);
       /* smartToggle.setChecked(alarm.isSmart());
        smartToggle.setOnCheckedChangeListener((button, checked) -> {
            alarm.setSmart(checked);
            markDirty();
        });*/

        final View smartRow = findViewById(R.id.view_smart_alarm_detail_smart);
        smartRow.setOnClickListener(ignored -> smartToggle.toggle());

        final ImageButton smartHelp = (ImageButton) findViewById(R.id.view_smart_alarm_detail_smart_help);
        final Drawable smartHelpDrawable = smartHelp.getDrawable().mutate();
        final int accent = ContextCompat.getColor(activity, R.color.light_accent);
        final int dimmedAccent = Drawing.colorWithAlpha(accent, 178);
        Drawables.setTintColor(smartHelpDrawable, dimmedAccent);
        smartHelp.setImageDrawable(smartHelpDrawable);
        Views.setSafeOnClickListener(smartHelp, helpClickListener);


        final View soundRow = findViewById(R.id.view_smart_alarm_detail_tone);
        Views.setSafeOnClickListener(soundRow, toneClickListener);

        this.toneName = (TextView) soundRow.findViewById(R.id.view_smart_alarm_detail_tone_name);
        /*if (alarm.getSound() != null && !TextUtils.isEmpty(alarm.getSound().name)) {
            toneName.setText(alarm.getSound().name);
        } else {
            toneName.setText(R.string.no_sound_placeholder);
        }*/

        final View repeatRow = findViewById(R.id.view_smart_alarm_detail_repeat);
        Views.setSafeOnClickListener(repeatRow, repeatClickListener);

        this.repeatDays = (TextView) repeatRow.findViewById(R.id.view_smart_alarm_detail_repeat_days);
        //repeatDays.setText(alarm.getRepeatSummary(getActivity(), false));

        final View deleteRow = findViewById(R.id.view_smart_alarm_detail_delete);
        Views.setSafeOnClickListener(deleteRow, deleteClickListener);

       /* if (this.index == SmartAlarmDetailActivity.INDEX_NEW) {
            final View deleteRowDivider = view.findViewById(R.id.fragment_smart_alarm_detail_delete_divider);
            deleteRowDivider.setVisibility(View.GONE);
            deleteRow.setVisibility(View.GONE);
        }*/

    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_smart_alarm_detail;
    }

    @Override
    public void releaseViews() {

    }
}
