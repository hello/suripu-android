package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;

import org.joda.time.LocalDate;

import java.util.EnumSet;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.fb.model.FacebookProfile;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.api.model.v2.MultiDensityImage;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.interactors.FacebookInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.ui.adapter.AccountSettingsRecyclerAdapter;
import is.hello.sense.ui.adapter.SettingsRecyclerAdapter;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ProfileImageManager;
import is.hello.sense.ui.common.ScrollEdge;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterBirthdayFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterGenderFragment;
import is.hello.sense.ui.fragments.onboarding.RegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.RegisterWeightFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.handholding.TutorialOverlayView;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.recycler.InsetItemDecoration;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.DateFormatter;
import retrofit.mime.TypedFile;

public class AccountSettingsFragment extends InjectionFragment
        implements AccountEditor.Container, ProfileImageManager.Listener {
    private static final int REQUEST_CODE_PASSWORD = 0x20;
    private static final int REQUEST_CODE_ERROR = 0xE3;
    private static final String CURRENT_ACCOUNT_INSTANCE_KEY = "currentAccount";

    @Inject
    Picasso picasso;
    @Inject
    AccountInteractor accountPresenter;
    @Inject
    DateFormatter dateFormatter;
    @Inject
    UnitFormatter unitFormatter;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    FacebookInteractor facebookPresenter;
    @Inject
    ProfileImageManager.Builder builder;

    private final AccountSettingsRecyclerAdapter.CircleItem profilePictureItem =
            new AccountSettingsRecyclerAdapter.CircleItem(
                stateSafeExecutor.bind(this::changePicture)
            );

    private SettingsRecyclerAdapter.DetailItem nameItem;
    private SettingsRecyclerAdapter.DetailItem emailItem;
    private SettingsRecyclerAdapter.DetailItem birthdayItem;
    private SettingsRecyclerAdapter.DetailItem genderItem;
    private SettingsRecyclerAdapter.DetailItem heightItem;
    private SettingsRecyclerAdapter.DetailItem weightItem;

    private SettingsRecyclerAdapter.ToggleItem enhancedAudioItem;


    @Nullable
    private Account.Preferences accountPreferences;
    private RecyclerView recyclerView;
    private ProfileImageManager profileImageManager;
    private Account currentAccount;
    private ProgressBar loadingIndicator;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.currentAccount = (Account) savedInstanceState.getSerializable(CURRENT_ACCOUNT_INSTANCE_KEY);
        } else {
            Analytics.trackEvent(Analytics.Backside.EVENT_ACCOUNT, null);
        }

        accountPresenter.update();
        addPresenter(accountPresenter);
        addPresenter(facebookPresenter);
        //Required so that it is retained after exiting on home button
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.static_recycler, container, false);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.static_recycler_view_loading);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.static_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     EnumSet.of(ScrollEdge.TOP), FadingEdgesItemDecoration.Style.STRAIGHT));

        final AccountSettingsRecyclerAdapter adapter = new AccountSettingsRecyclerAdapter(getActivity(), picasso);

        final int verticalPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);
        final int sectionPadding = resources.getDimensionPixelSize(R.dimen.gap_medium);
        final InsetItemDecoration decoration = new InsetItemDecoration();
        recyclerView.addItemDecoration(decoration);

        decoration.addTopInset(adapter.getItemCount(), verticalPadding);

        adapter.add(profilePictureItem);
        nameItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.missing_data_placeholder), this::changeName, R.id.fragment_account_settings_name);
        nameItem.setIcon(R.drawable.icon_settings_name, R.string.label_name);
        adapter.add(nameItem);
        emailItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.missing_data_placeholder), this::changeEmail);
        emailItem.setIcon(R.drawable.icon_settings_email, R.string.label_email);
        adapter.add(emailItem);

        decoration.addBottomInset(adapter.getItemCount(), sectionPadding);
        final SettingsRecyclerAdapter.DetailItem passwordItem =
                new SettingsRecyclerAdapter.DetailItem(getString(R.string.title_change_password),
                                                       this::changePassword);
        passwordItem.setIcon(R.drawable.icon_settings_lock, R.string.label_password);
        adapter.add(passwordItem);
        birthdayItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.label_dob), this::changeBirthDate);
        birthdayItem.setIcon(R.drawable.icon_settings_calendar, R.string.label_dob);
        adapter.add(birthdayItem);
        this.genderItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.label_gender),
                                                                 this::changeGender);
        genderItem.setIcon(R.drawable.icon_settings_gender, R.string.label_gender);
        adapter.add(genderItem);

        this.heightItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.label_height),
                                                                 this::changeHeight);
        heightItem.setIcon(R.drawable.icon_settings_height, R.string.label_height);
        adapter.add(heightItem);

        this.weightItem = new SettingsRecyclerAdapter.DetailItem(getString(R.string.label_weight),
                                                                 this::changeWeight);
        weightItem.setIcon(R.drawable.icon_settings_weight, R.string.label_weight);
        adapter.add(weightItem);

        decoration.addTopInset(adapter.getItemCount(), sectionPadding);
        this.enhancedAudioItem = new SettingsRecyclerAdapter.ToggleItem(getString(R.string.label_enhanced_audio),
                                                                        this::changeEnhancedAudio);
        adapter.add(enhancedAudioItem);

        adapter.add(new SettingsRecyclerAdapter.TextItem(getString(R.string.info_enhanced_audio), null));

        decoration.addItemInset(adapter.getItemCount(), new Rect(0, sectionPadding, 0, verticalPadding));
        final SettingsRecyclerAdapter.DetailItem signOutItem =
                new SettingsRecyclerAdapter.DetailItem(getString(R.string.action_log_out),
                                                       this::signOut);
        signOutItem.setIcon(R.drawable.icon_settings_signout, R.string.action_log_out);
        adapter.add(signOutItem);

        recyclerView.setAdapter(adapter);

        loadingIndicator.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);

        facebookPresenter.init();

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(accountPresenter.account,
                         this::bindAccount,
                         this::accountUnavailable);

        bindAndSubscribe(accountPresenter.preferences(),
                         this::bindAccountPreferences,
                         Functions.LOG_ERROR);

        profileImageManager = builder.setFragmentListener(this)
                                     .build();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        profileImageManager.hidePictureOptions();

        this.loadingIndicator = null;
        this.nameItem = null;
        this.emailItem = null;
        this.genderItem = null;
        this.birthdayItem = null;
        this.heightItem = null;
        this.weightItem = null;
        this.enhancedAudioItem = null;

        this.recyclerView = null;

        this.profileImageManager = null;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(CURRENT_ACCOUNT_INSTANCE_KEY, currentAccount);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (profileImageManager.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        facebookPresenter.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_PASSWORD) {
            accountPresenter.update();
        } else if (requestCode == REQUEST_CODE_ERROR) {
            getActivity().finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        profileImageManager.onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    //endregion


    //region Glue

    public FragmentNavigationActivity getNavigationContainer() {
        return (FragmentNavigationActivity) getActivity();
    }

    private void showLoadingIndicator() {
        recyclerView.setVisibility(View.INVISIBLE);
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        recyclerView.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.GONE);
    }

    //endregion


    //region Binding Data

    public void bindAccount(@NonNull final Account account) {
        final String photoUrl = account.getProfilePhotoUrl(getResources());
        if (photoUrl.isEmpty()) {
            profileImageManager.removeDeleteOption();
        } else {
            profileImageManager.addDeleteOption();
        }
        profilePictureItem.setValue(photoUrl);
        nameItem.setText(account.getFullName());
        emailItem.setText(account.getEmail());

        birthdayItem.setValue(dateFormatter.formatAsLocalizedDate(account.getBirthDate()));
        genderItem.setValue(getString(account.getGender().nameRes));

        final CharSequence weight = unitFormatter.formatWeight(account.getWeight());
        weightItem.setValue(weight.toString());

        final CharSequence height = unitFormatter.formatHeight(account.getHeight());
        heightItem.setValue(height.toString());

        this.currentAccount = account;

        hideLoadingIndicator();

        showTutorialHelperIfNeeded(account.getCreated());

    }

    public void accountUnavailable(final Throwable e) {
        loadingIndicator.setVisibility(View.GONE);
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getActivity()).build();
        errorDialogFragment.setTargetFragment(this, REQUEST_CODE_ERROR);
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    public void bindAccountPreferences(@NonNull final Account.Preferences preferences) {
        this.accountPreferences = preferences;
        enhancedAudioItem.setValue(preferences.enhancedAudioEnabled);
    }

    private void showTutorialHelperIfNeeded(@NonNull final LocalDate createdAt) {
        if (Tutorial.TAP_NAME.shouldShow(getActivity()) && createdAt.isBefore(Constants.RELEASE_DATE_FOR_LAST_NAME)) {
            final TutorialOverlayView overlayView = new TutorialOverlayView(getActivity(), Tutorial.TAP_NAME);
            overlayView.setAnchorContainer(getView());
            getAnimatorContext().runWhenIdle(() -> overlayView.postShow(R.id.static_recycler_container)
                                            );
        }
    }

    //endregion


    //region Basic Info
    private void changePicture() {
        if (profileImageManager != null) {
            profileImageManager.showPictureOptions();
        }
    }

    public void changeName() {
        final ChangeNameFragment fragment = new ChangeNameFragment();
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.action_change_name), true);
    }

    public void changeEmail() {
        final ChangeEmailFragment fragment = new ChangeEmailFragment();
        fragment.setTargetFragment(this, REQUEST_CODE_PASSWORD);
        getNavigationContainer().pushFragmentAllowingStateLoss(fragment, getString(R.string.title_change_email), true);
    }

    public void changePassword() {
        final ChangePasswordFragment fragment = ChangePasswordFragment.newInstance(currentAccount.getEmail());
        getNavigationContainer().pushFragmentAllowingStateLoss(fragment, getString(R.string.title_change_password), true);
    }

    //endregion


    //region Demographics

    public void changeBirthDate() {
        final OnboardingRegisterBirthdayFragment fragment = new OnboardingRegisterBirthdayFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_dob), true);
    }

    public void changeGender() {
        final OnboardingRegisterGenderFragment fragment = new OnboardingRegisterGenderFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_gender), true);
    }

    public void changeHeight() {
        final RegisterHeightFragment fragment = new RegisterHeightFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_height), true);
    }

    public void changeWeight() {
        final RegisterWeightFragment fragment = new RegisterWeightFragment();
        AccountEditor.setWantsSkipButton(fragment, false);
        fragment.setTargetFragment(this, 0x00);
        getNavigationContainer().overlayFragmentAllowingStateLoss(fragment, getString(R.string.label_weight), true);
    }

    //endregion


    //region Preferences

    public void changeEnhancedAudio() {
        if (accountPreferences == null) {
            return;
        }

        accountPreferences.enhancedAudioEnabled = !enhancedAudioItem.getValue();
        enhancedAudioItem.setValue(accountPreferences.enhancedAudioEnabled);

        showLoadingIndicator();
        bindAndSubscribe(accountPresenter.updatePreferences(accountPreferences),
                         ignored -> {
                             preferences.edit()
                                        .putBoolean(PreferencesInteractor.ENHANCED_AUDIO_ENABLED,
                                                    accountPreferences.enhancedAudioEnabled)
                                        .apply();
                             hideLoadingIndicator();
                         },
                         e -> {
                             accountPreferences.enhancedAudioEnabled = !accountPreferences.enhancedAudioEnabled;
                             enhancedAudioItem.setValue(accountPreferences.enhancedAudioEnabled);
                             accountUnavailable(e);
                         });
    }

    //endregion


    //region Actions

    public void signOut() {
        Analytics.trackEvent(Analytics.Backside.EVENT_SIGN_OUT, null);

        final SenseAlertDialog signOutDialog = new SenseAlertDialog(getActivity());
        signOutDialog.setTitle(R.string.dialog_title_log_out);
        signOutDialog.setMessage(R.string.dialog_message_log_out);
        signOutDialog.setNegativeButton(android.R.string.cancel, null);
        signOutDialog.setPositiveButton(R.string.action_log_out, (dialog, which) -> {
            Analytics.trackEvent(Analytics.Global.EVENT_SIGNED_OUT, null);
            // Let the dialog finish dismissing before we block the main thread.
            recyclerView.post(() -> {
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
    public void onAccountUpdated(@NonNull final SenseFragment updatedBy) {
        stateSafeExecutor.execute(() -> {
            LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(accountPresenter.saveAccount(currentAccount),
                             ignored -> {
                                 if (updatedBy instanceof Analytics.OnEventListener) {
                                     ((Analytics.OnEventListener) updatedBy).onSuccess();
                                 }
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


    //region ProfileImageManagerListener methods

    @Override
    public void onImportFromFacebook() {
        profileImageManager.setShowOptions(false);
        if (!facebookPresenter.profile.hasObservers()) {
            bindAndSubscribe(facebookPresenter.profile,
                             this::getFacebookProfileSuccess,
                             this::getFacebookProfileError);
        }
        facebookPresenter.login(this);
    }

    @Override
    public void onFromCamera(@NonNull final Uri imageUri) {
        showProfileLoadingIndicator(true);
        profileImageManager.compressImage(imageUri);
    }

    @Override
    public void onFromGallery(@NonNull final Uri imageUri) {
        showProfileLoadingIndicator(true);
        profileImageManager.compressImage(imageUri);
    }

    @Override
    public void onImageCompressedSuccess(@NonNull final TypedFile compressedImage, @NonNull final Analytics.ProfilePhoto.Source source) {
        bindAndSubscribe(accountPresenter.updateProfilePicture(compressedImage, Analytics.Account.EVENT_CHANGE_PROFILE_PHOTO, source),
                         this::updateProfilePictureSuccess,
                         this::updateProfilePictureError);
    }

    @Override
    public void onImageCompressedError(@NonNull final Throwable e, @StringRes final int titleRes, @StringRes final int messageRes) {
        handleError(e, titleRes, messageRes);
    }

    @Override
    public void onRemove() {
        bindAndSubscribe(accountPresenter.deleteProfilePicture(),
                         this::removePhotoSuccess,
                         this::removePhotoError);
    }

    private void getFacebookProfileSuccess(@NonNull final FacebookProfile profile) {
        final String fbImageUri = profile.getPictureUrl();
        if (!TextUtils.isEmpty(fbImageUri)) {
            final Uri newUri = Uri.parse(fbImageUri);
            showProfileLoadingIndicator(true);
            profileImageManager.compressImage(newUri);
        } else {
            profileImageManager.setShowOptions(true);
        }
    }

    private void getFacebookProfileError(final Throwable error) {
        handleError(error, R.string.error_account_upload_photo_title, R.string.error_internet_connection_generic_message);
    }

    private void  updateProfilePictureSuccess(@NonNull final MultiDensityImage compressedPhoto) {
        showProfileLoadingIndicator(false);
        currentAccount.setProfilePhoto(compressedPhoto);
        profileImageManager.addDeleteOption();
        profileImageManager.clear();
        profilePictureItem.setValue(currentAccount.getProfilePhotoUrl(getResources()));
    }

    private void updateProfilePictureError(@NonNull final Throwable e) {
        //restore previous saved photo and refresh view
        currentAccount.setProfilePhoto(currentAccount.getProfilePhoto());
        bindAccount(currentAccount);
        onImageCompressedError(e, R.string.error_account_upload_photo_title, R.string.error_account_upload_photo_message);
        profileImageManager.clear();
    }

    private void removePhotoSuccess(final VoidResponse response) {
        showProfileLoadingIndicator(false);
        profileImageManager.removeDeleteOption();
        currentAccount.setProfilePhoto(null);
        bindAccount(currentAccount);
        Analytics.trackEvent(Analytics.Account.EVENT_DELETE_PROFILE_PHOTO, null);

    }

    private void removePhotoError(@NonNull final Throwable e) {
        handleError(e, R.string.error_account_remove_photo_title, R.string.error_account_remove_photo_message);
    }


    private void handleError(@NonNull final Throwable error, @StringRes final int titleRes, @StringRes final int messageRes) {
        stateSafeExecutor.execute(() -> {
            showProfileLoadingIndicator(false);
            if (getFragmentManager().findFragmentByTag(ErrorDialogFragment.TAG) == null) {
                ErrorDialogFragment.presentError(getActivity(), new Throwable(getString(messageRes)), titleRes);
            }
        });
    }
    // endregion

    private void showProfileLoadingIndicator(final boolean show) {
        if (getView() != null) {
            final View progressBar = getView().findViewById(R.id.item_profile_progress_bar);
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                profileImageManager.setShowOptions(!show);
                return;
            }
        }
        profileImageManager.setShowOptions(show);
    }

}
