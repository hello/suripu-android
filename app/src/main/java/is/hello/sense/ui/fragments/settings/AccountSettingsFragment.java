package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterBirthdayFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterGenderFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;

public class AccountSettingsFragment extends InjectionFragment
        implements AdapterView.OnItemClickListener, AccountEditor.Container {
    private static final int REQUEST_CODE_PASSWORD = 0x20;
    private static final int REQUEST_CODE_ERROR = 0xE3;

    @Inject AccountPresenter accountPresenter;
    @Inject DateFormatter dateFormatter;
    @Inject UnitFormatter unitFormatter;
    @Inject PreferencesPresenter preferences;

    private ProgressBar loadingIndicator;

    private StaticItemAdapter.TextItem nameItem;
    private StaticItemAdapter.TextItem emailItem;

    private StaticItemAdapter.TextItem birthdayItem;
    private StaticItemAdapter.TextItem genderItem;
    private StaticItemAdapter.TextItem heightItem;
    private StaticItemAdapter.TextItem weightItem;

    private StaticItemAdapter.CheckItem enhancedAudioItem;

    private Account currentAccount;
    private @Nullable Account.Preferences accountPreferences;
    private ListView listView;
    private StaticItemAdapter adapter;


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

        this.adapter = new StaticItemAdapter(getActivity());
        adapter.setEllipsize(TextUtils.TruncateAt.END);

        adapter.addSectionTitle(R.string.title_info);
        this.nameItem = adapter.addTextItem(R.string.label_name, R.string.missing_data_placeholder, this::changeName);
        this.emailItem = adapter.addTextItem(R.string.label_email, R.string.missing_data_placeholder, this::changeEmail);
        adapter.addTextItem(R.string.title_change_password, R.string.detail_change_password, this::changePassword);

        adapter.addSectionTitle(R.string.title_demographics);
        this.birthdayItem = adapter.addTextItem(R.string.label_dob, R.string.missing_data_placeholder, this::changeBirthDate);
        this.genderItem = adapter.addTextItem(R.string.label_gender, R.string.missing_data_placeholder, this::changeGender);
        this.heightItem = adapter.addTextItem(R.string.label_height, R.string.missing_data_placeholder, this::changeHeight);
        this.weightItem = adapter.addTextItem(R.string.label_weight, R.string.missing_data_placeholder, this::changeWeight);

        adapter.addSectionTitle(R.string.label_options);
        this.enhancedAudioItem = adapter.addCheckItem(R.string.label_enhanced_audio, false, this::changeEnhancedAudio);
        adapter.addFooterItem(R.string.info_enhanced_audio);

        adapter.addSectionDivider();
        adapter.addTextItem(R.string.action_log_out, 0, this::signOut);

        listView.setAdapter(adapter);

        loadingIndicator.setVisibility(View.VISIBLE);
        listView.setVisibility(View.INVISIBLE);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(accountPresenter.account,
                         this::bindAccount,
                         this::accountUnavailable);

        bindAndSubscribe(accountPresenter.preferences(),
                         this::bindAccountPreferences,
                         Functions.LOG_ERROR);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.loadingIndicator = null;

        this.nameItem = null;
        this.emailItem = null;

        this.birthdayItem = null;
        this.genderItem = null;
        this.heightItem = null;
        this.weightItem = null;

        this.enhancedAudioItem = null;

        this.listView = null;
        this.adapter = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("currentAccount", currentAccount);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PASSWORD && resultCode == Activity.RESULT_OK) {
            accountPresenter.update();
        } else if (requestCode == REQUEST_CODE_ERROR && resultCode == Activity.RESULT_OK) {
            getActivity().finish();
        }
    }

    //endregion


    //region Glue

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (currentAccount == null) {
            return;
        }

        adapter.onItemClick(adapterView, view, position, id);
    }

    public FragmentNavigationActivity getNavigationContainer() {
        return (FragmentNavigationActivity) getActivity();
    }

    private void showLoadingIndicator() {
        listView.setVisibility(View.INVISIBLE);
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        listView.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.GONE);
    }

    //endregion


    //region Binding Data

    public void bindAccount(@NonNull Account account) {
        nameItem.setDetail(account.getName());
        emailItem.setDetail(account.getEmail());

        birthdayItem.setDetail(dateFormatter.formatAsLocalizedDate(account.getBirthDate()));
        genderItem.setDetail(getString(account.getGender().nameRes));

        final CharSequence weight = unitFormatter.formatWeight(account.getWeight());
        weightItem.setDetail(weight.toString());

        final CharSequence height = unitFormatter.formatHeight(account.getHeight());
        heightItem.setDetail(height.toString());

        this.currentAccount = account;

        hideLoadingIndicator();
    }

    public void accountUnavailable(Throwable e) {
        loadingIndicator.setVisibility(View.GONE);
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getResources()).build();
        errorDialogFragment.setTargetFragment(this, REQUEST_CODE_ERROR);
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    public void bindAccountPreferences(@NonNull Account.Preferences preferences) {
        this.accountPreferences = preferences;
        enhancedAudioItem.setChecked(preferences.enhancedAudioEnabled);
    }

    //endregion


    //region Basic Info

    public void changeName(@NonNull StaticItemAdapter.TextItem item) {
        final ChangeNameFragment fragment = new ChangeNameFragment();
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.action_change_name), true);
    }

    public void changeEmail(@NonNull StaticItemAdapter.TextItem item) {
        final ChangeEmailFragment fragment = new ChangeEmailFragment();
        fragment.setTargetFragment(this, REQUEST_CODE_PASSWORD);
        getNavigationContainer().pushFragmentAllowingStateLoss(fragment, getString(R.string.title_change_email), true);
    }

    public void changePassword(@NonNull StaticItemAdapter.TextItem item) {
        final ChangePasswordFragment fragment = ChangePasswordFragment.newInstance(currentAccount.getEmail());
        getNavigationContainer().pushFragmentAllowingStateLoss(fragment, getString(R.string.title_change_password), true);
    }

    //endregion


    //region Demographics

    public void changeBirthDate(@NonNull StaticItemAdapter.TextItem item) {
        final OnboardingRegisterBirthdayFragment fragment = new OnboardingRegisterBirthdayFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_dob), true);
    }

    public void changeGender(@NonNull StaticItemAdapter.TextItem item) {
        final OnboardingRegisterGenderFragment fragment = new OnboardingRegisterGenderFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_gender), true);
    }

    public void changeHeight(@NonNull StaticItemAdapter.TextItem item) {
        final OnboardingRegisterHeightFragment fragment = new OnboardingRegisterHeightFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_height), true);
    }

    public void changeWeight(@NonNull StaticItemAdapter.TextItem item) {
        final OnboardingRegisterWeightFragment fragment = new OnboardingRegisterWeightFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_weight), true);
    }

    //endregion


    //region Preferences

    public void changeEnhancedAudio(@NonNull StaticItemAdapter.CheckItem item) {
        if (accountPreferences == null) {
            return;
        }

        accountPreferences.enhancedAudioEnabled = !enhancedAudioItem.isChecked();
        enhancedAudioItem.setChecked(accountPreferences.enhancedAudioEnabled);

        showLoadingIndicator();
        bindAndSubscribe(accountPresenter.updatePreferences(accountPreferences),
                         ignored -> {
                             preferences.edit()
                                        .putBoolean(PreferencesPresenter.ENHANCED_AUDIO_ENABLED,
                                                    accountPreferences.enhancedAudioEnabled)
                                        .apply();
                             hideLoadingIndicator();
                         },
                         e -> {
                             accountPreferences.enhancedAudioEnabled = !accountPreferences.enhancedAudioEnabled;
                             enhancedAudioItem.setChecked(accountPreferences.enhancedAudioEnabled);
                             accountUnavailable(e);
                         });
    }

    //endregion


    //region Actions

    public void signOut(@NonNull StaticItemAdapter.TextItem item) {
        Analytics.trackEvent(Analytics.TopView.EVENT_SIGN_OUT, null);

        final SenseAlertDialog signOutDialog = new SenseAlertDialog(getActivity());
        signOutDialog.setTitle(R.string.dialog_title_log_out);
        signOutDialog.setMessage(R.string.dialog_message_log_out);
        signOutDialog.setNegativeButton(android.R.string.cancel, null);
        signOutDialog.setPositiveButton(R.string.action_log_out, (dialog, which) -> {
            Analytics.trackEvent(Analytics.Global.EVENT_SIGNED_OUT, null);
            // Let the dialog finish dismissing before we block the main thread.
            listView.post(() -> {
                getActivity().finish();
                accountPresenter.logOut();
            });
        });
        signOutDialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        signOutDialog.show();
    }

    //endregion


    //region Updates

    @NonNull
    @Override
    public Account getAccount() {
        return currentAccount;
    }

    @Override
    public void onAccountUpdated(@NonNull SenseFragment updatedBy) {
        stateSafeExecutor.execute(() -> {
            LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(accountPresenter.saveAccount(currentAccount),
                             ignored -> {
                                 LoadingDialogFragment.close(getFragmentManager());
                                 updatedBy.getFragmentManager().popBackStackImmediate();
                             },
                             e -> {
                                 LoadingDialogFragment.close(getFragmentManager());
                                 ErrorDialogFragment.presentError(getActivity(), e);
                             });
        });
    }

    //endregion
}
