package is.hello.sense.ui.fragments.onboarding;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.squareup.picasso.Picasso;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import javax.inject.Inject;

import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiEndpoint;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.DynamicApiEndpoint;
import is.hello.sense.api.fb.model.FacebookProfile;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.ErrorResponse;
import is.hello.sense.api.model.RegistrationError;
import is.hello.sense.api.model.v2.MultiDensityImage;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.interactors.FacebookInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.ProfileImageManager;
import is.hello.sense.ui.common.StatusBarColorProvider;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.FacebookInfoDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.LabelEditText;
import is.hello.sense.ui.widget.ProfileImageView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import retrofit.mime.TypedFile;
import rx.Observable;

public class RegisterFragment extends InjectionFragment
        implements StatusBarColorProvider, TextWatcher, ProfileImageManager.Listener {
    @Inject
    ApiService apiService;
    @Inject
    ApiEndpoint apiEndpoint;
    @Inject
    ApiSessionManager sessionManager;
    @Inject
    AccountInteractor accountPresenter;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    FacebookInteractor facebookPresenter;
    @Inject
    Picasso picasso;
    @Inject
    ProfileImageManager.Builder builder;

    private ProfileImageView profileImageView;
    private ProfileImageManager profileImageManager;
    private Button autofillFacebookButton;
    private Account account;
    private LabelEditText firstNameTextLET;
    private LabelEditText lastNameTextLET;
    private LabelEditText emailTextLET;
    private LabelEditText passwordTextLET;

    private Button nextButton;

    private LinearLayout credentialsContainer;
    private Uri imageUri = Uri.EMPTY;

    private final static int OPTION_FACEBOOK_DESCRIPTION = 0x66;
    private final static String ACCOUNT_INSTANCE_KEY = "account";
    private final static String URI_INSTANCE_KEY = "uri";

    //region Lifecycle

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_ACCOUNT, null);

            this.account = Account.createDefault();
        } else {
            if (savedInstanceState.containsKey(ACCOUNT_INSTANCE_KEY)) {
                this.account = (Account) savedInstanceState.getSerializable(ACCOUNT_INSTANCE_KEY);
            }
            if (savedInstanceState.containsKey(URI_INSTANCE_KEY)) {
                this.imageUri = Uri.parse(savedInstanceState.getString(URI_INSTANCE_KEY));
            }
        }

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_register, container, false);

        this.credentialsContainer = (LinearLayout) view.findViewById(R.id.fragment_onboarding_register_credentials);
        AnimatorTemplate.DEFAULT.apply(credentialsContainer.getLayoutTransition());

        this.profileImageView = (ProfileImageView) view.findViewById(R.id.fragment_onboarding_register_profile_image);

        this.firstNameTextLET = (LabelEditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_first_name_let);
        firstNameTextLET.addTextChangedListener(this);

        this.lastNameTextLET = (LabelEditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_last_name_let);
        lastNameTextLET.addTextChangedListener(this);

        this.emailTextLET = (LabelEditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_email_let);
        emailTextLET.addTextChangedListener(this);

        this.passwordTextLET = (LabelEditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_password_let);
        passwordTextLET.addTextChangedListener(this);
        passwordTextLET.setOnEditorActionListener(new EditorActionHandler(this::register));

        this.nextButton = (Button) view.findViewById(R.id.fragment_onboarding_register_next);

        nextButton.setActivated(false);
        nextButton.setText(R.string.action_next);

        final FocusClickListener nextButtonClickListener = new FocusClickListener(credentialsContainer, stateSafeExecutor.bind(this::register));
        Views.setSafeOnClickListener(nextButton, stateSafeExecutor, nextButtonClickListener);

        autofillFacebookButton = (Button) view.findViewById(R.id.fragment_onboarding_register_import_facebook_button);
        Views.setSafeOnClickListener(autofillFacebookButton, stateSafeExecutor, (v) -> bindFacebookProfile(false));
        facebookPresenter.init();

        final ImageButton facebookInfoButton = (ImageButton) view.findViewById(R.id.fragment_onboarding_register_import_facebook_info_button);
        Views.setSafeOnClickListener(facebookInfoButton, stateSafeExecutor, v -> this.showFacebookInfoBottomSheet(true));

        profileImageManager = builder.build(this);

        final View.OnClickListener profileImageOnClickListener = (v) -> profileImageManager.showPictureOptions();
        Views.setSafeOnClickListener(profileImageView, stateSafeExecutor, profileImageOnClickListener);
        profileImageView.setButtonClickListener(stateSafeExecutor, profileImageOnClickListener);

        OnboardingToolbar.of(this, view).setWantsBackButton(true);

        if (BuildConfig.DEBUG) {
            final Button selectHost = new Button(getActivity());
            Styles.setTextAppearance(selectHost, R.style.AppTheme_Button_Borderless_Accent_Bounded);
            selectHost.setBackgroundResource(R.drawable.selectable_dark_bounded);
            selectHost.setGravity(Gravity.CENTER);
            final Observable<String> apiUrl =
                    preferences.observableString(DynamicApiEndpoint.PREF_API_ENDPOINT_OVERRIDE,
                                                 apiEndpoint.getUrl());
            bindAndSubscribe(apiUrl, selectHost::setText, Functions.LOG_ERROR);

            final int padding = getResources().getDimensionPixelSize(R.dimen.gap_small);
            selectHost.setPadding(padding, padding, padding, padding);

            Views.setSafeOnClickListener(selectHost, ignored -> {
                try {
                    startActivity(new Intent(getActivity(), Class.forName("is.hello.sense.debug.EnvironmentActivity")));
                } catch (final ClassNotFoundException e) {
                    Log.e(getClass().getSimpleName(), "Could not find environment activity", e);
                }
            });

            final LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                  ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.gap_small);
            credentialsContainer.addView(selectHost, layoutParams);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.showFacebookInfoBottomSheet(false);
        this.profileImageManager.hidePictureOptions();

        firstNameTextLET.removeTextChangedListener(this);
        this.firstNameTextLET = null;

        lastNameTextLET.removeTextChangedListener(this);
        this.lastNameTextLET = null;

        emailTextLET.removeTextChangedListener(this);
        this.emailTextLET = null;

        passwordTextLET.removeTextChangedListener(this);
        passwordTextLET.setOnEditorActionListener(null);
        this.passwordTextLET = null;

        this.nextButton.setOnClickListener(null);
        this.nextButton = null;
        this.credentialsContainer = null;

        this.profileImageView.setOnClickListener(null);
        this.profileImageView.setButtonClickListener(null);
        this.autofillFacebookButton.setOnClickListener(null);
        this.profileImageView = null;
        this.profileImageManager = null;
        this.autofillFacebookButton = null;
        this.facebookPresenter.logout();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ACCOUNT_INSTANCE_KEY, account);
        outState.putString(URI_INSTANCE_KEY, imageUri.toString());
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (profileImageManager.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        facebookPresenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        profileImageManager.onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    public int getStatusBarColor(@NonNull final Resources resources) {
        return ContextCompat.getColor(getActivity(), R.color.status_bar_grey);
    }

    @Override
    public void onStatusBarTransitionBegan(@ColorInt final int targetColor) {
    }

    @Override
    public void onStatusBarTransitionEnded(@ColorInt final int finalColor) {
    }

    //endregion


    //region Registration

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }

    private void displayRegistrationError(@NonNull final RegistrationError error) {
        clearRegistrationError();
        final LabelEditText affectedField;
        switch (error) {
            default:
            case UNKNOWN:
            case NAME_TOO_LONG:
            case NAME_TOO_SHORT: {
                //Todo if last name is to be validated this will need modification
                affectedField = firstNameTextLET;
                break;
            }

            case EMAIL_INVALID:
            case EMAIL_IN_USE: {
                affectedField = emailTextLET;
                break;
            }

            case PASSWORD_INSECURE:
            case PASSWORD_TOO_SHORT: {
                affectedField = passwordTextLET;
                break;
            }
        }
        affectedField.setError(error.messageRes);
        affectedField.requestFocus();
    }

    private void clearRegistrationError() {

        firstNameTextLET.removeError();
        lastNameTextLET.removeError();
        emailTextLET.removeError();
        passwordTextLET.removeError();
    }

    private boolean doCompleteValidation() {
        final CharSequence firstName = AccountInteractor.normalizeInput(firstNameTextLET.getInputText());
        final CharSequence lastName = AccountInteractor.normalizeInput(lastNameTextLET.getInputText());
        final CharSequence email = AccountInteractor.normalizeInput(emailTextLET.getInputText());
        final CharSequence password = passwordTextLET.getInputText();

        if (!AccountInteractor.validateName(firstName)) {
            displayRegistrationError(RegistrationError.NAME_TOO_SHORT);
            firstNameTextLET.requestFocus();
            return false;
        }

        //Currently we do not validate Last Name

        if (!AccountInteractor.validateEmail(email)) {
            displayRegistrationError(RegistrationError.EMAIL_INVALID);
            emailTextLET.requestFocus();
            return false;
        }

        if (!AccountInteractor.validatePassword(password)) {
            displayRegistrationError(RegistrationError.PASSWORD_TOO_SHORT);
            passwordTextLET.requestFocus();
            return false;
        }

        firstNameTextLET.setInputText(firstName.toString());
        lastNameTextLET.setInputText(lastName.toString());
        emailTextLET.setInputText(email.toString());
        clearRegistrationError();

        return true;
    }


    public void register() {
        if (!doCompleteValidation()) {
            return;
        }

        account.setFirstName(firstNameTextLET.getInputText());
        account.setLastName(lastNameTextLET.getInputText());
        account.setEmail(emailTextLET.getInputText());
        account.setPassword(passwordTextLET.getInputText());

        LoadingDialogFragment.show(getFragmentManager(),
                                   getString(R.string.dialog_loading_message),
                                   LoadingDialogFragment.OPAQUE_BACKGROUND);

        bindAndSubscribe(apiService.createAccount(account), this::login, error -> {
            LoadingDialogFragment.close(getFragmentManager());

            if (ApiException.statusEquals(error, 400)) {
                final ApiException apiError = (ApiException) error;
                final ErrorResponse errorResponse = apiError.getErrorResponse();
                if (errorResponse != null) {
                    final RegistrationError registrationError =
                            RegistrationError.fromString(errorResponse.getMessage());
                    displayRegistrationError(registrationError);

                    return;
                }
            }

            final ErrorDialogFragment.Builder errorDialogBuilder =
                    new ErrorDialogFragment.Builder(error, getActivity());

            if (ApiException.statusEquals(error, 409)) {

                displayRegistrationError(RegistrationError.EMAIL_IN_USE);
                emailTextLET.requestFocus();
                return;
            }

            final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    public void login(@NonNull final Account createdAccount) {
        final OAuthCredentials credentials = new OAuthCredentials(apiEndpoint,
                                                                  emailTextLET.getInputText(),
                                                                  passwordTextLET.getInputText());
        bindAndSubscribe(apiService.authorize(credentials), session -> {
            sessionManager.setSession(session);

            preferences.putLocalDate(PreferencesInteractor.ACCOUNT_CREATION_DATE,
                                     LocalDate.now());
            accountPresenter.pushAccountPreferences();

            Analytics.trackRegistration(session.getAccountId(),
                                        createdAccount.getFullName(),
                                        createdAccount.getEmail(),
                                        DateTime.now());

            account = createdAccount;
            profileImageManager.compressImage(imageUri);

        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentError(getActivity(), error);
        });
    }

    private void goToNextScreen() {
        getOnboardingActivity().showBirthday(
                account, true);
    }

    //endregion


    //region Next button state control
    //Todo include lastNameText non empty validation?
    private boolean isInputValidSimple() {
        return (!TextUtils.isEmpty(firstNameTextLET.getInputText()) &&
                TextUtils.getTrimmedLength(emailTextLET.getInputText()) > 0 &&
                !TextUtils.isEmpty(passwordTextLET.getInputText()));
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
    }

    @Override
    public void afterTextChanged(final Editable s) {
        final boolean isValid = isInputValidSimple();
        nextButton.setActivated(isValid);
        final int buttonText = isValid ? R.string.action_continue : R.string.action_next;
        nextButton.setText(buttonText);
    }

    //endregion

    //region Profile Image View
    private void updateProfileImage(@NonNull final Uri imageUri) {
        this.imageUri = imageUri;
        final int resizeDimen = profileImageView.getSizeDimen();
        picasso.load(imageUri)
               .resizeDimen(resizeDimen, resizeDimen)
               .centerCrop()
               .into(profileImageView);
        profileImageManager.addDeleteOption();
        profileImageManager.setShowOptions(true);
    }
    //endregion

    //region Facebook interactor
    private void bindFacebookProfile(final boolean onlyPhoto) {
        if (!facebookPresenter.subscriptionSubject.hasObservers()) {
            bindAndSubscribe(facebookPresenter.subscriptionSubject,
                             this::onFacebookProfileSuccess,
                             this::onFacebookProfileError);
        }

        facebookPresenter.login(RegisterFragment.this, onlyPhoto);
    }

    private void onFacebookProfileSuccess(@NonNull final FacebookProfile profile) {
        final String facebookImageUrl = profile.getPictureUrl();
        final String firstName = profile.getFirstName();
        final String lastName = profile.getLastName();
        final String email = profile.getEmail();
        if (!TextUtils.isEmpty(facebookImageUrl)) {
            updateProfileImage(Uri.parse(facebookImageUrl));
            profileImageManager.saveSource(Analytics.ProfilePhoto.Source.FACEBOOK);
        }
        if (!TextUtils.isEmpty(firstName)) {
            firstNameTextLET.setInputText(firstName);
            autofillFacebookButton.setEnabled(false);
        }
        if (!TextUtils.isEmpty(lastName)) {
            lastNameTextLET.setInputText(lastName);
        }
        if (!TextUtils.isEmpty(email)) {
            emailTextLET.setInputText(email);
        }

        profileImageManager.setShowOptions(true);

    }

    private void onFacebookProfileError(@NonNull final Throwable throwable) {
        handleError(throwable, getString(R.string.error_internet_connection_generic_message));
    }

    private void handleError(@NonNull final Throwable error, @NonNull final String errorMessage) {
        stateSafeExecutor.execute(() -> {
            profileImageManager.setShowOptions(true);
            if (getFragmentManager().findFragmentByTag(ErrorDialogFragment.TAG) == null) {
                ErrorDialogFragment.presentError(getActivity(), new Throwable(errorMessage), R.string.error_account_upload_photo_title);
            }
        });
    }

    private void showFacebookInfoBottomSheet(final boolean shouldShow) {
        FacebookInfoDialogFragment bottomSheet = (FacebookInfoDialogFragment) getFragmentManager().findFragmentByTag(FacebookInfoDialogFragment.TAG);
        if (bottomSheet != null && !shouldShow) {
            bottomSheet.dismissSafely();
        } else if(bottomSheet == null && shouldShow){
            bottomSheet = FacebookInfoDialogFragment.newInstance();
            bottomSheet.setTargetFragment(RegisterFragment.this, OPTION_FACEBOOK_DESCRIPTION);
            bottomSheet.showAllowingStateLoss(getFragmentManager(), FacebookInfoDialogFragment.TAG);
        }
    }

    //endregion

    //region Profile Image Manager Listener Methods

    @Override
    public void onImportFromFacebook() {
        bindFacebookProfile(true);
    }

    @Override
    public void onFromCamera(@NonNull final Uri imageUri) {
        updateProfileImage(imageUri);
    }

    @Override
    public void onFromGallery(@NonNull final Uri imageUri) {
        updateProfileImage(imageUri);
    }

    @Override
    public void onImageCompressedSuccess(@NonNull final TypedFile compressImage, @NonNull final Analytics.ProfilePhoto.Source source) {
        bindAndSubscribe(accountPresenter.updateProfilePicture(compressImage, Analytics.Onboarding.EVENT_CHANGE_PROFILE_PHOTO, source),
                         this::updateProfilePictureSuccess,
                         this::updateProfilePictureError);

    }

    @Override
    public void onImageCompressedError(@NonNull final Throwable e, @StringRes final int titleRes, @StringRes final int messageRes) {
        goToNextScreen();
    }

    @Override
    public void onRemove() {
        final int defaultDimen = profileImageView.getSizeDimen();
        picasso.cancelRequest(profileImageView);
        picasso.load(profileImageView.getDefaultProfileRes())
               .centerCrop()
               .resizeDimen(defaultDimen, defaultDimen)
               .into(profileImageView);
        Analytics.trackEvent(Analytics.Onboarding.EVENT_DELETE_PROFILE_PHOTO, null);
        profileImageManager.removeDeleteOption();
        profileImageManager.setShowOptions(true);
    }

    //endregion


    private void updateProfilePictureSuccess(@NonNull final MultiDensityImage compressedPhoto) {
        profileImageManager.clear();
        goToNextScreen();
    }

    private void updateProfilePictureError(@NonNull final Throwable e) {
        Analytics.trackError(e, "Onboarding photo upload api");
        profileImageManager.clear();
        goToNextScreen();
    }

    private static class FocusClickListener implements View.OnClickListener {

        private final ViewGroup container;
        private final
        @Nullable
        Runnable runOnActivatedCommand;

        public FocusClickListener(@NonNull final ViewGroup container) {
            this(container, null);
        }

        public FocusClickListener(@NonNull final ViewGroup container, @Nullable final Runnable runOnActivatedCommand) {
            this.container = container;
            this.runOnActivatedCommand = runOnActivatedCommand;
        }

        @Override
        public void onClick(@NonNull final View v) {
            if (!v.isActivated()) {
                final View focusedView = container.getFocusedChild();
                if (focusedView != null) {

                    final View nextFocusView = container.findViewById(focusedView.getNextFocusForwardId());
                    if (nextFocusView != null) {
                        nextFocusView.requestFocus();
                    }
                }
            } else if (runOnActivatedCommand != null) {
                runOnActivatedCommand.run();
            }
        }
    }
}
