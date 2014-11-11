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

import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.TimeZoneDialogFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterBirthdayFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterGenderFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import rx.Observable;

public class MyInfoFragment extends InjectionFragment implements AdapterView.OnItemClickListener, AccountEditingFragment.Container {
    private static final int REQUEST_CODE_TIME_ZONE = 0x19;

    @Inject AccountPresenter accountPresenter;
    @Inject DateFormatter dateFormatter;
    @Inject UnitFormatter unitFormatter;

    private StaticItemAdapter.Item birthdayItem;
    private StaticItemAdapter.Item genderItem;
    private StaticItemAdapter.Item heightItem;
    private StaticItemAdapter.Item weightItem;
    private StaticItemAdapter.Item timeZoneItem;

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
        View view = inflater.inflate(R.layout.fragment_my_info, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());
        String placeholder = getString(R.string.missing_data_placeholder);
        this.birthdayItem = adapter.addItem(getString(R.string.label_dob), placeholder, this::changeBirthDate);
        this.genderItem = adapter.addItem(getString(R.string.label_gender), placeholder, this::changeGender);
        this.heightItem = adapter.addItem(getString(R.string.label_height), placeholder, this::changeHeight);
        this.weightItem = adapter.addItem(getString(R.string.label_weight), placeholder, this::changeWeight);
        this.timeZoneItem = adapter.addItem(getString(R.string.label_time_zone), placeholder, this::changeTimeZone);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Pair<Account, UnitSystem>> forAccount = Observable.combineLatest(accountPresenter.account,
                                                                                    unitFormatter.unitSystem,
                                                                                    Pair::new);
        bindAndSubscribe(forAccount, this::bindAccount, this::accountUnavailable);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_TIME_ZONE && resultCode == Activity.RESULT_OK) {
            String timeZoneId = data.getStringExtra(TimeZoneDialogFragment.RESULT_TIMEZONE_ID);
            DateTimeZone timeZone = DateTimeZone.forID(timeZoneId);
            int offset = timeZone.getOffset(DateTimeUtils.currentTimeMillis());
            currentAccount.setTimeZoneOffset(offset);

            LoadingDialogFragment.show(getFragmentManager());
            accountPresenter.saveAccount(currentAccount);
            accountPresenter.updateTimeZone(SenseTimeZone.fromDateTimeZone(timeZone))
                            .subscribe(ignored -> Logger.info(MyInfoFragment.class.getSimpleName(), "Updated time zone"),
                                    Functions.LOG_ERROR);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (currentAccount == null)
            return;

        StaticItemAdapter.Item item = (StaticItemAdapter.Item) adapterView.getItemAtPosition(position);
        if (item.getAction() != null)
            item.getAction().run();
    }


    public FragmentNavigation getNavigationContainer() {
        return (FragmentNavigation) getActivity();
    }

    public void changeBirthDate() {
        OnboardingRegisterBirthdayFragment fragment = new OnboardingRegisterBirthdayFragment();
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().showFragment(fragment, getString(R.string.label_dob), true);
    }

    public void changeGender() {
        OnboardingRegisterGenderFragment fragment = new OnboardingRegisterGenderFragment();
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().showFragment(fragment, getString(R.string.label_dob), true);
    }

    public void changeHeight() {
        OnboardingRegisterHeightFragment fragment = new OnboardingRegisterHeightFragment();
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().showFragment(fragment, getString(R.string.label_dob), true);
    }

    public void changeWeight() {
        OnboardingRegisterWeightFragment fragment = new OnboardingRegisterWeightFragment();
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().showFragment(fragment, getString(R.string.label_dob), true);
    }

    public void changeTimeZone() {
        TimeZoneDialogFragment dialogFragment = new TimeZoneDialogFragment();
        dialogFragment.setTargetFragment(this, REQUEST_CODE_TIME_ZONE);
        dialogFragment.show(getFragmentManager(), TimeZoneDialogFragment.TAG);
    }

    @NonNull
    @Override
    public Account getAccount() {
        return currentAccount;
    }

    @Override
    public void onAccountUpdated(@NonNull AccountEditingFragment updatedBy) {
        getFragmentManager().popBackStackImmediate();

        LoadingDialogFragment.show(getFragmentManager());
        accountPresenter.saveAccount(currentAccount);
    }


    public void bindAccount(@NonNull Pair<Account, UnitSystem> forAccount) {
        LoadingDialogFragment.close(getFragmentManager());

        Account account = forAccount.first;
        UnitSystem unitSystem = forAccount.second;

        birthdayItem.setValue(dateFormatter.formatAsBirthDate(account.getBirthDate()));
        genderItem.setValue(getString(account.getGender().nameRes));
        heightItem.setValue(unitSystem.formatHeight(account.getHeight()));
        weightItem.setValue(unitSystem.formatMass(account.getWeight()));
        timeZoneItem.setValue(DateTimeZone.forOffsetMillis(account.getTimeZoneOffset()).getName(DateTimeUtils.currentTimeMillis()));

        this.currentAccount = account;
    }

    public void accountUnavailable(@NonNull Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
