package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.Locale;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.units.systems.MetricUnitSystem;
import is.hello.sense.units.systems.UsCustomaryUnitSystem;
import is.hello.sense.util.Analytics;
import rx.Observable;

public class UnitSettingsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    private static final int REQUEST_CODE_ERROR = 0xE3;

    @Inject PreferencesPresenter preferencesPresenter;

    private StaticItemAdapter.TextItem unitSystemItem;
    private StaticItemAdapter.CheckItem use24TimeItem;

    private ProgressBar loadingIndicator;
    private ListView listView;

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
        View view = inflater.inflate(R.layout.list_view_static, container, false);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.list_view_static_loading);

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());

        String unitSystem = preferencesPresenter.getString(PreferencesPresenter.UNIT_SYSTEM, UnitSystem.getLocaleUnitSystemName(Locale.getDefault()));
        this.unitSystemItem = adapter.addTextItem(getString(R.string.setting_title_units), mapUnitSystemName(unitSystem), this::updateUnitSystem);

        boolean use24Time = preferencesPresenter.getUse24Time();
        this.use24TimeItem = adapter.addCheckItem(R.string.setting_title_use_24_time, use24Time, this::updateUse24Time);

        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showLoading();
        bindAndSubscribe(preferencesPresenter.pullAccountPreferences(),
                         ignored -> hideLoading(),
                         this::pullingPreferencesFailed);

        Observable<String> unitSystemName = preferencesPresenter.observableString(PreferencesPresenter.UNIT_SYSTEM, UnitSystem.getLocaleUnitSystemName(Locale.getDefault()));
        bindAndSubscribe(unitSystemName.map(this::mapUnitSystemName),
                         unitSystemItem::setDetail,
                         Functions.LOG_ERROR);
        bindAndSubscribe(preferencesPresenter.observableUse24Time(),
                use24TimeItem::setChecked,
                Functions.LOG_ERROR);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.unitSystemItem = null;
        this.use24TimeItem = null;

        this.loadingIndicator = null;
        this.listView = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ERROR && resultCode == Activity.RESULT_OK) {
            getActivity().finish();
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        StaticItemAdapter.Item item = (StaticItemAdapter.Item) parent.getItemAtPosition(position);
        if (item.getAction() != null) {
            item.getAction().run();
        }
    }


    public @NonNull String mapUnitSystemName(@NonNull String unitSystemName) {
        switch (unitSystemName) {
            case UsCustomaryUnitSystem.NAME: {
                return getString(R.string.unit_system_us_customary);
            }

            default:
            case MetricUnitSystem.NAME: {
                return getString(R.string.unit_system_metric);
            }
        }
    }

    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }


    public void updateUnitSystem() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true);

        String[] unitSystemIds = {
                UsCustomaryUnitSystem.NAME,
                MetricUnitSystem.NAME,
        };
        String[] unitSystemNames = {
                getString(R.string.unit_system_us_customary),
                getString(R.string.unit_system_metric),
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.item_simple_text, unitSystemNames);
        builder.setAdapter(adapter, (dialog, position) -> {
            preferencesPresenter.edit()
                    .putString(PreferencesPresenter.UNIT_SYSTEM, unitSystemIds[position])
                    .commit();
            preferencesPresenter.pushAccountPreferences().subscribe();
        });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.getListView().setDivider(null);
        dialog.show();
    }

    public void updateUse24Time() {
        boolean update = !use24TimeItem.isChecked();
        preferencesPresenter.edit()
                            .putBoolean(PreferencesPresenter.USE_24_TIME, update)
                            .apply();
        preferencesPresenter.pushAccountPreferences().subscribe();
    }

    public void pullingPreferencesFailed(Throwable e) {
        loadingIndicator.setVisibility(View.GONE);

        ErrorDialogFragment errorDialogFragment = ErrorDialogFragment.newInstance(e);
        errorDialogFragment.setTargetFragment(this, REQUEST_CODE_ERROR);
        errorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
    }
}
