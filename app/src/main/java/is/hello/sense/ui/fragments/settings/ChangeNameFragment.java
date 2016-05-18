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
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.EditorActionHandler;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class ChangeNameFragment extends SenseFragment {
    private TextView firstNameText;
    private TextView lastNameText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_name, container, false);

        this.firstNameText = (TextView) view.findViewById(R.id.fragment_change_first_name_value);
        firstNameText.setOnEditorActionListener(new EditorActionHandler(() -> submit(firstNameText)));
        firstNameText.setText(AccountEditor.getContainer(this).getAccount().getFirstName());

        this.lastNameText = (TextView) view.findViewById(R.id.fragment_change_last_name_value);
        lastNameText.setOnEditorActionListener(new EditorActionHandler(() -> submit(lastNameText)));
        lastNameText.setText(AccountEditor.getContainer(this).getAccount().getLastName());

        Button submit = (Button) view.findViewById(R.id.fragment_change_name_submit);
        Views.setSafeOnClickListener(submit, this::submit);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.firstNameText = null;
        this.lastNameText = null;
    }

    public void submit(@NonNull View sender) {
        final String firstName = AccountPresenter.normalizeInput(firstNameText.getText());
        final String lastName = AccountPresenter.normalizeInput(lastNameText.getText());
        firstNameText.setText(firstName);
        lastNameText.setText(lastName);
        //Todo Currently no validation for lastName
        // Todo suggest updating the error validation animation
        if (!AccountPresenter.validateName(firstName)) {
            animatorFor(firstNameText)
                    .scale(1.4f)
                    .addOnAnimationCompleted(finished -> {
                        if (finished) {
                            animatorFor(firstNameText)
                                    .scale(1.0f)
                                    .start();
                        }
                    })
                    .start();
            return;
        }

        final AccountEditor.Container container = AccountEditor.getContainer(this);
        container.getAccount().setFirstName(firstName);
        container.getAccount().setLastName(lastName);
        container.onAccountUpdated(this);
    }
}
