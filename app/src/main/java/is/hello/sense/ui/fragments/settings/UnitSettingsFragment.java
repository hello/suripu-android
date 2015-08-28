package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import rx.functions.Action1;

public class UnitSettingsFragment extends InjectionFragment implements Handler.Callback {
    private static final int REQUEST_CODE_ERROR = 0xE3;

    private static final int DELAY_PUSH_PREFERENCES = 3000;
    private static final int MSG_PUSH_PREFERENCES = 0x5;

    @Inject PreferencesPresenter preferencesPresenter;

    private final Handler handler = new Handler(Looper.getMainLooper(), this);
    private CheckBox use24TimeToggle;
    private Spinner temperatureSpinner;
    private Spinner weightSpinner;
    private Spinner heightSpinner;


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_UNITS_TIME, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_unit_settings, container, false);

        this.use24TimeToggle = (CheckBox) view.findViewById(R.id.fragment_unit_settings_use_24_time);
        Views.setSafeOnClickListener(use24TimeToggle, this::updateUse24Time);

        this.temperatureSpinner = (Spinner) view.findViewById(R.id.fragment_unit_settings_temperature);
        temperatureSpinner.setAdapter(new SwitchAdapter(getActivity(), new int[]{
                R.string.setting_option_temperature_us,
                R.string.setting_option_temperature_metric,
        }));
        final TextView temperatureTitle = (TextView) view.findViewById(R.id.fragment_unit_settings_temperature_title);
        temperatureTitle.setOnClickListener(ignored -> temperatureSpinner.performClick());

        this.weightSpinner = (Spinner) view.findViewById(R.id.fragment_unit_settings_weight);
        weightSpinner.setAdapter(new SwitchAdapter(getActivity(), new int[]{
                R.string.setting_option_weight_us,
                R.string.setting_option_weight_metric,
        }));
        final TextView weightTitle = (TextView) view.findViewById(R.id.fragment_unit_settings_weight_title);
        weightTitle.setOnClickListener(ignored -> weightSpinner.performClick());

        this.heightSpinner = (Spinner) view.findViewById(R.id.fragment_unit_settings_height);
        heightSpinner.setAdapter(new SwitchAdapter(getActivity(), new int[]{
                R.string.setting_option_height_us,
                R.string.setting_option_height_metric,
        }));
        final TextView heightTitle = (TextView) view.findViewById(R.id.fragment_unit_settings_height_title);
        heightTitle.setOnClickListener(ignored -> heightSpinner.performClick());

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(preferencesPresenter.pullAccountPreferences(),
                         Functions.NO_OP,
                         this::pullingPreferencesFailed);


        bindAndSubscribe(preferencesPresenter.observableUse24Time(),
                         use24TimeToggle::setChecked,
                         Functions.LOG_ERROR);

        boolean defaultIsMetric = UnitFormatter.isDefaultLocaleMetric();
        bindAndSubscribe(preferencesPresenter.observableBoolean(PreferencesPresenter.USE_CELSIUS,
                                                                defaultIsMetric),
                         createSpinnerValueBinder(temperatureSpinner, PreferencesPresenter.USE_CELSIUS),
                         Functions.LOG_ERROR);
        bindAndSubscribe(preferencesPresenter.observableBoolean(PreferencesPresenter.USE_GRAMS,
                                                                defaultIsMetric),
                         createSpinnerValueBinder(weightSpinner, PreferencesPresenter.USE_GRAMS),
                         Functions.LOG_ERROR);
        bindAndSubscribe(preferencesPresenter.observableBoolean(PreferencesPresenter.USE_CENTIMETERS,
                                                                defaultIsMetric),
                         createSpinnerValueBinder(heightSpinner, PreferencesPresenter.USE_CENTIMETERS),
                         Functions.LOG_ERROR);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.use24TimeToggle = null;
        this.temperatureSpinner = null;
        this.weightSpinner = null;
        this.heightSpinner = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (handler.hasMessages(MSG_PUSH_PREFERENCES)) {
            handler.removeMessages(MSG_PUSH_PREFERENCES);
            preferencesPresenter.pushAccountPreferences();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ERROR && resultCode == Activity.RESULT_OK) {
            getActivity().finish();
        }
    }

    //endregion


    //region Spinners

    private Action1<Boolean> createSpinnerValueBinder(@NonNull Spinner spinner, @NonNull String key) {
        return value -> {
            spinner.setOnItemSelectedListener(null);

            final int position = value
                    ? SwitchAdapter.POSITION_TRUE
                    : SwitchAdapter.POSITION_FALSE;
            spinner.setSelection(position);

            // Otherwise, the item listener will fire on the next layout
            // when the selection is actually updated, and we end up with
            // a bunch of completely unnecessary requests.
            spinner.post(() -> {
                spinner.setOnItemSelectedListener(createSpinnerSelectionListener(key));
            });
        };
    }

    private Spinner.OnItemSelectedListener createSpinnerSelectionListener(@NonNull String key) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case SwitchAdapter.POSITION_TRUE:
                        updatePreference(key, true);
                        break;
                    case SwitchAdapter.POSITION_FALSE:
                        updatePreference(key, false);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
    }

    //endregion


    public void updatePreference(@NonNull String key, boolean newValue) {
        preferencesPresenter.edit()
                            .putBoolean(key, newValue)
                            .apply();

        handler.removeMessages(MSG_PUSH_PREFERENCES);
        handler.sendEmptyMessageDelayed(MSG_PUSH_PREFERENCES, DELAY_PUSH_PREFERENCES);
    }

    public void updateUse24Time(@NonNull View ignored) {
        updatePreference(PreferencesPresenter.USE_24_TIME, use24TimeToggle.isChecked());
    }

    public void pullingPreferencesFailed(Throwable e) {
        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e).build();
        errorDialogFragment.setTargetFragment(this, REQUEST_CODE_ERROR);
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_PUSH_PREFERENCES) {
            preferencesPresenter.pushAccountPreferences();
            return true;
        }
        return false;
    }


    static class SwitchAdapter extends ArrayAdapter<Boolean> {
        static final int POSITION_FALSE = 0;
        static final int POSITION_TRUE = 1;

        private final LayoutInflater inflater;
        private final @StringRes int[] titles;

        SwitchAdapter(@NonNull Context context,
                      @NonNull @StringRes int[] titles) {
            super(context, R.layout.item_simple_text, new Boolean[] { false, true });

            this.inflater = LayoutInflater.from(context);
            this.titles = titles;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text = (TextView) convertView;
            if (text == null) {
                text = (TextView) inflater.inflate(R.layout.item_unit_spinner, parent, false);
            }

            final int title = titles[position];
            text.setText(title);

            return text;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }
}
