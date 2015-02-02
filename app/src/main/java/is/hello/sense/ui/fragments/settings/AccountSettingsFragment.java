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
import android.widget.ProgressBar;

import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

import java.util.HashMap;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.AccountPreference;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.TimeZoneDialogFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterBirthdayFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterGenderFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.units.UnitSystem;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;
import rx.Observable;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class AccountSettingsFragment extends InjectionFragment implements AdapterView.OnItemClickListener, AccountEditingFragment.Container {
    private static final int REQUEST_CODE_TIME_ZONE = 0x19;
    private static final int REQUEST_CODE_PASSWORD = 0x20;

    @Inject AccountPresenter accountPresenter;
    @Inject DateFormatter dateFormatter;
    @Inject UnitFormatter unitFormatter;

    private ProgressBar loadingIndicator;

    private StaticItemAdapter.TextItem nameItem;
    private StaticItemAdapter.TextItem emailItem;

    private StaticItemAdapter.TextItem birthdayItem;
    private StaticItemAdapter.TextItem genderItem;
    private StaticItemAdapter.TextItem heightItem;
    private StaticItemAdapter.TextItem weightItem;
    private StaticItemAdapter.TextItem timeZoneItem;

    private StaticItemAdapter.CheckItem enhancedAudioItem;

    private Account currentAccount;
    private ListView listView;


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.currentAccount = (Account) savedInstanceState.getSerializable("currentAccount");
        } else {
            Analytics.trackEvent(Analytics.TopView.EVENT_ACCOUNT, null);
        }

        accountPresenter.update();
        addPresenter(accountPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_view_static, container, false);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.list_view_static_loading);

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());

        adapter.addSectionTitle(R.string.title_info);
        this.nameItem = adapter.addTextItem(R.string.label_name, R.string.missing_data_placeholder, this::changeName);
        this.emailItem = adapter.addTextItem(R.string.label_email, R.string.missing_data_placeholder, this::changeEmail);
        adapter.addTextItem(R.string.title_change_password, R.string.detail_change_password, this::changePassword);

        adapter.addSectionTitle(R.string.title_demographics);
        this.birthdayItem = adapter.addTextItem(R.string.label_dob, R.string.missing_data_placeholder, this::changeBirthDate);
        this.genderItem = adapter.addTextItem(R.string.label_gender, R.string.missing_data_placeholder, this::changeGender);
        this.heightItem = adapter.addTextItem(R.string.label_height, R.string.missing_data_placeholder, this::changeHeight);
        this.weightItem = adapter.addTextItem(R.string.label_weight, R.string.missing_data_placeholder, this::changeWeight);
        this.timeZoneItem = adapter.addTextItem(R.string.label_time_zone, R.string.missing_data_placeholder, this::changeTimeZone);

        adapter.addSectionTitle(R.string.label_options);
        this.enhancedAudioItem = adapter.addCheckItem(R.string.label_enhanced_audio, false, this::changeEnhancedAudio);
        adapter.addFooterItem(R.string.info_enhanced_audio);

        adapter.addSectionDivider();
        adapter.addTextItem(R.string.action_log_out, 0, this::signOut);

        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Observable<Pair<Account, UnitSystem>> forAccount = Observable.combineLatest(accountPresenter.account,
                                                                                    unitFormatter.unitSystem,
                                                                                    Pair::new);
        bindAndSubscribe(forAccount, this::bindAccount, this::presentError);

        bindAndSubscribe(accountPresenter.preferences(),
                         this::bindAccountPreferences,
                         this::presentError);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("currentAccount", currentAccount);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_TIME_ZONE && resultCode == Activity.RESULT_OK) {
            String timeZoneId = data.getStringExtra(TimeZoneDialogFragment.RESULT_TIMEZONE_ID);
            DateTimeZone timeZone = DateTimeZone.forID(timeZoneId);
            int offset = timeZone.getOffset(DateTimeUtils.currentTimeMillis());
            currentAccount.setTimeZoneOffset(offset);

            saveAccount();
            accountPresenter.updateTimeZone(SenseTimeZone.fromDateTimeZone(timeZone))
                            .subscribe(ignored -> Logger.info(getClass().getSimpleName(), "Updated time zone"),
                            Functions.LOG_ERROR);
        } else if (requestCode == REQUEST_CODE_PASSWORD && resultCode == Activity.RESULT_OK) {
            accountPresenter.update();
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

    private void showLoadingIndicator() {
        animate(listView)
                .fadeOut(View.INVISIBLE)
                .start();
        animate(loadingIndicator)
                .fadeIn()
                .start();
    }

    private void hideLoadingIndicator() {
        animate(loadingIndicator)
                .fadeOut(View.INVISIBLE)
                .start();
        animate(listView)
                .fadeIn()
                .start();
    }

    //endregion


    //region Binding Data

    public void bindAccount(@NonNull Pair<Account, UnitSystem> forAccount) {
        Account account = forAccount.first;
        UnitSystem unitSystem = forAccount.second;

        nameItem.setDetail(account.getName());
        emailItem.setDetail(account.getEmail());

        birthdayItem.setDetail(dateFormatter.formatAsBirthDate(account.getBirthDate()));
        genderItem.setDetail(getString(account.getGender().nameRes));
        heightItem.setDetail(unitSystem.formatHeight(account.getHeight()).toString());
        weightItem.setDetail(unitSystem.formatMass(account.getWeight()).toString());
        timeZoneItem.setDetail(DateTimeZone.forOffsetMillis(account.getTimeZoneOffset()).getName(DateTimeUtils.currentTimeMillis()));

        this.currentAccount = account;
    }

    public void bindAccountPreferences(@NonNull HashMap<AccountPreference.Key, Object> settings) {
        boolean enhancedAudio = (boolean) settings.get(AccountPreference.Key.ENHANCED_AUDIO);
        enhancedAudioItem.setChecked(enhancedAudio);
    }

    public void presentError(Throwable e) {
        hideLoadingIndicator();
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }

    //endregion


    //region Basic Info

    public void changeName() {
        FragmentNavigation navigation = (FragmentNavigation) getActivity();
        ChangeNameFragment fragment = new ChangeNameFragment();
        fragment.setTargetFragment(this, 0x00);
        navigation.pushFragment(fragment, getString(R.string.action_change_name), true);
    }

    public void changeEmail() {
        FragmentNavigation navigation = (FragmentNavigation) getActivity();
        ChangeEmailFragment fragment = new ChangeEmailFragment();
        fragment.setTargetFragment(this, REQUEST_CODE_PASSWORD);
        navigation.pushFragment(fragment, getString(R.string.title_change_email), true);
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


    //region Preferences

    public void changeEnhancedAudio() {
        boolean newSetting = !enhancedAudioItem.isChecked();
        AccountPreference update = new AccountPreference(AccountPreference.Key.ENHANCED_AUDIO);
        update.setEnabled(newSetting);
        enhancedAudioItem.setChecked(newSetting);

        showLoadingIndicator();
        bindAndSubscribe(accountPresenter.updatePreference(update),
                         ignored -> hideLoadingIndicator(),
                         e -> {
                             enhancedAudioItem.setChecked(!newSetting);
                             presentError(e);
                         });
    }

    //endregion


    //region Actions

    public void signOut() {
        Analytics.trackEvent(Analytics.TopView.EVENT_SIGN_OUT, null);

        SenseAlertDialog builder = new SenseAlertDialog(getActivity());
        builder.setTitle(R.string.dialog_title_log_out);
        builder.setMessage(R.string.dialog_message_log_out);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(R.string.action_log_out, (dialog, which) -> {
            accountPresenter.logOut();
            Analytics.trackEvent(Analytics.Global.EVENT_SIGNED_OUT, null);
        });
        builder.setDestructive(true);
        builder.show();
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
        saveAccount();
    }

    private void saveAccount() {
        coordinator.postOnResume(() -> {
            showLoadingIndicator();
            bindAndSubscribe(accountPresenter.saveAccount(currentAccount),
                             ignored -> hideLoadingIndicator(),
                             this::presentError);
        });
    }

    //endregion
}
