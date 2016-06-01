package is.hello.sense.ui.fragments.onboarding;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import java.util.ArrayList;

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
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.FacebookPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.permissions.ExternalStoragePermission;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.ProfileImageManager;
import is.hello.sense.ui.common.StatusBarColorProvider;
import is.hello.sense.ui.dialogs.BottomSheetDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.LabelEditText;
import is.hello.sense.ui.widget.ProfileImageView;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import is.hello.sense.util.Logger;
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
    AccountPresenter accountPresenter;
    @Inject
    PreferencesPresenter preferences;
    @Inject
    FacebookPresenter facebookPresenter;
    @Inject
    Picasso picasso;
    @Inject
    ProfileImageManager.Builder builder;

    private ProfileImageView profileImageView;
    private ProfileImageManager profileImageManager;
    private final ExternalStoragePermission externalStoragePermission;
    private Button autofillFacebookButton;
    private Account account;
    private LabelEditText firstNameTextLET;
    private LabelEditText lastNameTextLET;
    private LabelEditText emailTextLET;
    private LabelEditText passwordTextLET;

    private Button nextButton;

    private LinearLayout credentialsContainer;

    private final static int OPTION_FACEBOOK_DESCRIPTION = 0x66;

    //region Lifecycle

    public RegisterFragment(){
        super();
        this.externalStoragePermission = ExternalStoragePermission.forCamera(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_ACCOUNT, null);

            this.account = Account.createDefault();
        } else {
            this.account = (Account) savedInstanceState.getSerializable("account");
        }

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        Views.setSafeOnClickListener(nextButton, nextButtonClickListener);

        autofillFacebookButton = (Button) view.findViewById(R.id.fragment_onboarding_register_import_facebook_button);
        Views.setSafeOnClickListener(autofillFacebookButton, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bindFacebookProfile(false);
            }
        });
        facebookPresenter.init();

        final ImageButton facebookInfoButton = (ImageButton) view.findViewById(R.id.fragment_onboarding_register_import_facebook_info_button);
        Views.setSafeOnClickListener(facebookInfoButton, new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                final ArrayList<SenseBottomSheet.Option> options = new ArrayList<>(1);
                final CharSequence clickableLink = Styles.resolveSupportLinks(getActivity(),getResources().getString(R.string.facebook_oauth_description));
                options.add(new SenseBottomSheet.Option(OPTION_FACEBOOK_DESCRIPTION)
                                    .setDescription(clickableLink.toString())
                           );

                final BottomSheetDialogFragment bottomSheetDialogFragment = BottomSheetDialogFragment.newInstance(R.string.facebook_oauth_title,options);
                bottomSheetDialogFragment.setWantsBigTitle(true);
                bottomSheetDialogFragment.setTargetFragment(RegisterFragment.this,OPTION_FACEBOOK_DESCRIPTION);
                bottomSheetDialogFragment.showAllowingStateLoss(getFragmentManager(),BottomSheetDialogFragment.TAG);


            }
        });

        profileImageManager = builder.addFragmentListener(this).build();

        final View.OnClickListener profileImageOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(externalStoragePermission.isGranted()) {
                    profileImageManager.showPictureOptions();
                } else{
                    externalStoragePermission.requestPermissionWithDialogForCamera();
                }
            }
        };

        profileImageView.setOnClickListener(profileImageOnClickListener);
        profileImageView.addButtonListener(profileImageOnClickListener);

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
                } catch (ClassNotFoundException e) {
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

        firstNameTextLET.removeTextChangedListener(this);
        this.firstNameTextLET = null;

        lastNameTextLET.removeTextChangedListener(this);
        this.lastNameTextLET = null;

        emailTextLET.removeTextChangedListener(this);
        this.emailTextLET = null;

        passwordTextLET.removeTextChangedListener(this);
        this.passwordTextLET = null;

        this.nextButton = null;
        this.credentialsContainer = null;

        this.profileImageView = null;
        this.profileImageManager = null;
        this.autofillFacebookButton = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("account", account);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebookPresenter.onActivityResult(requestCode, resultCode, data);
        profileImageManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public int getStatusBarColor(@NonNull Resources resources) {
        return ContextCompat.getColor(getActivity(), R.color.status_bar_grey);
    }

    @Override
    public void onStatusBarTransitionBegan(@ColorInt int targetColor) {
    }

    @Override
    public void onStatusBarTransitionEnded(@ColorInt int finalColor) {
    }

    //endregion


    //region Registration

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }

    private void displayRegistrationError(@NonNull RegistrationError error) {
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
        final CharSequence firstName = AccountPresenter.normalizeInput(firstNameTextLET.getInputText());
        final CharSequence lastName = AccountPresenter.normalizeInput(lastNameTextLET.getInputText());
        final CharSequence email = AccountPresenter.normalizeInput(emailTextLET.getInputText());
        final CharSequence password = passwordTextLET.getInputText();

        if (!AccountPresenter.validateName(firstName)) {
            displayRegistrationError(RegistrationError.NAME_TOO_SHORT);
            firstNameTextLET.requestFocus();
            return false;
        }

        //Currently we do not validate Last Name

        if (!AccountPresenter.validateEmail(email)) {
            displayRegistrationError(RegistrationError.EMAIL_INVALID);
            emailTextLET.requestFocus();
            return false;
        }

        if (!AccountPresenter.validatePassword(password)) {
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
                    new ErrorDialogFragment.Builder(error, getResources());

            if (ApiException.statusEquals(error, 409)) {

                displayRegistrationError(RegistrationError.EMAIL_IN_USE);
                emailTextLET.requestFocus();
                return;
            }

            final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    public void login(@NonNull Account createdAccount) {
        final OAuthCredentials credentials = new OAuthCredentials(apiEndpoint,
                                                                  emailTextLET.getInputText(),
                                                                  passwordTextLET.getInputText());
        bindAndSubscribe(apiService.authorize(credentials), session -> {
            sessionManager.setSession(session);

            preferences.putLocalDate(PreferencesPresenter.ACCOUNT_CREATION_DATE,
                                     LocalDate.now());
            accountPresenter.pushAccountPreferences();

            Analytics.trackRegistration(session.getAccountId(),
                                        createdAccount.getFirstName(),
                                        createdAccount.getEmail(),
                                        DateTime.now());

            getOnboardingActivity().showBirthday(createdAccount, true);
        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentError(getActivity(), error);
        });
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
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        final boolean isValid = isInputValidSimple();
        nextButton.setActivated(isValid);
        final int buttonText = isValid ? R.string.action_continue : R.string.action_next;
        nextButton.setText(buttonText);
    }

    //endregion

    //region Profile Image View
    private void updateProfileImage(@NonNull final String imagePath){
        final int resizeDimen = profileImageView.getSizeDimen();
        picasso.load(imagePath)
               .resizeDimen(resizeDimen, resizeDimen)
               .centerCrop()
               .into(profileImageView);
    }
    //endregion

    //region Facebook presenter
    public void bindFacebookProfile(@NonNull final Boolean onlyPhoto){
        bindAndSubscribe(facebookPresenter.profile,
                         this::onFacebookProfileSuccess,
                         this::onFacebookProfileError);

        facebookPresenter.login(RegisterFragment.this, onlyPhoto);
    }

    private void onFacebookProfileSuccess(FacebookProfile profile) {
        final String facebookImageUrl = profile.getPictureUrl();
        final String firstName = profile.getFirstName();
        final String lastName = profile.getLastName();
        final String email = profile.getEmail();
        if(facebookImageUrl != null) {
            updateProfileImage(facebookImageUrl);
            profileImageManager.setImageUri(Uri.parse(facebookImageUrl));
        }
        if(firstName != null) firstNameTextLET.setInputText(firstName);
        if(lastName != null) lastNameTextLET.setInputText(lastName);
        if(email != null){
            emailTextLET.setInputText(email);
            autofillFacebookButton.setEnabled(false); //we know this was through autofill profile button
        }
        //Todo should? passwordTextLET.requestFocus();
    }

    private void onFacebookProfileError(Throwable throwable) {
        Logger.error(getClass().getSimpleName(), "failed to fetch fb image", throwable);
    }
    //endregion

    //region Profile Image Manager Listener Methods

    @Override
    public void onImportFromFacebook() {
        bindFacebookProfile(true);
    }

    @Override
    public void onFromCamera(String imageUriString) {
        updateProfileImage(imageUriString);
    }

    @Override
    public void onFromGallery(String imageUriString) {
        updateProfileImage(imageUriString);
    }

    @Override
    public void onUploadReady(TypedFile imageFile) {

    }

    @Override
    public void onRemove() {
        final int defaultDimen = profileImageView.getSizeDimen();
        picasso.cancelRequest(profileImageView);
        picasso.load(profileImageView.getDefaultProfileRes())
                .centerCrop()
                .resizeDimen(defaultDimen, defaultDimen)
                .into(profileImageView);
        facebookPresenter.logout();
    }

    //endregion

    public static class FocusClickListener implements View.OnClickListener {

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
