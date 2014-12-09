package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.EditorActionHandler;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class ChangePasswordFragment extends InjectionFragment {
    private EditText currentPassword;
    private EditText newPassword;
    private EditText confirmNewPassword;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        this.currentPassword = (EditText) view.findViewById(R.id.fragment_change_password_current);
        this.newPassword = (EditText) view.findViewById(R.id.fragment_change_password_new);
        this.confirmNewPassword = (EditText) view.findViewById(R.id.fragment_change_password_new_confirm);
        confirmNewPassword.setOnEditorActionListener(new EditorActionHandler(this::changePassword));

        Button submit = (Button) view.findViewById(R.id.fragment_change_password_submit);
        submit.setOnClickListener(ignored -> changePassword());

        return view;
    }


    private boolean popIfEmpty(EditText... editTexts) {
        for (EditText editText : editTexts) {
            if (TextUtils.isEmpty(editText.getText())) {
                editText.requestFocus();
                animate(editText).simplePop(1.4f).start();
                return true;
            }
        }

        return false;
    }

    public void changePassword() {
        if (popIfEmpty(currentPassword, newPassword, confirmNewPassword)) {
            return;
        }

        if (newPassword.getText().length() <= 3) {
            ErrorDialogFragment errorDialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_invalid_register_password));
            errorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            newPassword.requestFocus();
            return;
        }

        if (!TextUtils.equals(newPassword.getText(), confirmNewPassword.getText())) {
            ErrorDialogFragment errorDialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_mismatching_new_passwords));
            errorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            return;
        }


    }
}
