package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.widget.LabelEditText;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.EditorActionHandler;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class ChangeNameFragment extends SenseFragment {
    private LabelEditText firstNameLET;
    private LabelEditText lastNameLET;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_name, container, false);

        this.firstNameLET = (LabelEditText) view.findViewById(R.id.fragment_change_first_name_let);
        firstNameLET.setOnEditorActionListener(new EditorActionHandler(() -> submit(firstNameLET)));
        firstNameLET.setInputText(AccountEditor.getContainer(this).getAccount().getFirstName());

        this.lastNameLET = (LabelEditText) view.findViewById(R.id.fragment_change_last_name_let);
        lastNameLET.setOnEditorActionListener(new EditorActionHandler(() -> submit(lastNameLET)));
        lastNameLET.setInputText(AccountEditor.getContainer(this).getAccount().getLastName());

        Button submit = (Button) view.findViewById(R.id.fragment_change_name_submit);
        Views.setSafeOnClickListener(submit, this::submit);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.firstNameLET = null;
        this.lastNameLET = null;
    }

    public void submit(@NonNull View sender) {
        final String firstName = AccountPresenter.normalizeInput(firstNameLET.getInputText());
        final String lastName = AccountPresenter.normalizeInput(lastNameLET.getInputText());
        firstNameLET.setInputText(firstName);
        lastNameLET.setInputText(lastName);
        //Todo Currently no validation for lastName
        // Todo suggest updating the error validation animation
        if (!AccountPresenter.validateName(firstName)) {
            firstNameLET.setError(R.string.invalid_first_name);
            firstNameLET.requestFocus();
            animatorFor(firstNameLET)
                    .scale(1.4f)
                    .addOnAnimationCompleted(finished -> {
                        if (finished) {
                            animatorFor(firstNameLET)
                                    .scale(1.0f)
                                    .start();
                        }
                    })
                    .start();
            return;
        }
        firstNameLET.removeError();

        final AccountEditor.Container container = AccountEditor.getContainer(this);
        container.getAccount().setFirstName(firstName);
        container.getAccount().setLastName(lastName);
        container.onAccountUpdated(this);
    }
}
