package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import javax.inject.Inject;

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiEndpoint;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.PasswordUpdate;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.animation.MultiAnimator;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import rx.Observable;

import static is.hello.sense.ui.animation.MultiAnimator.animatorFor;

public class ChangePasswordFragment extends InjectionFragment {
    private static final String ARG_EMAIL = ChangePasswordFragment.class.getName() + ".ARG_EMAIL";

    @Inject
    ApiEndpoint apiEndpoint;
    @Inject ApiService apiService;
    @Inject ApiSessionManager apiSessionManager;

    private String email;

    private EditText currentPassword;
    private EditText newPassword;
    private EditText confirmNewPassword;

    public static ChangePasswordFragment newInstance(@NonNull String email) {
        ChangePasswordFragment changePasswordFragment = new ChangePasswordFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_EMAIL, email);
        changePasswordFragment.setArguments(arguments);

        return changePasswordFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.email = getArguments().getString(ARG_EMAIL);

        setRetainInstance(true);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_CHANGE_PASSWORD, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        this.currentPassword = (EditText) view.findViewById(R.id.fragment_change_password_current);
        this.newPassword = (EditText) view.findViewById(R.id.fragment_change_password_new);
        this.confirmNewPassword = (EditText) view.findViewById(R.id.fragment_change_password_new_confirm);
        confirmNewPassword.setOnEditorActionListener(new EditorActionHandler(this::changePassword));

        Button submit = (Button) view.findViewById(R.id.fragment_change_password_submit);
        Views.setSafeOnClickListener(submit, ignored -> changePassword());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.currentPassword = null;
        this.newPassword = null;
        this.confirmNewPassword = null;
    }

    private boolean popIfEmpty(EditText... editTexts) {
        for (EditText editText : editTexts) {
            if (TextUtils.isEmpty(editText.getText())) {
                editText.requestFocus();
                animatorFor(editText).simplePop(1.4f).start();
                return true;
            }
        }

        return false;
    }

    public void changePassword() {
        if (popIfEmpty(currentPassword, newPassword, confirmNewPassword)) {
            return;
        }

        if (!AccountPresenter.validatePassword(newPassword.getText())) {
            ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                    .withMessage(StringRef.from(R.string.error_account_password_too_short))
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
            newPassword.requestFocus();
            return;
        }

        if (!TextUtils.equals(newPassword.getText(), confirmNewPassword.getText())) {
            ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                    .withMessage(StringRef.from(R.string.error_mismatching_new_passwords))
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
            return;
        }

        LoadingDialogFragment.show(getFragmentManager(),
                null, LoadingDialogFragment.OPAQUE_BACKGROUND);
        PasswordUpdate passwordUpdate = new PasswordUpdate(currentPassword.getText().toString(), newPassword.getText().toString());
        bindAndSubscribe(apiService.changePassword(passwordUpdate),
                         ignored -> recreateSession(),
                         this::presentError);
    }

    public void recreateSession() {
        String password = newPassword.getText().toString();
        Observable<OAuthSession> authorize = apiService.authorize(new OAuthCredentials(apiEndpoint, email, password));
        bindAndSubscribe(authorize,
                         session -> {
                             apiSessionManager.setSession(session);
                             LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), null);
                             getFragmentManager().popBackStack();
                         },
                         this::presentError);
    }

    public void presentError(@Nullable Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e);
        if (ApiException.statusEquals(e, 409)) {
            errorDialogBuilder.withMessage(StringRef.from(R.string.error_message_current_pw_wrong));
        }

        ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }
}
