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

public class AccountSettingsFragment extends InjectionFragment implements AdapterView.OnItemClickListener, AccountEditingFragment.Container {
    private static final int REQUEST_CODE_TIME_ZONE = 0x19;

    @Inject AccountPresenter accountPresenter;
    @Inject DateFormatter dateFormatter;
    @Inject UnitFormatter unitFormatter;

    private StaticItemAdapter.Item nameItem;
    private StaticItemAdapter.Item emailItem;

    private StaticItemAdapter.Item birthdayItem;
    private StaticItemAdapter.Item genderItem;
    private StaticItemAdapter.Item heightItem;
    private StaticItemAdapter.Item weightItem;
    private StaticItemAdapter.Item timeZoneItem;

    private Account currentAccount;


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(accountPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.simple_list_view, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());

        String placeholder = getString(R.string.missing_data_placeholder);
        this.nameItem = adapter.addItem(getString(R.string.label_name), placeholder);
        this.emailItem = adapter.addItem(getString(R.string.label_email), placeholder, this::changeEmail);
        adapter.addItem(getString(R.string.title_change_password), null, this::changePassword);

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

        accountPresenter.update();

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
                            .subscribe(ignored -> Logger.info(getClass().getSimpleName(), "Updated time zone"),
                            Functions.LOG_ERROR);
        }
    }

    //endregion


    //region Glue

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (currentAccount == null) {
            return;
        }

        StaticItemAdapter.Item item = (StaticItemAdapter.Item) adapterView.getItemAtPosition(position);
        if (item.getAction() != null) {
            item.getAction().run();
        }
    }

    public FragmentNavigation getNavigationContainer() {
        return (FragmentNavigation) getActivity();
    }

    //endregion


    //region Binding Data

    public void bindAccount(@NonNull Pair<Account, UnitSystem> forAccount) {
        LoadingDialogFragment.close(getFragmentManager());

        Account account = forAccount.first;
        UnitSystem unitSystem = forAccount.second;

        nameItem.setValue(account.getName());
        emailItem.setValue(account.getEmail());

        birthdayItem.setValue(dateFormatter.formatAsBirthDate(account.getBirthDate()));
        genderItem.setValue(getString(account.getGender().nameRes));
        heightItem.setValue(unitSystem.formatHeight(account.getHeight()));
        weightItem.setValue(unitSystem.formatMass(account.getWeight()));
        timeZoneItem.setValue(DateTimeZone.forOffsetMillis(account.getTimeZoneOffset()).getName(DateTimeUtils.currentTimeMillis()));

        this.currentAccount = account;
    }

    public void accountUnavailable(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }

    //endregion


    //region Basic Info

    public void changeEmail() {
        FragmentNavigation navigation = (FragmentNavigation) getActivity();
        navigation.pushFragment(new ChangeEmailFragment(), getString(R.string.title_change_email), true);
    }

    public void changePassword() {
        FragmentNavigation navigation = (FragmentNavigation) getActivity();
        navigation.pushFragment(ChangePasswordFragment.newInstance(currentAccount.getEmail()), getString(R.string.title_change_password), true);
    }

    //endregion


    //region Demographics

    public void changeBirthDate() {
        OnboardingRegisterBirthdayFragment fragment = new OnboardingRegisterBirthdayFragment();
        fragment.setWantsSkipButton(false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().pushFragment(fragment, getString(R.string.label_dob), true);
    }

    public void changeGender() {
        OnboardingRegisterGenderFragment fragment = new OnboardingRegisterGenderFragment();
        fragment.setWantsSkipButton(false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().pushFragment(fragment, getString(R.string.label_gender), true);
    }

    public void changeHeight() {
        OnboardingRegisterHeightFragment fragment = new OnboardingRegisterHeightFragment();
        fragment.setWantsSkipButton(false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().pushFragment(fragment, getString(R.string.label_height), true);
    }

    public void changeWeight() {
        OnboardingRegisterWeightFragment fragment = new OnboardingRegisterWeightFragment();
        fragment.setWantsSkipButton(false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().pushFragment(fragment, getString(R.string.label_weight), true);
    }

    public void changeTimeZone() {
        TimeZoneDialogFragment dialogFragment = new TimeZoneDialogFragment();
        dialogFragment.setTargetFragment(this, REQUEST_CODE_TIME_ZONE);
        dialogFragment.show(getFragmentManager(), TimeZoneDialogFragment.TAG);
    }

    //endregion


    //region Updates

    @NonNull
    @Override
    public Account getAccount() {
        return currentAccount;
    }

    @Override
    public void onAccountUpdated(@NonNull AccountEditingFragment updatedBy) {
        updatedBy.getFragmentManager().popBackStackImmediate();
        coordinator.postOnResume(() -> {
            LoadingDialogFragment.show(getFragmentManager());
            accountPresenter.saveAccount(currentAccount);
        });
    }

    //endregion
}
