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

    private StaticItemAdapter.TextItem nameItem;
    private StaticItemAdapter.TextItem emailItem;

    private StaticItemAdapter.TextItem birthdayItem;
    private StaticItemAdapter.TextItem genderItem;
    private StaticItemAdapter.TextItem heightItem;
    private StaticItemAdapter.TextItem weightItem;
    private StaticItemAdapter.TextItem timeZoneItem;

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
        View view = inflater.inflate(R.layout.list_view_static_item, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());

        adapter.addTitle(R.string.title_info);
        this.nameItem = adapter.addItem(R.string.label_name, R.string.missing_data_placeholder);
        this.emailItem = adapter.addItem(R.string.label_email, R.string.missing_data_placeholder, this::changeEmail);
        adapter.addItem(R.string.title_change_password, R.string.detail_change_password, this::changePassword);

        adapter.addTitle(R.string.title_demographics);
        this.birthdayItem = adapter.addItem(R.string.label_dob, R.string.missing_data_placeholder, this::changeBirthDate);
        this.genderItem = adapter.addItem(R.string.label_gender, R.string.missing_data_placeholder, this::changeGender);
        this.heightItem = adapter.addItem(R.string.label_height, R.string.missing_data_placeholder, this::changeHeight);
        this.weightItem = adapter.addItem(R.string.label_weight, R.string.missing_data_placeholder, this::changeWeight);
        this.timeZoneItem = adapter.addItem(R.string.label_time_zone, R.string.missing_data_placeholder, this::changeTimeZone);

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

        nameItem.setDetail(account.getName());
        emailItem.setDetail(account.getEmail());

        birthdayItem.setDetail(dateFormatter.formatAsBirthDate(account.getBirthDate()));
        genderItem.setDetail(getString(account.getGender().nameRes));
        heightItem.setDetail(unitSystem.formatHeight(account.getHeight()));
        weightItem.setDetail(unitSystem.formatMass(account.getWeight()));
        timeZoneItem.setDetail(DateTimeZone.forOffsetMillis(account.getTimeZoneOffset()).getName(DateTimeUtils.currentTimeMillis()));

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
