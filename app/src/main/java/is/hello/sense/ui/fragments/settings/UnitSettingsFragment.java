package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.EnumSet;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.adapter.UnitSettingsAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ScrollEdge;
import is.hello.sense.ui.recycler.DividerItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;

public class UnitSettingsFragment extends InjectionFragment
        implements Handler.Callback, UnitSettingsAdapter.OnRadioChangeListener {
    private static final int REQUEST_CODE_ERROR = 0xE3;

    private static final int DELAY_PUSH_PREFERENCES = 3000;
    private static final int MSG_PUSH_PREFERENCES = 0x5;

    @Inject AccountPresenter accountPresenter;
    @Inject PreferencesPresenter preferencesPresenter;

    private final Handler handler = new Handler(Looper.getMainLooper(), this);


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Backside.EVENT_UNITS_TIME, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_unit_settings, container, false);

        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.fragment_unit_settings_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(resources));
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     EnumSet.of(ScrollEdge.TOP),
                                                                     FadingEdgesItemDecoration.Style.STRAIGHT));

        final UnitSettingsAdapter unitSettingsAdapter = new UnitSettingsAdapter(getActivity(), this);
        recyclerView.setAdapter(unitSettingsAdapter);

        final boolean defaultIsMetric = UnitFormatter.isDefaultLocaleMetric();
        unitSettingsAdapter.addItem(new UnitSettingsAdapter.UnitItem(PreferencesPresenter.USE_24_TIME,
                                                                     R.string.setting_time_unit,
                                                                     R.string.setting_time_12_hour,
                                                                     R.string.setting_time_24_hour),
                                    preferencesPresenter.getBoolean(PreferencesPresenter.USE_24_TIME,
                                                                    preferencesPresenter.getUse24Time()));
        unitSettingsAdapter.addItem(new UnitSettingsAdapter.UnitItem(PreferencesPresenter.USE_CELSIUS,
                                                                     R.string.setting_unit_temperature,
                                                                     R.string.setting_option_temperature_us,
                                                                     R.string.setting_option_temperature_metric),
                                    preferencesPresenter.getBoolean(PreferencesPresenter.USE_CELSIUS,
                                                                    defaultIsMetric));
        unitSettingsAdapter.addItem(new UnitSettingsAdapter.UnitItem(PreferencesPresenter.USE_GRAMS,
                                                                     R.string.setting_unit_weight,
                                                                     R.string.setting_option_weight_us,
                                                                     R.string.setting_option_weight_metric),
                                    preferencesPresenter.getBoolean(PreferencesPresenter.USE_GRAMS,
                                                                    defaultIsMetric));
        //noinspection SuspiciousNameCombination
        unitSettingsAdapter.addItem(new UnitSettingsAdapter.UnitItem(PreferencesPresenter.USE_CENTIMETERS,
                                                                     R.string.setting_unit_height,
                                                                     R.string.setting_option_height_us,
                                                                     R.string.setting_option_height_metric),
                                    preferencesPresenter.getBoolean(PreferencesPresenter.USE_CENTIMETERS,
                                                                    defaultIsMetric));
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (handler.hasMessages(MSG_PUSH_PREFERENCES)) {
            handler.removeMessages(MSG_PUSH_PREFERENCES);
            accountPresenter.pushAccountPreferences();
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


    @Override
    public void onRadioValueChanged(@NonNull String key, boolean newValue) {
        preferencesPresenter.edit()
                            .putBoolean(key, newValue)
                            .apply();

        handler.removeMessages(MSG_PUSH_PREFERENCES);
        handler.sendEmptyMessageDelayed(MSG_PUSH_PREFERENCES, DELAY_PUSH_PREFERENCES);
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_PUSH_PREFERENCES) {
            accountPresenter.pushAccountPreferences();
            return true;
        }
        return false;
    }

}
