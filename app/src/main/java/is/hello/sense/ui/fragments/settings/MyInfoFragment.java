package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class MyInfoFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject AccountPresenter accountPresenter;
    @Inject DateFormatter dateFormatter;
    @Inject UnitFormatter unitFormatter;

    private StaticItemAdapter.Item birthdayItem;
    private StaticItemAdapter.Item genderItem;
    private StaticItemAdapter.Item heightItem;
    private StaticItemAdapter.Item weightItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountPresenter.update();
        addPresenter(accountPresenter);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.simple_list_view, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());
        this.birthdayItem = adapter.addItem(getString(R.string.label_dob), getString(R.string.missing_data_placeholder));
        this.genderItem = adapter.addItem(getString(R.string.label_gender), getString(R.string.missing_data_placeholder));
        this.heightItem = adapter.addItem(getString(R.string.label_height), getString(R.string.missing_data_placeholder));
        this.weightItem = adapter.addItem(getString(R.string.label_weight), getString(R.string.missing_data_placeholder));
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Pair<Account, UnitSystem>> forAccount = Observable.combineLatest(accountPresenter.account,
                unitFormatter.unitSystem,
                Pair::new);
        track(bindFragment(this, forAccount).subscribe(this::bindAccount, this::presentError));
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

    }


    public void bindAccount(@NonNull Pair<Account, UnitSystem> forAccount) {
        Account account = forAccount.first;
        UnitSystem unitSystem = forAccount.second;

        birthdayItem.setValue(dateFormatter.formatAsBirthDate(account.getBirthDate()));
        genderItem.setValue(account.getGender().toString());
        heightItem.setValue(unitSystem.formatHeight(account.getHeight()));
        weightItem.setValue(unitSystem.formatMass(account.getWeight()));
    }

    public void presentError(@NonNull Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
