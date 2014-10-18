package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.Gender;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.DatePickerDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.GenderPickerDialogFragment;
import is.hello.sense.ui.dialogs.HeightDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.WeightDialogFragment;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.DateFormatter;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class MyInfoFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    private static final int REQUEST_CODE_BIRTH_DATE = 0x11;
    private static final int REQUEST_CODE_GENDER = 0x12;
    private static final int REQUEST_CODE_HEIGHT = 0x13;
    private static final int REQUEST_CODE_WEIGHT = 0x14;

    @Inject AccountPresenter accountPresenter;
    @Inject DateFormatter dateFormatter;
    @Inject UnitFormatter unitFormatter;

    private StaticItemAdapter.Item birthdayItem;
    private StaticItemAdapter.Item genderItem;
    private StaticItemAdapter.Item heightItem;
    private StaticItemAdapter.Item weightItem;

    private Account currentAccount;

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
        this.birthdayItem = adapter.addItem(getString(R.string.label_dob), getString(R.string.missing_data_placeholder), this::changeBirthDate);
        this.genderItem = adapter.addItem(getString(R.string.label_gender), getString(R.string.missing_data_placeholder), this::changeGender);
        this.heightItem = adapter.addItem(getString(R.string.label_height), getString(R.string.missing_data_placeholder), this::changeHeight);
        this.weightItem = adapter.addItem(getString(R.string.label_weight), getString(R.string.missing_data_placeholder), this::changeWeight);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Pair<Account, UnitSystem>> forAccount = Observable.combineLatest(accountPresenter.account,
                unitFormatter.unitSystem,
                Pair::new);
        track(bindFragment(this, forAccount).subscribe(this::bindAccount, this::accountUnavailable));
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (currentAccount == null)
            return;

        StaticItemAdapter.Item item = (StaticItemAdapter.Item) adapterView.getItemAtPosition(position);
        if (item.getAction() != null)
            item.getAction().run();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_BIRTH_DATE) {
                int year = data.getIntExtra(DatePickerDialogFragment.RESULT_YEAR, 0);
                int month = data.getIntExtra(DatePickerDialogFragment.RESULT_MONTH, 0);
                int day = data.getIntExtra(DatePickerDialogFragment.RESULT_DAY, 0);

                currentAccount.setBirthDate(new DateTime(year, month, day, 0, 0));
            } else if (requestCode == REQUEST_CODE_GENDER) {
                String genderName = data.getStringExtra(GenderPickerDialogFragment.RESULT_GENDER);
                Gender newGender = Gender.fromString(genderName);
                currentAccount.setGender(newGender);
            } else if (requestCode == REQUEST_CODE_HEIGHT) {
                int heightInInches = data.getIntExtra(HeightDialogFragment.RESULT_HEIGHT, 0);
                currentAccount.setHeight(UnitOperations.inchesToCentimeters(heightInInches));
            } else if (requestCode == REQUEST_CODE_WEIGHT) {
                long weightInPounds = data.getIntExtra(WeightDialogFragment.RESULT_WEIGHT, 0);
                currentAccount.setWeight(UnitOperations.poundsToGrams(weightInPounds));
            }

            LoadingDialogFragment.show(getFragmentManager());
            accountPresenter.saveAccount(currentAccount);
        }
    }

    public void changeBirthDate() {
        DatePickerDialogFragment dialogFragment = DatePickerDialogFragment.newInstance(currentAccount.getBirthDate());
        dialogFragment.setTargetFragment(this, REQUEST_CODE_BIRTH_DATE);
        dialogFragment.show(getFragmentManager(), DatePickerDialogFragment.TAG);
    }

    public void changeGender() {
        GenderPickerDialogFragment dialogFragment = new GenderPickerDialogFragment();
        dialogFragment.setTargetFragment(this, REQUEST_CODE_GENDER);
        dialogFragment.show(getFragmentManager(), GenderPickerDialogFragment.TAG);
    }

    public void changeHeight() {
        long heightInInches = UnitOperations.centimetersToInches(currentAccount.getHeight());
        HeightDialogFragment dialogFragment = HeightDialogFragment.newInstance(heightInInches);
        dialogFragment.setTargetFragment(this, REQUEST_CODE_HEIGHT);
        dialogFragment.show(getFragmentManager(), HeightDialogFragment.TAG);
    }

    public void changeWeight() {
        long weight = UnitOperations.gramsToPounds(currentAccount.getWeight());
        WeightDialogFragment dialogFragment = WeightDialogFragment.newInstance(weight);
        dialogFragment.setTargetFragment(this, REQUEST_CODE_WEIGHT);
        dialogFragment.show(getFragmentManager(), WeightDialogFragment.TAG);
    }


    public void bindAccount(@NonNull Pair<Account, UnitSystem> forAccount) {
        LoadingDialogFragment.close(getFragmentManager());

        Account account = forAccount.first;
        UnitSystem unitSystem = forAccount.second;

        birthdayItem.setValue(dateFormatter.formatAsBirthDate(account.getBirthDate()));
        genderItem.setValue(getString(account.getGender().nameRes));
        heightItem.setValue(unitSystem.formatHeight(account.getHeight()));
        weightItem.setValue(unitSystem.formatMass(account.getWeight()));

        this.currentAccount = account;
    }

    public void accountUnavailable(@NonNull Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
